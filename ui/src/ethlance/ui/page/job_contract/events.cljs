(ns ethlance.ui.page.job-contract.events
  (:require
    [district.ui.graphql.events :as gql-events]
    [district.ui.notification.events :as notification.events]
    [district.ui.router.effects :as router.effects]
    [district.ui.smart-contracts.queries :as contract-queries]
    [district.ui.web3-accounts.queries :as accounts-queries]
    [district.ui.web3-tx.events :as web3-events]
    [district.ui.web3.queries]
    [district0x.re-frame.web3-fx]
    [ethlance.shared.contract-constants :as contract-constants]
    [ethlance.shared.utils :refer [base58->hex]]
    [ethlance.ui.event.utils :as event.utils]
    [re-frame.core :as re]))


;; Page State
(def state-key :page.job-contract)


(defn initialize-page
  "Event FX Handler. Setup listener to dispatch an event when the page is active/visited."
  []
  {:fx [[:dispatch [:page.job-contract/set-dispute-candidate-percentage "50"]]]
   ::router.effects/watch-active-page
   [{:id :page.job-contract/initialize-page
     :name :route.job/contract
     :dispatch []}]})


;;
;; Registered Events
;;
(def create-assoc-handler (partial event.utils/create-assoc-handler state-key))


(defn create-logging-handler
  ([] (create-logging-handler ""))
  ([text] (fn [_db args] (println ">>> Received event in" state-key " " text " with args:" args))))


(re/reg-event-fx :page.job-contract/initialize-page initialize-page)
(re/reg-event-fx :page.job-contract/set-message-text (create-assoc-handler :message-text))


(re/reg-event-fx :page.job-contract/set-accept-proposal-message-text
                 (create-assoc-handler :accept-proposal-message-text))


(re/reg-event-fx :page.job-contract/set-accept-invitation-message-text
                 (create-assoc-handler :accept-invitation-message-text))


(re/reg-event-fx :page.job-contract/set-dispute-text (create-assoc-handler :dispute-text))
(re/reg-event-fx :page.job-contract/set-dispute-candidate-percentage (create-assoc-handler :dispute-candidate-percentage))

(re/reg-event-fx :page.job-contract/set-feedback-rating (create-assoc-handler :feedback-rating))
(re/reg-event-fx :page.job-contract/set-feedback-text (create-assoc-handler :feedback-text))

(re/reg-event-fx :page.job-contract/tx-hash (create-logging-handler))


(defn clear-forms
  [db]
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
  [_cofx [_event-name params]]
  (let [job-story-id (:job-story/id params)
        text (:text params)
        rating (:rating params)
        to (:to params)
        mutation-params {:job-story/id job-story-id
                         :text text
                         :rating rating
                         :to to}]
    {:fx [[:dispatch [::gql-events/mutation {:queries [[:leave-feedback mutation-params]]
                                             :on-success [::graphql-mutation-success]
                                             :on-error [::graphql-mutation-error]
                                             :id :SendEmployerFeedbackMutation}]]
          [:dispatch [::set-buttons-disabled true]]]}))


(re/reg-event-fx
  ::graphql-mutation-success
  (fn [_cofx _event-v]
    {:fx [[:dispatch [::set-buttons-disabled false]]
          [:dispatch [:page.job-contract/refetch-messages]]
          [:dispatch [:page.job-contract/clear-message-forms]]]}))

(re/reg-event-fx
  ::graphql-mutation-error
  (fn [_cofx _event-v]
    {:fx [[:dispatch [::set-buttons-disabled false]]
          [:dispatch [:page.job-contract/refetch-messages]]]}))

(defn send-message
  [{:keys [db]} [_event-name params]]
  (let [job-story-id (:job-story/id params)
        text (:text params)
        mutation-params {:job-story/id job-story-id :text text}]
    {:db (clear-forms db)
     :fx [[:dispatch [::gql-events/mutation
                      {:queries [[:send-message mutation-params]]
                       :on-success [::graphql-mutation-success]
                       :on-error [::graphql-mutation-error]
                       :id :SendDirectMessageMutation}]]
          [:dispatch [::set-buttons-disabled true]]]}))


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


(defn raise-dispute
  [{:keys [db]}
   [_ {invoice-id :invoice/id job-id :job/id job-story-id :job-story/id}]]
  (let [ipfs-dispute {:message/text (get-in db [state-key :dispute-text])
                      :message/creator (accounts-queries/active-account db)
                      :job/id job-id
                      :job-story/id job-story-id
                      :invoice/id invoice-id}]
    {:fx [[:dispatch [::set-buttons-disabled true]]]
     :ipfs/call {:func "add"
                 :args [(js/Blob. [ipfs-dispute])]
                 :on-success [:page.job-contract/raise-dispute-to-ipfs-success ipfs-dispute]
                 :on-error [::dispute-to-ipfs-failure invoice-id]}}))


(re/reg-event-fx
  :page.job-contract/raise-dispute-to-ipfs-success
  (fn [{:keys [db]} [_ dispute-details ipfs-event]]
    (let [creator (:message/creator dispute-details)
          ipfs-hash (base58->hex (:Hash ipfs-event))
          invoice-id (:invoice/id dispute-details)
          job-contract-address (:job/id dispute-details)
          tx-opts {:from creator}]
      {:fx [[:dispatch [::set-buttons-disabled true]]
            [:dispatch [::web3-events/send-tx
                        {:instance (contract-queries/instance db :job job-contract-address)
                         :fn :raiseDispute
                         :args [invoice-id ipfs-hash]
                         :tx-opts tx-opts
                         :tx-hash [::tx-hash]
                         :on-tx-hash-error [::tx-hash-error]
                         :on-tx-success [::dispute-tx-success "Transaction to raise dispute processed successfully"]
                         :on-tx-error [::dispute-tx-error]}]]]})))


(re/reg-event-db
  ::set-buttons-disabled
  (fn [db [_ disabled?]]
    (assoc-in db [state-key :buttons-disabled?] disabled?)))

(re/reg-event-fx
  :page.job-contract/accept-proposal
  (fn [_cofx [_ proposal-data]]
    (let [to-ipfs {:candidate (:candidate proposal-data)
                   :employer (:employer proposal-data)
                   :job-story-message/type :accept-proposal
                   :job-story/id (:job-story/id proposal-data)
                   :job/id (:job/id proposal-data)
                   :message/creator (:employer proposal-data)
                   :text (:text proposal-data)}]
      {:fx [[:dispatch [::set-buttons-disabled true]]]
       :ipfs/call {:func "add"
                   :args [(js/Blob. [to-ipfs])]
                   :on-success [:accept-proposal-to-ipfs-success to-ipfs]
                   :on-error [::accept-proposal-to-ipfs-failure to-ipfs]}})))


(re/reg-event-fx
  :accept-proposal-to-ipfs-success
  (fn [{:keys [db]} [_event ipfs-accept ipfs-event]]
    (let [creator (:employer ipfs-accept)
          ipfs-hash (base58->hex (:Hash ipfs-event))
          job-contract-address (:job/id ipfs-accept)
          candidate (:candidate ipfs-accept)
          tx-opts {:from creator}]
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
  (fn [_cofx event]
    {:fx [[:dispatch [::set-buttons-disabled false]]]}))


(re/reg-event-fx
  ::accept-proposal-tx-success
  (fn [{:keys [db]} event]
    {:db (clear-forms db)
     :fx [[:dispatch [::set-buttons-disabled false]]
          [:dispatch [:page.job-contract/refetch-messages]]
          [:dispatch [::notification.events/show "Transaction to accept proposal processed successfully"]]]}))


(re/reg-event-fx
  ::tx-hash-error
  (fn [_cofx _event]
    {:fx [[:dispatch [::set-buttons-disabled false]]
          [:dispatch [::notification.events/show "Error, the transaction was not sent"]]]}))

(re/reg-event-fx
  ::accept-proposal-tx-failure
  (fn [_cofx _event]
    {:fx [[:dispatch [::set-buttons-disabled false]]
          [:dispatch [::notification.events/show "Error processing accept proposal transaction"]]]}))


(re/reg-event-fx
  :page.job-contract/clear-message-forms
  (fn [{:keys [db]}]
    {:db (clear-forms db)}))


(re/reg-event-fx
  ::dispute-tx-success
  (fn [{:keys [db]} [_event-name message]]
    {:db (clear-forms db)
     :fx [[:dispatch [::set-buttons-disabled false]]
          [:dispatch [:page.job-contract/refetch-messages]]
          [:dispatch [::notification.events/show message]]]}))

(re/reg-event-fx
  ::dispute-to-ipfs-failure
  (fn [_cofx _]
    {:fx [[:dispatch [::set-buttons-disabled false]]]}))

(re/reg-event-db
  ::dispute-tx-error
  (fn [_cofx _]
    {:fx [[:dispatch [::set-buttons-disabled false]]]}))

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
    {:ipfs/call {:func "add"
                 :args [(js/Blob. [ipfs-dispute])]
                 :on-success [:page.job-contract/resolve-dispute-to-ipfs-success event]
                 :on-error [::dispute-to-ipfs-failure event]}}))


(defn send-resolve-dispute-tx
  [cofx [_event-name forwarded-event-data ipfs-data]]
  (let [creator (accounts-queries/active-account (:db cofx))
        job-address (:job/id forwarded-event-data)
        invoice-id (:invoice/id forwarded-event-data)
        token-type (:token-type forwarded-event-data)
        raw-amount (:token-amount forwarded-event-data)
        amount (case token-type
                 :eth raw-amount
                 :erc20 (Math/floor raw-amount)
                 :erc721 1
                 :erc1155 (Math/floor raw-amount))
        token-address (:token-address forwarded-event-data)
        token-id (:token-id forwarded-event-data)
        address-placeholder "0x0000000000000000000000000000000000000000"
        token-address (if (not (= token-type :eth))
                        token-address
                        address-placeholder)
        offered-value {:value (str amount)
                       :token
                       {:tokenId token-id
                        :tokenContract
                        {:tokenType (contract-constants/token-type->enum-val token-type)
                         :tokenAddress token-address}}}
        instance (contract-queries/instance (:db cofx) :job job-address)
        tx-opts {:from creator}
        ipfs-hash (base58->hex (:Hash ipfs-data))
        contract-args [invoice-id [(clj->js offered-value)] ipfs-hash]]
    {:dispatch [::web3-events/send-tx
                {:instance instance
                 :fn :resolve-dispute
                 :args contract-args
                 :tx-opts tx-opts
                 :tx-hash [:page.job-contract/tx-hash]
                 :on-tx-hash-error [::dispute-tx-error]
                 :on-tx-success [::dispute-tx-success "Transaction to resolve dispute processed successfully"]
                 :on-tx-error [::dispute-tx-error]}]}))


(re/reg-event-fx :page.job-contract/resolve-dispute send-resolve-dispute-ipfs)
(re/reg-event-fx :page.job-contract/resolve-dispute-to-ipfs-success send-resolve-dispute-tx)
(re/reg-event-fx :page.job-contract/send-message send-message)
(re/reg-event-fx :page.job-contract/accept-invitation accept-invitation)
(re/reg-event-fx :page.job-contract/raise-dispute raise-dispute)
(re/reg-event-fx :page.job-contract/send-feedback send-feedback)
