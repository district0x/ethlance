(ns ethlance.ui.page.job-detail.events
  (:require
    [cljs-web3-next.eth :as w3n-eth]
    [district.ui.conversion-rates.queries :as conversion-rates.queries]
    [district.ui.notification.events :as notification.events]
    [district.ui.router.effects :as router.effects]
    [district.ui.router.queries :as router.queries]
    [district.ui.smart-contracts.queries :as contract-queries]
    [district.ui.web3-accounts.queries :as accounts-queries]
    [district.ui.web3-tx.events :as web3-events]
    [district.ui.web3.queries :as web3-queries]
    [ethlance.shared.contract-constants :as contract-constants]
    [ethlance.shared.utils :refer [eth->wei base58->hex]]
    [ethlance.ui.event.utils :as event.utils]
    [ethlance.ui.util.tokens :as util.tokens]
    [re-frame.core :as re]))


;; Page State
(def state-key :page.job-detail)


(def state-default
  {:add-funds-tx-in-progress? false
   :end-job-tx-in-progress? false
   :invite-arbiters-in-progress? false
   :proposal-offset 0
   :proposal-limit 3
   :arbitrations-offset 0
   :arbitrations-limit 3
   :selected-arbiters #{}
   :job-arbiter-idle false
   :show-invite-arbiters false})


(def interceptors [re/trim-v])


(defn initialize-page
  "Event FX Handler. Setup listener to dispatch an event when the page is active/visited."
  [{:keys [db]}]
  {::router.effects/watch-active-page
   [{:id :page.job-detail/initialize-page
     :name :route.job/detail
     :dispatch [:page.job-detail/fetch-proposals]}]
   :db (assoc db state-key state-default)})


(defn set-add-funds-tx-in-progress
  [db in-progress?]
  (assoc-in db [state-key :add-funds-tx-in-progress?] in-progress?))


(defn set-end-job-tx-in-progress
  [db in-progress?]
  (assoc-in db [state-key :end-job-tx-in-progress?] in-progress?))


(defn set-invite-arbiters-tx-in-progress
  [db in-progress?]
  (assoc-in db [state-key :invite-arbiters-tx-in-progress?] in-progress?))


;;
;; Registered Events
;;
(def create-assoc-handler (partial event.utils/create-assoc-handler state-key))


(defn create-logging-handler
  ([] (create-logging-handler ""))
  ([text] (fn [_db args] (println ">>> Received event in" state-key " " text " with args:" args))))


;; TODO: switch based on dev environment
(re/reg-event-fx :page.job-detail/initialize-page initialize-page)
(re/reg-event-fx :page.job-detail/set-proposal-token-amount (create-assoc-handler :job/proposal-token-amount))
(re/reg-event-fx :page.job-detail/set-proposal-text (create-assoc-handler :job/proposal-text))
(re/reg-event-fx :page.job-detail/set-proposal-offset (create-assoc-handler :proposal-offset))

(re/reg-event-fx :page.job-detail/set-arbitrations-offset (create-assoc-handler :arbitrations-offset))


(re/reg-event-db
  :page.job-detail/set-arbitration-token-amount
  (fn [db [_ token-amount]]
    (-> db
        (assoc-in ,,, [state-key :arbitration-token-amount] token-amount)
        (assoc-in ,,, [state-key :arbitration-token-amount-usd]
                      (util.tokens/round 2 (* token-amount (conversion-rates.queries/conversion-rate db :ETH :USD)))))))


(re/reg-event-db
  :page.job-detail/set-arbitration-token-amount-usd
  (fn [db [_ usd-amount]]
    (-> db
        (assoc-in ,,, [state-key :arbitration-token-amount-usd] usd-amount)
        (assoc-in ,,, [state-key :arbitration-token-amount]
                      (util.tokens/round 4 (* usd-amount (conversion-rates.queries/conversion-rate db :USD :ETH)))))))


(re/reg-event-fx :page.job-detail/set-show-invite-arbiters (create-assoc-handler :show-invite-arbiters))


(re/reg-event-db
  :page.job-detail/set-arbitration-to-accept
  (fn [db [_ arbitration]]
    (let [db-path [:page.job-detail :arbitration-to-accept]
          previous-selection (get-in db db-path)
          toggled-value (if (= previous-selection arbitration)
                          nil
                          arbitration)]
      (assoc-in db db-path toggled-value))))


(def job-story-requested-fields
  [:job-story/id
   :job/id
   :job-story/status
   :job-story/proposal-rate
   :job-story/date-created
   :job-story/candidate
   [:candidate
    [:user/id
     [:user [:user/id :user/name]]]]
   [:proposal-message
    [:message/id
     :message/type
     :message/text
     [:creator
      [:user/id :user/name]]]]])


(re/reg-event-fx
  :page.job-proposal/send
  [interceptors]
  (fn [{:keys [db]} [contract-address token-type]]
    (let [text (get-in db [state-key :job/proposal-text])
          token-amount (get-in db [state-key :job/proposal-token-amount :token-amount])
          proposal {:contract contract-address
                    :text text
                    :rate (js/parseFloat token-amount)}]
      {:dispatch [:district.ui.graphql.events/mutation
                  {:queries [[:create-job-proposal {:input proposal}
                              job-story-requested-fields]]
                   :on-success [:page.job-detail/fetch-proposals]}]})))


(re/reg-event-fx
  :page.job-proposal/remove
  [interceptors]
  (fn [{:keys [db]} [job-story-id]]
    {:db (-> db
             (assoc-in ,,, [state-key :job/proposal-token-amount] nil)
             (assoc-in ,,, [state-key :job/proposal-text] nil))
     :dispatch [:district.ui.graphql.events/mutation
                {:queries [[:remove-job-proposal {:job-story/id job-story-id}
                            job-story-requested-fields]]
                 :on-success [:page.job-detail/fetch-proposals]}]}))


(re/reg-event-fx
  :page.job-detail/fetch-proposals
  [interceptors]
  (fn [{:keys [db]} [_ router-params]]
    (let [queried-contract-address (:contract router-params)
          contract-from-db (get-in db [:district.ui.router :active-page :params :id])
          contract (or queried-contract-address contract-from-db)]
      {:dispatch [:district.ui.graphql.events/query!
                  {:queries
                   [[:job-story-search {:search-params {:job contract}
                                        :limit (get-in db [state-key :proposal-limit])
                                        :offset (get-in db [state-key :proposal-offset])}
                     [:total-count
                      [:items job-story-requested-fields]]]]
                   :on-success [:proposal-stories-success]
                   :on-error [:proposal-stories-error]}]})))


(re/reg-event-fx
  :proposal-stories-success
  [interceptors]
  (fn [{:keys [db]} data]
    (let [result (some :job-story-search data)
          stories (get result :items)
          id-mapped (reduce
                      (fn [acc job-story]
                        (assoc acc (:job-story/id job-story) job-story))
                      {}
                      stories)]
      {:db (-> db
               (assoc ,,, :job-stories id-mapped)
               (assoc-in ,,, [state-key :proposal-total-count] (:total-count result)))})))


(re/reg-event-fx
  :proposal-stories-error
  [interceptors]
  (fn [{:keys [db]} error]
    (merge db [state-key :graphql-error] error)))


(defn send-arbitration-data-to-ipfs
  [_cofx [_ event]]
  (let [ipfs-arbitration (select-keys event
                                      [:job/id
                                       :user/id
                                       :job-arbiter/fee
                                       :job-arbiter/fee-currency-id])]
    {:ipfs/call {:func "add"
                 :args [(js/Blob. [ipfs-arbitration])]
                 :on-success [:page.job-detail/arbitration-to-ipfs-success event]
                 :on-error [:page.job-detail/arbitration-to-ipfs-failure event]}}))


(defn set-quote-for-arbitration-tx
  [cofx [_event-name forwarded-event-data ipfs-data]]
  (let [job-address (:job/id forwarded-event-data)
        arbiter-address (:user/id forwarded-event-data)
        token-type (:job-arbiter/fee-currency-id forwarded-event-data)
        amount-in-wei (eth->wei (:job-arbiter/fee forwarded-event-data))
        not-used-token-id 0
        address-placeholder "0x0000000000000000000000000000000000000000"
        offered-value {:value amount-in-wei
                       :token
                       {:tokenId not-used-token-id
                        :tokenContract
                        {:tokenType (contract-constants/token-type->enum-val token-type)
                         :tokenAddress address-placeholder}}}
        instance (contract-queries/instance (:db cofx) :job job-address)
        tx-opts {:from arbiter-address :gas 10000000}
        ipfs-hash (base58->hex (:Hash ipfs-data))
        ;; TODO: decide if sending to IPFS would serve for anything or all the
        ;;       information involved is already in the contract & QuoteForArbitrationEvent
        contract-args [[(clj->js offered-value)]]]
    {:dispatch [::web3-events/send-tx
                {:instance instance
                 :fn :set-quote-for-arbitration
                 :args contract-args
                 :tx-opts tx-opts
                 :tx-hash [::arbitration-tx-hash]
                 :on-tx-hash-error [::set-quote-for-arbitration-tx-hash-error]
                 :on-tx-success [:page.job-detail/arbitration-tx-success "Transaction to set quote successful"]
                 :on-tx-error [::set-quote-for-arbitration-tx-error]}]}))


(defn accept-quote-for-arbitration-tx
  [cofx [_event-name forwarded-event-data]]
  (let [job-address (:job/id forwarded-event-data)
        arbiter-address (:user/id forwarded-event-data)
        employer-address (:employer forwarded-event-data)
        token-type (:job-arbiter/fee-currency-id forwarded-event-data)
        amount-in-wei (str (:job-arbiter/fee forwarded-event-data))
        not-used-token-id 0
        address-placeholder "0x0000000000000000000000000000000000000000"
        offered-value {:value amount-in-wei
                       :token
                       {:tokenId not-used-token-id
                        :tokenContract
                        {:tokenType (contract-constants/token-type->enum-val token-type)
                         :tokenAddress address-placeholder}}}
        instance (contract-queries/instance (:db cofx) :job job-address)
        tx-opts {:from employer-address :gas 10000000 :value amount-in-wei}
        contract-args [arbiter-address [(clj->js offered-value)]]]
    {:dispatch [::web3-events/send-tx
                {:instance instance
                 :fn :accept-quote-for-arbitration
                 :args contract-args
                 :tx-opts tx-opts
                 :tx-hash [::arbitration-tx-hash]
                 :on-tx-hash-error [::accept-quote-for-arbitration-tx-hash-error]
                 :on-tx-success [:page.job-detail/arbitration-tx-success "Transaction to accept quote successful"]
                 :on-tx-error [::accept-quote-for-arbitration-tx-error]}]}))


(defn invite-arbiters
  [{:keys [db]} [_event-name event-data]]
  (let [job-address (:job/id event-data)
        arbiter-addresses (:arbiters event-data)
        employer-address (:employer event-data)
        instance (contract-queries/instance db :job job-address)
        tx-opts {:from employer-address}
        contract-args [employer-address arbiter-addresses]]
    {:db (set-invite-arbiters-tx-in-progress db true)
     :dispatch [::web3-events/send-tx
                {:instance instance
                 :fn :invite-arbiters
                 :args contract-args
                 :tx-opts tx-opts
                 :tx-hash [::arbitration-tx-hash]
                 :on-tx-hash-error [::invite-arbiters-tx-hash-error]
                 :on-tx-success [:page.job-detail/arbitration-tx-success "Transaction to invite arbiters successful"]
                 :on-tx-error [::invite-arbiters-tx-error]}]}))


(defn end-job-tx
  [{:keys [db] :as cofx} [_event-name event-data]]
  (let [job-address (:job/id event-data)
        employer-address (:employer event-data)
        instance (contract-queries/instance (:db cofx) :job job-address)
        tx-opts {:from employer-address :gas 10000000}]
    {:db (set-end-job-tx-in-progress db true)
     :dispatch [::web3-events/send-tx
                {:instance instance
                 :fn :end-job
                 :args []
                 :tx-opts tx-opts
                 :tx-hash [::arbitration-tx-hash]
                 :on-tx-hash-error [::end-job-tx-hash-error]
                 :on-tx-success [:page.job-detail/end-job-tx-success]
                 :on-tx-error [::end-job-tx-error]}]}))


(re/reg-event-fx :page.job-detail/set-quote-for-arbitration send-arbitration-data-to-ipfs)
(re/reg-event-fx :page.job-detail/accept-quote-for-arbitration accept-quote-for-arbitration-tx)
(re/reg-event-fx :page.job-detail/arbitration-to-ipfs-success set-quote-for-arbitration-tx)
(re/reg-event-db :page.job-detail/arbitration-to-ipfs-failure (create-logging-handler))

(re/reg-event-fx ::arbitration-tx-hash (create-logging-handler))
(re/reg-event-fx ::set-quote-for-arbitration-tx-hash-error (create-logging-handler))
(re/reg-event-fx ::set-quote-for-arbitration-tx-error (create-logging-handler))

(re/reg-event-fx ::accept-quote-for-arbitration-tx-hash-error (create-logging-handler))
(re/reg-event-fx ::accept-quote-for-arbitration-tx-error (create-logging-handler))

(re/reg-event-fx :page.job-detail/end-job end-job-tx)
(re/reg-event-fx ::end-job-tx-hash-error (create-logging-handler))


(re/reg-event-fx
  ::end-job-tx-error
  (fn [{:keys [db]} _]
    {:db (set-end-job-tx-in-progress db false)}))


(re/reg-event-fx :page.job-detail/invite-arbiters invite-arbiters)


(re/reg-event-db
  ::invite-arbiters-tx-error
  (fn [db _]
    (set-invite-arbiters-tx-in-progress db false)))


(re/reg-event-db
  ::invite-arbiters-tx-hash-error
  (fn [db _]
    (set-invite-arbiters-tx-in-progress db false)))


(re/reg-event-fx
  :page.job-detail/fetch-job-arbiter-status
  (fn [cofx _event]
    (let [web3-instance (web3-queries/web3 (:db cofx))
          job-address (:id (router.queries/active-page-params (:db cofx)))
          contract-instance (contract-queries/instance (:db cofx) :job job-address)
          to-call {:instance contract-instance
                   :fn :is-accepted-arbiter-idle
                   :args []
                   :on-success [::job-arbiter-status-success job-address]
                   :on-error [::job-arbiter-status-error]}]
      {:web3/call {:web3 web3-instance :fns [to-call]}})))


(re/reg-event-db
  ::job-arbiter-status-success
  (fn [db [_ job-address arbiter-idle?]]
    (println ">>> ::job-arbiter-status-success received" job-address arbiter-idle?)
    (assoc-in db [state-key :job-arbiter-idle] arbiter-idle?)))


(re/reg-event-db
  ::job-arbiter-status-error
  (fn [db event]
    (println ">>> ::job-arbiter-status-error" event)
    db))


(re/reg-event-fx
  :page.job-detail/arbitration-tx-success
  (fn [cofx [_event message]]
    {:db (-> (:db cofx)
             (set-invite-arbiters-tx-in-progress ,,, false)
             (assoc ,,, state-key state-default))
     :fx [[:dispatch [:page.job-detail/arbitrations-updated]]
          [:dispatch [::notification.events/show message]]]}))


(re/reg-event-db
  :page.job-detail/set-selected-arbiters
  (fn [db [_ selection]]
    (assoc-in db [state-key :selected-arbiters] selection)))


(re/reg-event-fx
  :page.job-detail/end-job-tx-success
  (fn [{:keys [db]} _event]
    {:db (set-end-job-tx-in-progress db false)
     :fx [[:dispatch [:page.job-detail/job-updated]]
          [:dispatch [::notification.events/show "Transaction to end job processed successfully"]]]}))


(re/reg-event-fx :page.job-detail/set-add-funds-amount (create-assoc-handler :add-funds-amount))
(re/reg-event-fx :page.job-detail/start-adding-funds (create-assoc-handler :adding-funds?))


(re/reg-event-fx
  ::send-add-funds-tx
  (fn [{:keys [db]} [_ funds-params]]
    (let [token-type (:token-type funds-params)
          tx-opts-base {:from (:funder funds-params) :gas 10000000}
          offered-value (:offered-value funds-params)
          tx-opts (if (= token-type :eth)
                    (assoc tx-opts-base :value (:value offered-value))
                    tx-opts-base)]
      {:fx [[:dispatch [::web3-events/send-tx
                        {:instance (contract-queries/instance db :job (:receiver funds-params))
                         :fn :add-funds
                         :args [[(clj->js (:offered-value funds-params))]]
                         :tx-opts tx-opts
                         :tx-hash [::tx-hash]
                         :on-tx-hash-error [::tx-hash-error]
                         :on-tx-success [::add-funds-tx-success]
                         :on-tx-error [::add-funds-tx-error]}]]]})))


(re/reg-event-fx
  ::erc20-allowance-amount-success
  (fn [{:keys [db] :as cofx} [_ funds-params result]]
    (let [offered-value (funds-params :offered-value)
          amount (:value offered-value)
          erc20-address (get-in offered-value [:token :tokenContract :tokenAddress])
          erc20-abi (:erc20 ethlance.shared.contract-constants/abi)
          erc20-instance (w3n-eth/contract-at (web3-queries/web3 db) erc20-abi erc20-address)
          owner (:funder funds-params)
          spender (:receiver funds-params)
          enough-allowance? (>= (int result) amount)
          increase-allowance-event [::web3-events/send-tx
                                    {:instance erc20-instance
                                     :fn :approve
                                     :args [spender amount]
                                     :tx-opts {:from owner}
                                     :on-tx-success [::send-add-funds-tx funds-params]
                                     :on-tx-error [::erc20-allowance-approval-error]}]]
      {:fx [[:dispatch (if enough-allowance?
                         [::send-add-funds-tx funds-params]
                         increase-allowance-event)]]})))


(re/reg-event-fx
  ::erc20-allowance-amount-error
  (fn [_cofx result]
    (println ">>> ::erc20-allowance-amount-error" result)))


(re/reg-event-fx
  ::ensure-erc20-allowance
  (fn [{:keys [db]} [_ funds-params]]
    (let [offered-value (funds-params :offered-value)
          erc20-address (get-in offered-value [:token :tokenContract :tokenAddress])
          erc20-abi (:erc20 ethlance.shared.contract-constants/abi)
          erc20-instance (w3n-eth/contract-at (web3-queries/web3 db) erc20-abi erc20-address)
          owner (:funder funds-params)
          spender (:receiver funds-params)]
      {:fx [[:web3/call {:fns
                         [{:instance erc20-instance
                           :fn :allowance
                           :args [owner spender]
                           :on-success [::erc20-allowance-amount-success funds-params]
                           :on-error [::erc20-allowance-amount-error]}]}]]})))


(re/reg-event-fx
  ::safe-transfer-with-add-funds
  (fn [{:keys [db]} [_ funds-params]]
    (let [offered-value (:offered-value funds-params)
          amount (:value offered-value)
          token-address (get-in offered-value [:token :tokenContract :tokenAddress])
          token-type (:token-type funds-params)
          token-abi (get ethlance.shared.contract-constants/abi token-type)
          token-id (get-in offered-value [:token :tokenId])
          token-instance (w3n-eth/contract-at (web3-queries/web3 db) token-abi token-address)
          owner (:funder funds-params)
          spender (:receiver funds-params)
          not-used-invoice-id 0
          contract-callback-params [(:add-funds contract-constants/job-callback-target-method)
                                    owner
                                    [offered-value]
                                    not-used-invoice-id]
          job-instance (contract-queries/instance db :job spender)
          data-for-callback (w3n-eth/encode-abi
                              job-instance
                              :transfer-callback-delegate
                              contract-callback-params)
          safe-transfer-args (case token-type
                               :erc721 [owner spender token-id data-for-callback]
                               :erc1155 [owner spender token-id amount data-for-callback])
          safe-transfer-with-create-job-tx [::web3-events/send-tx
                                            {:instance token-instance
                                             :fn :safe-transfer-from
                                             :args safe-transfer-args
                                             :tx-opts {:from owner}
                                             :on-tx-success [::add-funds-tx-success]
                                             :on-tx-error [::add-funds-tx-error]}]]
      {:fx [[:dispatch safe-transfer-with-create-job-tx]]})))


(re/reg-event-fx
  :page.job-detail/finish-adding-funds
  (fn [{:keys [db] :as cofx} [_ job-address token-details token-id tx-amount]]
    (let [funder (accounts-queries/active-account (:db cofx))
          token-type (:token-detail/type token-details)
          token-address (:token-detail/id token-details)
          address-placeholder "0x0000000000000000000000000000000000000000"
          token-address (if (not (= token-type :eth))
                          token-address
                          address-placeholder)
          offered-value {:value (str tx-amount)
                         :token
                         {:tokenId token-id
                          :tokenContract
                          {:tokenType (contract-constants/token-type->enum-val token-type)
                           :tokenAddress token-address}}}
          funds-params {:offered-value offered-value
                        :token-type token-type
                        :funder funder
                        :receiver job-address}
          next-event {:eth ::send-add-funds-tx
                      :erc20 ::ensure-erc20-allowance
                      :erc721 ::safe-transfer-with-add-funds
                      :erc1155 ::safe-transfer-with-add-funds}]
      {:db (set-add-funds-tx-in-progress db true)
       :fx [[:dispatch [(get next-event token-type) funds-params]]]})))


(re/reg-event-fx
  ::add-funds-tx-success
  (fn [{:keys [db]} [_event-name _tx-data]]
    {:db (-> db
             (assoc-in ,,, [state-key :adding-funds?] false)
             (assoc-in ,,, [state-key :add-funds-amount] nil)
             (set-add-funds-tx-in-progress ,,, false))
     :fx [[:dispatch [::notification.events/show "Transaction to add funds processed successfully"]]
          [:dispatch [:page.job-detail/job-updated]]]}))


(re/reg-event-db
  ::add-funds-tx-error
  (fn [db _event]
    {:db (set-add-funds-tx-in-progress db false)
     :dispatch [::notification.events/show "Error with add funds to job transaction"]}))
