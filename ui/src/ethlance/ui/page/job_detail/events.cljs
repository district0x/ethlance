(ns ethlance.ui.page.job-detail.events
  (:require [district.ui.router.effects :as router.effects]
            [district.ui.router.queries :as router.queries]
            [district.ui.conversion-rates.queries :as conversion-rates.queries]
            [district.ui.web3.queries :as web3.queries]
            [ethlance.ui.event.utils :as event.utils]
            [district.ui.notification.events :as notification.events]
            [ethlance.ui.util.tokens :as util.tokens]
            [ethlance.shared.utils :refer [eth->wei]]
            [district.ui.web3-tx.events :as web3-events]
            [ethlance.shared.contract-constants :as contract-constants]
            [district.ui.smart-contracts.queries :as contract-queries]
            [ethlance.shared.utils :refer [eth->wei base58->hex]]
            [district.ui.web3-accounts.queries :as accounts-queries]
            [re-frame.core :as re]))

;; Page State
(def state-key :page.job-detail)
(def state-default
  {:proposal-offset 0
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
  (let [page-state (get db state-key)]
    {
     ; :fx [[:dispatch [:page.job-detail/fetch-job-arbiter-status]]]
     ::router.effects/watch-active-page
     [{:id :page.job-detail/initialize-page
       :name :route.job/detail
       :dispatch [:page.job-detail/fetch-proposals]
       }]
     :db (assoc-in db [state-key] state-default)}))

;;
;; Registered Events
;;
(def create-assoc-handler (partial event.utils/create-assoc-handler state-key))
(defn create-logging-handler
  ([] (create-logging-handler ""))
  ([text] (fn [db args] (println ">>> Received event in" state-key " " text " with args:" args))))

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
     :message/creator]]])

(re/reg-event-fx
  :page.job-proposal/send
  [interceptors]
  (fn [{:keys [db]} [contract-address]]
    (let [user-address (accounts-queries/active-account db)
          text (get-in db [state-key :job/proposal-text])
          token-amount (get-in db [state-key :job/proposal-token-amount])
          proposal {:contract contract-address
                    :text text
                    :rate (js/parseFloat (eth->wei token-amount))}]
      {:dispatch [:district.ui.graphql.events/mutation
                  {:queries [[:create-job-proposal {:input proposal}
                              job-story-requested-fields]]
                   :on-success [:page.job-detail/fetch-proposals]}]})))

(re/reg-event-fx
  :page.job-proposal/remove
  [interceptors]
  (fn [{:keys [db]} [job-story-id]]
    (let [user-address (accounts-queries/active-account db)]
      {:db (-> db
               (assoc-in ,,, [state-key :job/proposal-token-amount] nil)
               (assoc-in ,,, [state-key :job/proposal-text] nil))
       :dispatch [:district.ui.graphql.events/mutation
                  {:queries [[:remove-job-proposal {:job-story/id job-story-id}
                              job-story-requested-fields]]
                   :on-success [:page.job-detail/fetch-proposals]}]})))

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
          stories (get-in result [:items])
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
  [{:keys [db]} [_ event]]
  (let [ipfs-arbitration (select-keys event
                                      [:job/id
                                       :user/id
                                       :job-arbiter/fee
                                       :job-arbiter/fee-currency-id])]
    {:ipfs/call {:func "add"
                 :args [(js/Blob. [ipfs-arbitration])]
                 :on-success [:page.job-detail/arbitration-to-ipfs-success event]
                 :on-error [:page.job-detail/arbitration-to-ipfs-failure event]}}))

(defn set-quote-for-arbitration-tx [cofx [_event-name forwarded-event-data ipfs-data]]
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
        ; TODO: decide if sending to IPFS would serve for anything or all the
        ;       information involved is already in the contract & QuoteForArbitrationEvent
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

(defn accept-quote-for-arbitration-tx [cofx [_event-name forwarded-event-data]]
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

(defn invite-arbiters [cofx [_event-name event-data]]
  (let [job-address (:job/id event-data)
        arbiter-addresses (:arbiters event-data)
        employer-address (:employer event-data)
        instance (contract-queries/instance (:db cofx) :job job-address)
        tx-opts {:from employer-address :gas 10000000}
        contract-args [employer-address arbiter-addresses]]
     {:dispatch [::web3-events/send-tx
                {:instance instance
                 :fn :invite-arbiters
                 :args contract-args
                 :tx-opts tx-opts
                 :tx-hash [::arbitration-tx-hash]
                 :on-tx-hash-error [::invite-arbiters-tx-hash-error]
                 :on-tx-success [:page.job-detail/arbitration-tx-success "Transaction to invite arbiters successful"]
                 :on-tx-error [::invite-arbiters-tx-error]}]}))

(defn end-job-tx [cofx [_event-name event-data]]
  (let [job-address (:job/id event-data)
        employer-address (:employer event-data)
        instance (contract-queries/instance (:db cofx) :job job-address)
        tx-opts {:from employer-address :gas 10000000}]
    {:dispatch [::web3-events/send-tx
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
(re/reg-event-fx ::end-job-tx-error (create-logging-handler))

(re/reg-event-fx :page.job-detail/invite-arbiters invite-arbiters)
(re/reg-event-fx ::invite-arbiters-tx-error (create-logging-handler))
(re/reg-event-fx ::invite-arbiters-tx-hash-error (create-logging-handler))

(re/reg-event-fx
  :page.job-detail/fetch-job-arbiter-status
  (fn [cofx event]
    (let [
          ; web3-instance (web3.queries/web3 (:db cofx))
          web3-instance (district.ui.web3.queries/web3 (:db cofx))
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
  (fn [cofx [event message]]
    {:db (assoc-in (:db cofx) [state-key] state-default)
     :fx [[:dispatch [:page.job-detail/arbitrations-updated]]
          [:dispatch [::notification.events/show message]]]}))

(re/reg-event-db
  :page.job-detail/set-selected-arbiters
  (fn [db [_ selection]]
    (assoc-in db [state-key :selected-arbiters] selection)))

(re/reg-event-fx
  :page.job-detail/end-job-tx-success
  (fn [cofx event]
    {:fx [[:dispatch [:page.job-detail/job-updated]]
          [:dispatch [::notification.events/show "Transaction to end job processed successfully"]]]}))
