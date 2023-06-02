(ns ethlance.ui.page.profile.events
  (:require [district.ui.router.effects :as router.effects]
            [district.ui.router.queries :refer [active-page-params]]
            [ethlance.ui.event.utils :as event.utils]
            [ethlance.shared.utils :refer [eth->wei base58->hex]]
            [district.ui.smart-contracts.queries :as contract-queries]
            [district.ui.web3-tx.events :as web3-events]
            [ethlance.ui.graphql :as graphql]
            [re-frame.core :as re]))

;; Page State
(def state-key :page.profiles)
(def state-default
  {})

(defn initialize-page
  "Event FX Handler. Setup listener to dispatch an event when the page is active/visited."
  []
  {::router.effects/watch-active-page
   [{:id :page.profile/initialize-page
     :name :route.user/profile
     :dispatch []}]})

(re/reg-event-fx
  :query-profile-page-data
  (fn [coeff _val]
    (let [
          query "query ($address: ID!) {
                  candidate(user_id: $address) {user_id candidate_feedback {items {feedback_rating feedback_text feedback_fromUser {user_name}} totalCount}}
                  employer(user_id: $address) {user_id employer_feedback {items {feedback_rating feedback_text feedback_fromUser {user_name}} totalCount}}
                  arbiter(user_id: $address) {user_id arbiter_feedback {items {feedback_rating feedback_text feedback_fromUser {user_name}} totalCount}}
                }"
          user-address (-> coeff :db active-page-params :address)]
      {:dispatch [::graphql/query {:query query :variables {:address user-address}}]})))

;;
;; Registered Events
;;
(def create-assoc-handler (partial event.utils/create-assoc-handler state-key))

(re/reg-event-fx :page.profile/initialize-page initialize-page)
(re/reg-event-fx :page.profile/set-job-for-invitation (create-assoc-handler :job-for-invitation))
(re/reg-event-fx :page.profile/set-invitation-text (create-assoc-handler :invitation-text))

(re/reg-event-fx
  :page.profile/send-invitation
  (fn [{:keys [db]} [_ invitation-data]]
    (let [
          ipfs-invitation {:job-story-message/type :invitation
                           :job/id (:job invitation-data)
                           :candidate (:candidate invitation-data)
                           :employer (:employer invitation-data)
                           :text (:text invitation-data)
                           :message/creator (:inviter invitation-data)}]
      {:ipfs/call {:func "add"
                   :args [(js/Blob. [ipfs-invitation])]
                   :on-success [:invitation-to-ipfs-success ipfs-invitation]
                   :on-error [:invitation-to-ipfs-failure ipfs-invitation]}})))

(re/reg-event-fx
  :invitation-to-ipfs-success
  (fn [{:keys [db]} [_event ipfs-invitation ipfs-event]]
    (println ">>> :invitation-to-ipfs-success" _event ipfs-invitation ipfs-event)
    (let [creator (:inviter ipfs-invitation)
          ipfs-hash (base58->hex (:Hash ipfs-event))
          job-contract-address (:job/id ipfs-invitation)
          candidate (:candidate ipfs-invitation)
          tx-opts {:from creator :gas 10000000}]
       {:dispatch [::web3-events/send-tx
                  {:instance (contract-queries/instance db :job job-contract-address)
                   :fn :add-candidate
                   :args [candidate ipfs-hash]
                   :tx-opts tx-opts
                   :tx-hash [::tx-hash]
                   :on-tx-hash-error [::tx-hash-error]
                   :on-tx-success [::send-invitation-tx-success]
                   :on-tx-error [::send-invitation-tx-failure]}]})))

(re/reg-event-db
  ::invitation-to-ipfs-failure
  (fn [db event]
    (println ">>> :invitation-to-ipfs-failure" event)
    db))

(re/reg-event-db
  ::send-invitation-tx-success
  (fn [db event]
    (println ">>> ::send-invitation-tx-success" event)
    db))

(re/reg-event-db
  ::send-invitation-tx-failure
  (fn [db event]
    (println ">>> ::send-invitation-tx-failure" event)
    db))
