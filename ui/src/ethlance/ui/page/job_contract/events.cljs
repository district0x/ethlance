(ns ethlance.ui.page.job-contract.events
  (:require [district.ui.router.effects :as router.effects]
            [ethlance.ui.event.utils :as event.utils]
            [ethlance.shared.utils :refer [eth->wei base58->hex]]
            [district.ui.graphql.events :as gql-events]
            [district.ui.web3-tx.events :as web3-events]
            ["web3" :as w3]
            [ethlance.shared.contract-constants :as contract-constants]
            [district.ui.smart-contracts.queries :as contract-queries]
            [district.ui.web3-accounts.queries :as accounts-queries]
            [district.ui.web3.queries]
            [district0x.re-frame.web3-fx]
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
(defn create-logging-handler
  ([] (create-logging-handler ""))
  ([text] (fn [db args] (println ">>> Received event in" state-key " " text " with args:" args))))

(re/reg-event-fx :page.job-contract/initialize-page initialize-page)
(re/reg-event-fx :page.job-contract/set-message-text (create-assoc-handler :message-text))
(re/reg-event-fx :page.job-contract/set-message-recipient (create-assoc-handler :message-recipient))

(re/reg-event-fx :page.job-contract/set-accept-proposal-message-text
                 (create-assoc-handler :accept-proposal-message-text))
(re/reg-event-fx :page.job-contract/set-accept-invitation-message-text
                 (create-assoc-handler :accept-invitation-message-text))

(re/reg-event-fx :page.job-contract/set-dispute-text (create-assoc-handler :dispute-text))
(re/reg-event-fx :page.job-contract/set-dispute-candidate-percentage (create-assoc-handler :dispute-candidate-percentage))

(re/reg-event-fx :page.job-contract/set-feedback-rating (create-assoc-handler :feedback-rating))
(re/reg-event-fx :page.job-contract/set-feedback-text (create-assoc-handler :feedback-text))
(re/reg-event-fx :page.job-contract/set-feedback-recipient (create-assoc-handler :feedback-recipient))

(re/reg-event-db :page.job-contract/dispute-to-ipfs-failure (create-logging-handler))
(re/reg-event-fx :page.job-contract/tx-hash (create-logging-handler))
(re/reg-event-db ::dispute-tx-error (create-logging-handler))

(defn clear-forms [db]
  (let [field-names [:message-text
                     :message-recipient
                     :dispute-text
                     :dispute-candidate-percentage
                     :feedback-rating
                     :feedback-text
                     :feedback-recipient
                     :accept-invitation-message-text
                     :accept-proposal-message-text]]
    (reduce (fn [acc field] (assoc-in acc [state-key field] nil)) db field-names)))

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
    {:fx [[:dispatch [::gql-events/mutation {:queries [[:leave-feedback mutation-params]]
                                             :id :SendEmployerFeedbackMutation}]]
          [:dispatch [:page.job-contract/refetch-messages]]
          [:dispatch [:page.job-contract/clear-message-forms]]]}))

(defn send-message
  [{:keys [db]} [_event-name params]]
  (let [job-story-id (:job-story/id params)
        text (:text params)
        to (:to params)
        mutation-params {:job-story/id job-story-id
                        :text text
                        :to to}]
    {:db (clear-forms db)
     :fx [[:dispatch [::gql-events/mutation
                      {:queries [[:send-message mutation-params]]
                       :id :SendDirectMessageMutation}]]
          [:dispatch [:page.job-contract/refetch-messages]]]}))

(defn accept-invitation
  [{:keys [db]} [_event-name params]]
  (let [job-story-id (:job-story/id params)
        text (:text params)
        to (:to params)
        mutation-params {:job-story/id job-story-id
                         :job-story-message/type :accept-invitation
                         :message/type :job-story-message
                         :text text
                         :to to}]
    {:db (clear-forms db)
     :fx [[:dispatch [::gql-events/mutation
                      {:queries [[:send-message mutation-params]]
                       :id :SendDirectMessageMutation}]]
          [:dispatch [:page.job-contract/refetch-messages]]]}))

(defn raise-dispute [{:keys [db]}
                     [_ {invoice-id :invoice/id job-id :job/id job-story-id :job-story/id :as event}]]
  (let [ipfs-dispute {:message/text (get-in db [state-key :dispute-text])
                      :message/creator (accounts-queries/active-account db)
                      :job/id job-id
                      :job-story/id job-story-id
                      :invoice/id invoice-id}]
    {:ipfs/call {:func "add"
                 :args [(js/Blob. [ipfs-dispute])]
                 :on-success [:page.job-contract/raise-dispute-to-ipfs-success ipfs-dispute]
                 :on-error [:page.job-contract/dispute-to-ipfs-failure invoice-id]}}))


(re/reg-event-fx
  :page.job-contract/raise-dispute-to-ipfs-success
  (fn [{:keys [db]} [_ dispute-details ipfs-event]]
    (let [creator (:message/creator dispute-details)
          ipfs-hash (base58->hex (:Hash ipfs-event))
          invoice-id (:invoice/id dispute-details)
          job-contract-address (:job/id dispute-details)
          tx-opts {:from creator :gas 10000000}]
       {:dispatch [::web3-events/send-tx
                  {:instance (contract-queries/instance db :job job-contract-address)
                   :fn :raiseDispute
                   :args [invoice-id ipfs-hash]
                   :tx-opts tx-opts
                   :tx-hash [::tx-hash]
                   :on-tx-hash-error [::tx-hash-error]
                   :on-tx-success [::dispute-tx-success]
                   :on-tx-error [::dispute-tx-error]}]})))

(re/reg-event-fx
  :page.job-contract/accept-proposal
  (fn [{:keys [db]} [_ proposal-data]]
    (let [to-ipfs {:candidate (:candidate proposal-data)
                   :employer (:employer proposal-data)
                   :job-story-message/type :accept-proposal
                   :job-story/id (:job-story/id proposal-data)
                   :job/id (:job/id proposal-data)
                   :message/creator (:employer proposal-data)
                   :text (:text proposal-data)}]
      {:ipfs/call {:func "add"
                   :args [(js/Blob. [to-ipfs])]
                   :on-success [:accept-proposal-to-ipfs-success to-ipfs]
                   :on-error [:accept-proposal-to-ipfs-failure to-ipfs]}})))

(re/reg-event-fx
  :accept-proposal-to-ipfs-success
  (fn [{:keys [db]} [_event ipfs-accept ipfs-event]]
    (let [creator (:employer ipfs-accept)
          ipfs-hash (base58->hex (:Hash ipfs-event))
          job-contract-address (:job/id ipfs-accept)
          candidate (:candidate ipfs-accept)
          tx-opts {:from creator :gas 10000000}]
       {:dispatch [::web3-events/send-tx
                  {:instance (contract-queries/instance db :job job-contract-address)
                   :fn :add-candidate
                   :args [candidate ipfs-hash]
                   :tx-opts tx-opts
                   :tx-hash [::tx-hash]
                   :on-tx-hash-error [::tx-hash-error]
                   :on-tx-success [::accept-proposal-tx-success]
                   :on-tx-error [::accept-proposal-tx-failure]}]})))

(re/reg-event-fx
  ::accept-proposal-to-ipfs-failure
  (fn [{:keys [db]} event]
    (println ">>> :invitation-to-ipfs-failure" event)))

(re/reg-event-fx
  ::accept-proposal-tx-success
  (fn [{:keys [db]} event]
    (println ">>> ::send-invitation-tx-success" event)
    {:db (clear-forms db)
     :fx [[:dispatch [:page.job-contract/refetch-messages]]]}))

(re/reg-event-fx
  ::accept-proposal-tx-failure
  (fn [{:keys [db]} event]
    (println ">>> ::send-invitation-tx-failure" event)))

(re/reg-event-fx
  :page.job-contract/clear-message-forms
  (fn [{:keys [db]}]
    {:db (clear-forms db)}))

(re/reg-event-fx
  ::dispute-tx-success
  (fn [{:keys [db]} [event-name tx-data]]
    ; TODO: clear & disable form
    (println ">>> ethlance.ui.page.job-contract.events ::raise-dispute-tx-success" tx-data)
    {:db (clear-forms db)
     :fx [[:dispatch [:page.job-contract/refetch-messages]]]}))

(defn send-resolve-dispute-ipfs
  [{:keys [db]} [_ {invoice-id :invoice/id
                    job-id :job/id
                    job-story-id :job-story/id
                    :as event}]]
  (let [ipfs-dispute {:message/text (get-in db [state-key :dispute-text])
                      :message/creator (accounts-queries/active-account db)
                      :job/id job-id
                      :job-story/id job-story-id
                      :invoice/id invoice-id}]
    (println ">>> ethlance.ui.page.job-contract.events/raise-dispute to ipfs: " event)
    {:ipfs/call {:func "add"
                 :args [(js/Blob. [ipfs-dispute])]
                 :on-success [:page.job-contract/resolve-dispute-to-ipfs-success event]
                 :on-error [:page.job-contract/dispute-to-ipfs-failure event]}}))

(defn send-resolve-dispute-tx [cofx [_event-name forwarded-event-data ipfs-data]]
  (let [creator (accounts-queries/active-account (:db cofx))
        job-address (:job/id forwarded-event-data)
        invoice-id (:invoice/id forwarded-event-data)
        token-type (:token-type forwarded-event-data)
        raw-amount (:token-amount forwarded-event-data)
        token-address (:token-address forwarded-event-data)
        token-id (:token-id forwarded-event-data)
        address-placeholder "0x0000000000000000000000000000000000000000"
        token-address (if (not (= token-type :eth))
                        token-address
                        address-placeholder)
        offered-value {:value (str raw-amount)
                       :token
                       {:tokenId token-id
                        :tokenContract
                        {:tokenType (contract-constants/token-type->enum-val token-type)
                         :tokenAddress token-address}}}
        instance (contract-queries/instance (:db cofx) :job job-address)
        tx-opts {:from creator :gas 10000000}
        ipfs-hash (base58->hex (:Hash ipfs-data))
        contract-args [invoice-id [(clj->js offered-value)] ipfs-hash]]
    {:dispatch [::web3-events/send-tx
                {:instance instance
                 :fn :resolve-dispute
                 :args contract-args
                 :tx-opts tx-opts
                 :tx-hash [:page.job-contract/tx-hash]
                 :on-tx-hash-error [::dispute-tx-error]
                 :on-tx-success [::dispute-tx-success]
                 :on-tx-error [::dispute-tx-error]}]}))

(re/reg-event-fx :page.job-contract/resolve-dispute send-resolve-dispute-ipfs)
(re/reg-event-fx :page.job-contract/resolve-dispute-to-ipfs-success send-resolve-dispute-tx)
(re/reg-event-fx :page.job-contract/send-message send-message)
(re/reg-event-fx :page.job-contract/accept-invitation accept-invitation)
(re/reg-event-fx :page.job-contract/raise-dispute raise-dispute)
(re/reg-event-fx :page.job-contract/send-feedback send-feedback)
