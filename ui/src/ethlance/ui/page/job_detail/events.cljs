(ns ethlance.ui.page.job-detail.events
  (:require [district.ui.router.effects :as router.effects]
            [ethlance.ui.event.utils :as event.utils]
            [ethlance.ui.graphql :as graphql]
            [district.ui.web3-accounts.queries :as accounts-queries]
            [re-frame.core :as re]))

;; Page State
(def state-key :page.job-detail)
(def state-default
  {})

(def interceptors [re/trim-v])

(defn initialize-page
  "Event FX Handler. Setup listener to dispatch an event when the page is active/visited."
  []
  {::router.effects/watch-active-page
   [{:id :page.job-detail/initialize-page
     :name :route.job/detail
     :dispatch [:page.job-detail/fetch-proposals]}]})

;;
;; Registered Events
;;
(def create-assoc-handler (partial event.utils/create-assoc-handler state-key))

;; TODO: switch based on dev environment
(re/reg-event-fx :page.job-detail/initialize-page initialize-page)
(re/reg-event-fx :page.job-detail/set-proposal-token-amount (create-assoc-handler :job/proposal-token-amount))
(re/reg-event-fx :page.job-detail/set-proposal-text (create-assoc-handler :job/proposal-text))

(re/reg-event-fx
  :page.job-proposal/send
  [interceptors]
  (fn [{:keys [db]} [contract-address]]
    (let [user-address (accounts-queries/active-account db)
          text (get-in db [state-key :job/proposal-text])
          token-amount (get-in db [state-key :job/proposal-token-amount])
          proposal {:contract contract-address
                    :text text
                    :rate token-amount}]
      {:dispatch [::graphql/query
                  {:query
                   "mutation SendProposalMessage($input: ProposalInput) {
                     createJobProposal(input: $input) {
                       jobStory_id
                       jobStory_status
                       jobStory_proposalRate
                       jobStory_dateCreated
                       jobStory_candidate
                       job_contract
                       candidate {user {user_name user_address}}
                       jobStory_proposalMessage {message_id message_type message_text message_creator}
                     }}"
                   :variables {:input proposal}}]})))

(re/reg-event-fx
  :page.job-proposal/remove
  [interceptors]
  (fn [{:keys [db]} [job-story-id]]
    (let [user-address (accounts-queries/active-account db)]
      {:db (-> db
               (assoc-in ,,, [state-key :job/proposal-token-amount] nil)
               (assoc-in ,,, [state-key :job/proposal-text] nil))
       :dispatch [::graphql/query
                  {:query
                   "mutation RemoveProposal($jobStory_id: Int!) {
                     removeJobProposal(jobStory_id: $jobStory_id) {
                       jobStory_id
                       jobStory_status
                       jobStory_proposalRate
                       jobStory_dateCreated
                       jobStory_candidate
                       job_contract
                       candidate {user {user_name user_address}}
                       jobStory_proposalMessage {message_id message_type message_text message_creator}
                     }}"
                   :variables {:jobStory_id job-story-id}}]})))


(re/reg-event-fx
  :page.job-detail/fetch-proposals
  [interceptors]
  (fn [{:keys [db]} [_ router-params]]
    (let [queried-contract-address (:contract router-params)
          contract-from-db (get-in db [:district.ui.router :active-page :params :contract])
          contract (or queried-contract-address contract-from-db)]
      {:dispatch [::graphql/query
                  {:query
                   "query JobStoriesForProposal($jobContract: ID) {
                     jobStoryList(jobContract: $jobContract) {
                       jobStory_id
                       job_contract
                       jobStory_status
                       jobStory_proposalRate
                       jobStory_dateCreated
                       jobStory_candidate
                       candidate {user {user_name user_address}}
                       jobStory_proposalMessage {message_id message_type message_text message_creator}
                     }}"
                   :variables {:jobContract contract}}]})))
