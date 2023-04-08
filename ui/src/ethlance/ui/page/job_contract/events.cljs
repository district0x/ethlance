(ns ethlance.ui.page.job-contract.events
  (:require [district.ui.router.effects :as router.effects]
            [ethlance.ui.event.utils :as event.utils]
            [ethlance.shared.utils :refer [eth->wei base58->hex]]
            [district.ui.graphql.events :as gql-events]
            [district.ui.web3-tx.events :as web3-events]
            [district.ui.smart-contracts.queries :as contract-queries]
            [district.ui.web3-accounts.queries :as accounts-queries]
            [re-frame.core :as re]))

;; Page State
(def state-key :page.job-contract)
(def state-default
  {})

(defn initialize-page
  "Event FX Handler. Setup listener to dispatch an event when the page is active/visited."
  []
  {::router.effects/watch-active-page
   [{:id :page.job-contract/initialize-page
     :name :route.job/contract
     :dispatch []}]})

;;
;; Registered Events
;;
(def create-assoc-handler (partial event.utils/create-assoc-handler state-key))

;; TODO: switch based on dev environment
(re/reg-event-fx :page.job-contract/initialize-page initialize-page)
(re/reg-event-fx :page.job-contract/set-message-text (create-assoc-handler :message-text))
(re/reg-event-fx :page.job-contract/set-message-recipient (create-assoc-handler :message-recipient))

(re/reg-event-fx :page.job-contract/set-dispute-text (create-assoc-handler :dispute-text))
(re/reg-event-fx :page.job-contract/set-dispute-candidate-percentage (create-assoc-handler :dispute-candidate-percentage))

(re/reg-event-fx :page.job-contract/set-feedback-rating (create-assoc-handler :feedback-rating))
(re/reg-event-fx :page.job-contract/set-feedback-text (create-assoc-handler :feedback-text))
(re/reg-event-fx :page.job-contract/set-feedback-recipient (create-assoc-handler :feedback-recipient))

(defn send-feedback
  [{:keys [db]} [_event-name params]]
  (let [job-story-id (:job-story/id params)
        text (:text params)
        rating (:rating params)
        to (:to params)
        mutation-params {:job-story/id job-story-id
                        :text text
                        :rating rating
                        :to to}]
    {:dispatch [::gql-events/mutation
                {:queries [[:leave-feedback mutation-params]]
                 :id :SendEmployerFeedbackMutation}]}))

(defn send-message
  [{:keys [db]} [_event-name params]]
  (let [job-story-id (:job-story/id params)
        text (:text params)
        to (:to params)
        mutation-params {:job-story/id job-story-id
                        :text text
                        :to to}]
    {:dispatch [::gql-events/mutation
                {:queries [[:send-message mutation-params]]
                 :id :SendDirectMessageMutation}]}))

(defn raise-dispute [{:keys [db]} [_ {invoice-id :invoice/id job-id :job/id job-story-id :job-story/id :as event}]]
  (let [ipfs-dispute {:message/text (get-in db [state-key :dispute-text])
                      :message/creator (accounts-queries/active-account db)
                      :job/id job-id
                      :job-story/id job-story-id
                      :invoice/id invoice-id}]
    (println ">>> ethlance.ui.page.job-contract.events/raise-dispute to ipfs: " event)
    {:ipfs/call {:func "add"
                 :args [(js/Blob. [ipfs-dispute])]
                 :on-success [::dispute-to-ipfs-success ipfs-dispute]
                 :on-error [::dispute-to-ipfs-failure invoice-id]}}))

(re/reg-event-db
  ::dispute-to-ipfs-failure
  (fn [db event]
    (println ">>> EVENT ::dispute-to-ipfs-failure" event)
    db))

(re/reg-event-fx
  ::dispute-to-ipfs-success
  (fn [{:keys [db]} [_ dispute-details ipfs-event]]
    (let [creator (:message/creator dispute-details)
          ipfs-hash (base58->hex (:Hash ipfs-event))
          invoice-id (:invoice/id dispute-details)
          job-contract-address (:job/id dispute-details)
          tx-opts {:from creator :gas 10000000}
          ]
      (println ">>> ethlance.ui.page.job-contract.events ::dispute-to-ipfs-success"
               {:creator creator :ipfs-event ipfs-event :ipfs-hash ipfs-hash :job-contract job-contract-address})
       {:dispatch [::web3-events/send-tx
                  {:instance (contract-queries/instance db :job job-contract-address)
                   :fn :raiseDispute
                   :args [invoice-id ipfs-hash]
                   :tx-opts tx-opts
                   :tx-hash [::tx-hash]
                   :on-tx-hash-n [[::tx-hash]]
                   :on-tx-hash-error [::tx-hash-error]
                   :on-tx-hash-error-n [[::tx-hash-error]]
                   :on-tx-success [::send-dispute-tx-success]
                   :on-tx-success-n [[::send-dispute-tx-success]]
                   :on-tx-error [::send-dispute-tx-error]
                   :on-tx-error-n [[::send-dispute-tx-error]]}]})))


(re/reg-event-fx
  ::tx-hash
  (fn [db event] (println ">>> ethlance.ui.page.job-contract.events ::tx-hash" event)))

(re/reg-event-db
  ::raise-dispute-tx-success
  (fn [db [event-name tx-data]]
    ; TODO: clear & disable form
    (println ">>> ethlance.ui.page.job-contract.events ::raise-dispute-tx-success" tx-data)
    ; (re/dispatch [::router-events/navigate
    ;               :route.job/detail
    ;               {:contract (get-in tx-data [:events :Job-created :return-values :job])}])
    ))

(re/reg-event-db
  ::create-job-tx-error
  (fn [db event]
    (println ">>> got :create-job-tx-error event:" event)))

; TODO: probably can be removed - raising the dispute happens via Ethereum Tx &
;       event syncing on the server side
; (defn raise-dispute
;   [{:keys [db]} [_event-name params]]
;   (let [job-story-id (:job-story/id params)
;         text (:text params)
;         mutation-params {:job-story/id job-story-id :text text}]
;     {:dispatch [::gql-events/mutation
;                 {:queries [[:raise-dispute mutation-params]]
;                  :id :RaiseDisputeMutation}]}))

(re/reg-event-fx :page.job-contract/send-message send-message)
(re/reg-event-fx :page.job-contract/raise-dispute raise-dispute)
(re/reg-event-fx :page.job-contract/send-feedback send-feedback)
