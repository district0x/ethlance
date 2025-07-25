(ns ethlance.ui.page.new-job.events
  (:require
    [cljs.core.async :as async :refer [<!] :refer-macros [go]]
    [cljs-web3-next.eth :as w3n-eth]
    [district.ui.notification.events :as notification.events]
    [district.ui.router.effects :as router.effects]
    [district.ui.router.events :as router-events]
    [district.ui.smart-contracts.queries :as contract-queries]
    [district.ui.web3-accounts.queries :as accounts-queries]
    [district.ui.web3-tx.events :as web3-events]
    [district.ui.web3.queries :as web3-queries]
    [ethlance.shared.constants :as constants]
    [ethlance.shared.contract-constants :as contract-constants]
    [ethlance.shared.utils :refer [base58->hex js-obj->clj-map]]
    [ethlance.ui.event.utils :as event.utils]
    [ethlance.ui.util.tokens :as util.tokens]
    [re-frame.core :as re]))


(def state-key :page.new-job)
(def new-job-params-path [state-key :job-creation-params])


(defn get-job-creation-param
  [db param-key]
  (get-in db (conj new-job-params-path param-key)))


(def interceptors [re/trim-v])


(def state-default
  {:job/title "Rauamaak on meie saak"
   :job/description "Tee t88d ja n2e vaeva"
   :job/category "Admin Support"
   :job/bid-option :hourly-rate
   :job/required-experience-level :intermediate
   :job/estimated-project-length :day
   :job/required-availability :full-time
   :job/required-skills (set (repeatedly 2 #(rand-nth constants/skills)))
   :job/token-type :eth
   :job/token-amount 0.69
   :job/token-address "0x1111111111111111111111111111111111111111"
   :job/token-id 0
   :job/with-arbiter? false
   :job/invited-arbiters #{}
   :job/token-decimals 18})


(defn initialize-page
  "Event FX Handler. Setup listener to dispatch an event when the page is active/visited."
  []
  {::router.effects/watch-active-page
   [{:id :page.new-job/initialize-page
     :name :route.job/new
     :dispatch [:page.new-job/auto-fill-form]}]})


(def create-assoc-handler (partial event.utils/create-assoc-handler state-key))


(re/reg-event-db
  :page.new-job/auto-fill-form
  (fn [db]
    (assoc db state-key state-default)))


(re/reg-event-fx :page.new-job/initialize-page initialize-page)
(re/reg-event-fx :page.new-job/set-bid-option (create-assoc-handler :job/bid-option))
(re/reg-event-fx :page.new-job/set-category (create-assoc-handler :job/category))
(re/reg-event-fx :page.new-job/set-description (create-assoc-handler :job/description))
(re/reg-event-fx :page.new-job/set-estimated-project-length (create-assoc-handler :job/estimated-project-length))
(re/reg-event-fx :page.new-job/set-title (create-assoc-handler :job/title))
(re/reg-event-fx :page.new-job/set-required-availability (create-assoc-handler :job/required-availability))
(re/reg-event-fx :page.new-job/set-required-experience-level (create-assoc-handler :job/required-experience-level))
(re/reg-event-fx :page.new-job/set-required-skills (create-assoc-handler :job/required-skills))
(re/reg-event-fx :page.new-job/set-with-arbiter? (create-assoc-handler :job/with-arbiter?))


(re/reg-event-db
  :page.new-job/invite-arbiter
  (fn [db [_ arbiter]]
    (assoc-in db [state-key :job/invited-arbiters]
              (conj (get-in db [state-key :job/invited-arbiters]) arbiter))))


(re/reg-event-db
  :page.new-job/uninvite-arbiter
  (fn [db [_ arbiter]]
    (assoc-in db [state-key :job/invited-arbiters]
              (disj (get-in db [state-key :job/invited-arbiters]) arbiter))))


;; The simple setter implementation for eventual production
;; (re/reg-event-fx :page.new-job/set-token-type (create-assoc-handler :job/token-type))

(re/reg-event-db
  :page.new-job/decimals-response
  (fn [db [_ decimals]]
    (assoc-in db [state-key :job/token-decimals] decimals)))


(re/reg-event-fx
  :page.new-job/set-token-type
  (fn [{:keys [db] :as _cofx} [_ token-type]]
    (let [default-decimals (if (= token-type :eth) 18 0)
          decimals-fx-fn (if (= :erc20 token-type)
                          (fn []
                            [:web3/call
                             {:fns
                              [{:instance (w3n-eth/contract-at
                                          (district.ui.web3.queries/web3 db)
                                          (ethlance.shared.contract-constants/abi token-type)
                                          (get-in db [state-key :job/token-address]))
                                :fn :decimals
                                :args []
                                :on-success [:page.new-job/decimals-response]
                                :on-error [:page.new-job/decimals-response]}]}])
                          (fn [] [:dispatch [:page.new-job/decimals-response default-decimals]]))]
      {:fx [(decimals-fx-fn)]
       :db (-> db
               (assoc-in ,,, [state-key :job/token-amount] 1)
               (assoc-in ,,, [state-key :job/token-type] token-type))})))


(re/reg-event-fx :page.new-job/set-token-amount (create-assoc-handler :job/token-amount))
(re/reg-event-fx :page.new-job/set-token-address (create-assoc-handler :job/token-address))
(re/reg-event-fx :page.new-job/set-token-id (create-assoc-handler :job/token-id))


(def db->ipfs-mapping
  {:job/bid-option :job/bid-option
   :job/category :job/category
   :job/description :job/description
   :job/estimated-project-length :job/estimated-project-length
   :job/required-experience-level :job/required-experience-level
   :job/required-availability :job/required-availability
   :job/required-skills :job/required-skills
   :job/title :job/title
   :job/invited-arbiters :job/invited-arbiters})


(defn- db-job->ipfs-job
  "Useful for renaming map keys by reducing over a map of keyword -> keyword
  where: key is the name of the resulting map key and value is the key of the
  `job-data` map.

  Use `partial` to 'remember' the data object and get function signature
  suitable for `reduce`"
  [job-data acc ipfs-key db-key]
  (assoc acc ipfs-key (job-data db-key)))


(defn set-tx-in-progress
  [db in-progress?]
  (assoc-in db [state-key :tx-in-progress?] in-progress?))


(re/reg-event-fx
  ::job-to-ipfs-success
  (fn [cofx event]
    (println ">>> :job-to-ipfs-success")
    (let [creator (accounts-queries/active-account (:db cofx))
          job-fields (get-in cofx [:db state-key])
          token-type (:job/token-type job-fields)
          token-amount (if (= token-type :erc721)
                         1
                         (get-in job-fields [:job/token-amount :token-amount]))
          address-placeholder "0x0000000000000000000000000000000000000000"
          token-address (if (not (= token-type :eth))
                          (:job/token-address job-fields)
                          address-placeholder)
          offered-value {:value (str token-amount)
                         :token
                         {:tokenId (:job/token-id job-fields)
                          :tokenContract
                          {:tokenType (contract-constants/token-type->enum-val token-type)
                           :tokenAddress token-address}}}
          invited-arbiters (if (:job/with-arbiter? job-fields)
                             (into [] (:job/invited-arbiters job-fields))
                             [])
          new-job-params {:offered-value offered-value
                          :token-type token-type
                          :employer creator
                          :arbiters invited-arbiters
                          :ipfs-hash (base58->hex (get-in event [1 :Hash]))}
          next-event {:eth ::send-create-job-tx
                      :erc20 ::ensure-erc20-allowance
                      :erc721 ::safe-transfer-with-create-job
                      :erc1155 ::safe-transfer-with-create-job}]
      {:db (assoc-in (:db cofx) new-job-params-path new-job-params)
       :fx [[:dispatch [(get next-event token-type)]]]})))


(re/reg-event-fx
  ::job-to-ipfs-failure
  (fn [{:keys [db]} _]
    (println ">>> :job-to-ipfs-failure")
    {:db (set-tx-in-progress db false)
     :dispatch [::notification.events/show "Error with loading new job data to IPFS"]}))


(re/reg-event-fx
  :page.new-job/create
  [interceptors]
  (fn [{:keys [db]}]
    (let [db-job (get db state-key)
          ipfs-job (reduce-kv (partial db-job->ipfs-job db-job) {} db->ipfs-mapping)]
      {:data/upload {:data ipfs-job
                     :on-success [::job-to-ipfs-success]
                     :on-error [::job-to-ipfs-failure]}})))


(re/reg-event-fx
  ::send-create-job-tx
  (fn [{:keys [db]} _]
    (let [employer (get-job-creation-param db :employer)
          offered-value (get-job-creation-param db :offered-value)
          ipfs-hash (get-job-creation-param db :ipfs-hash)
          arbiters (get-job-creation-param db :arbiters)
          tx-opts-base {:from employer}
          token-type (get-job-creation-param db :token-type)
          tx-opts (if (= token-type :eth)
                    (assoc tx-opts-base :value (:value offered-value))
                    tx-opts-base)]
      {:db (set-tx-in-progress db true)
       :fx [[:dispatch [::web3-events/send-tx
                        {:instance (contract-queries/instance db :ethlance)
                         :fn :createJob
                         :args [employer [(clj->js offered-value)] arbiters ipfs-hash]
                         :tx-opts tx-opts
                         :tx-hash [::tx-hash]
                         :on-tx-hash-error [::tx-hash-error]
                         :on-tx-success [::create-job-tx-success]
                         :on-tx-error [::create-job-tx-error]}]]]})))


(re/reg-event-fx
  ::tx-hash-error
  (fn [{:keys [db]} result]
    {:db (set-tx-in-progress db false)
     :fx [[:dispatch [::notification.events/show "Error with sending transaction"]]]}))


(re/reg-event-fx
  ::erc20-allowance-approval-success
  (fn [_cofx result]
    {:fx [[:dispatch [::notification.events/show "Got the ERC20 allowance approval confirmation"]]
          [:dispatch [::send-create-job-tx]]]}))


(re/reg-event-fx
  ::erc20-allowance-approval-error
  (fn [{:keys [db]} result]
    {:db (set-tx-in-progress db false)
     :fx [[:dispatch [::notification.events/show "Error obtaining allowance for the ERC20 token"]]]}))


(re/reg-event-fx
  ::erc20-allowance-amount-success
  (fn [{:keys [db]} [_ result]]
    (let [offered-value (get-job-creation-param db :offered-value)
          amount (:value offered-value)
          erc20-address (get-in offered-value [:token :tokenContract :tokenAddress])
          erc20-abi (:erc20 ethlance.shared.contract-constants/abi)
          erc20-instance (w3n-eth/contract-at (web3-queries/web3 db) erc20-abi erc20-address)
          owner (get-job-creation-param db :employer)
          spender (contract-queries/contract-address db :ethlance)
          enough-allowance? (>= (int result) amount)
          increase-allowance-event [::web3-events/send-tx
                                    {:instance erc20-instance
                                     :fn :approve
                                     :args [spender amount]
                                     :tx-opts {:from owner}
                                     :on-tx-hash-error [::tx-hash-error]
                                     :on-tx-success [::erc20-allowance-approval-success]
                                     :on-tx-error [::erc20-allowance-approval-error]}]]
      {:db (set-tx-in-progress db true)
       :fx [[:dispatch (if enough-allowance?
                         [::send-create-job-tx]
                         increase-allowance-event)]]})))


(re/reg-event-fx
  ::erc20-allowance-amount-error
  (fn [{:keys [db]} result]
    {:db (set-tx-in-progress db false)
     :fx [[:dispatch [::notification.events/show "Couldn't get the ERC20 allowance amount"]]]}))


(re/reg-event-fx
  ::ensure-erc20-allowance
  (fn [{:keys [db] :as _cofx} _]
    (let [offered-value (get-job-creation-param db :offered-value)
          erc20-address (get-in offered-value [:token :tokenContract :tokenAddress])
          erc20-abi (:erc20 ethlance.shared.contract-constants/abi)
          erc20-instance (w3n-eth/contract-at (web3-queries/web3 db) erc20-abi erc20-address)
          owner (get-job-creation-param db :employer)
          spender (contract-queries/contract-address db :ethlance)]
      {:fx [[:web3/call {:fns
                         [{:instance erc20-instance
                           :fn :allowance
                           :args [owner spender]
                           :on-success [::erc20-allowance-amount-success] ; handler needs to read app-db
                           :on-error [::erc20-allowance-amount-error]}]}]]})))


(defn job-creation-params
  [_db]
  {:offered-value nil
   :arbiters nil
   :ipfs-hash nil})


(re/reg-event-fx
  ::safe-transfer-with-create-job
  (fn [{:keys [db] :as _cofx} _]
    (let [offered-value (get-job-creation-param db :offered-value)
          amount (:value offered-value)
          token-address (get-in offered-value [:token :tokenContract :tokenAddress])
          token-type (get-job-creation-param db :token-type)
          token-abi (get ethlance.shared.contract-constants/abi token-type)
          token-id (get-in offered-value [:token :tokenId])
          token-instance (w3n-eth/contract-at (web3-queries/web3 db) token-abi token-address)
          owner (get-job-creation-param db :employer)
          spender (contract-queries/contract-address db :ethlance)
          contract-callback-params [(:one-step-job-creation contract-constants/operation-type)
                                    owner
                                    [offered-value]
                                    (get-job-creation-param db :arbiters)
                                    (get-job-creation-param db :ipfs-hash)]
          ethlance-instance (contract-queries/instance db :ethlance)
          data-for-callback (w3n-eth/encode-abi ethlance-instance :transfer-callback-delegate contract-callback-params)
          safe-transfer-args (case token-type
                               :erc721 [owner spender token-id data-for-callback]
                               :erc1155 [owner spender token-id amount data-for-callback])
          safe-transfer-with-create-job-tx [::web3-events/send-tx
                                            {:instance token-instance
                                             :fn :safe-transfer-from
                                             :args safe-transfer-args
                                             :tx-opts {:from owner}
                                             :on-tx-hash-error [::tx-hash-error]
                                             :on-tx-success [::create-job-tx-success]
                                             :on-tx-error [::create-job-tx-error]}]]
      {:db (set-tx-in-progress db true)
       :fx [[:dispatch safe-transfer-with-create-job-tx]]})))


(re/reg-event-fx
  ::tx-hash
  (fn [_db event] (println ">>> ethlance.ui.page.new-job.events :tx-hash" event)))


(defn async-request-event
  [{:keys [contract event block-number callback]}]
  (-> (w3n-eth/get-past-events contract event {:from-block block-number :to-block block-number})
      (.then ,,, callback)))


(defn redirect-from-job-created-tx-receipt [web3 transaction-hash]
  (w3n-eth/get-transaction-receipt web3 transaction-hash
    (fn [err receipt]
      (let [job-created (util.tokens/parse-event-in-tx-receipt :JobCreated receipt)]
        (println "redirect-from-job-created-tx-receipt" {:err err :receipt receipt :parsed job-created :type (type job-created)})
        (re/dispatch
          [::router-events/navigate
           :route.job/detail
           {:id (:job (first job-created))}])))))

(re/reg-event-fx
  ::create-job-tx-success
  (fn [{:keys [db]} [_event-name tx-data]]
    (let [job-from-event (get-in tx-data [:events :Job-created :return-values :job])
          parsed-event (util.tokens/parse-event-in-tx-receipt :JobCreated tx-data)]
      (println ">>> ::create-job-tx-success tx-data:" tx-data)
      (println ">>> ::create-job-tx-success job-from-event:" job-from-event)
      (println ">>> ::create-job-tx-success parsed-event:" parsed-event)
      {:db (set-tx-in-progress db false)
       :fx [[:dispatch [::notification.events/show "Transaction to create job processed successfully"]]
            ;; When creating job via ERC721/1155 callback (onERC{721,1155}Received), the event data is part of the
            ;; tx-receipt, but doesn't have event name, making it difficult to find and decode. Thus this workaround:
            ;;   - requesting the transaction receipt (that then has JobCreated event), decoding it and getting the
            ;;     created job address from there
            (if job-from-event
              [:dispatch-later [{:ms 1000 :dispatch [::router-events/navigate :route.job/detail {:id (get-in tx-data [:events :Job-created :return-values :job])}]}]]
              (redirect-from-job-created-tx-receipt (web3-queries/web3 db) (:transaction-hash tx-data)))]})))


(re/reg-event-db
  ::create-job-tx-error
  (fn [db event]
    (println ">>> ethlance.ui.page.new-job.events ::create-job-tx-error" event)
    {:db (set-tx-in-progress db false)
     :dispatch [::notification.events/show "Error with creating new job transaction"]}))
