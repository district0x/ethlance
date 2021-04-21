(ns ethlance.ui.page.profile.events
  (:require [district.ui.router.effects :as router.effects]
            [district.ui.router.queries :refer [active-page-params]]
            [ethlance.ui.event.utils :as event.utils]
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
     :dispatch [:query-profile-page-data]}]})

(re/reg-event-fx
  :query-profile-page-data
  (fn [coeff _val]
    (let [
          query "query ($address: ID!) {
                  candidate(user_address: $address) {user_address candidate_feedback {items {feedback_rating feedback_text feedback_fromUser {user_name}} totalCount}}
                  employer(user_address: $address) {user_address employer_feedback {items {feedback_rating feedback_text feedback_fromUser {user_name}} totalCount}}
                  arbiter(user_address: $address) {user_address arbiter_feedback {items {feedback_rating feedback_text feedback_fromUser {user_name}} totalCount}}
                }"
          user-address (-> coeff :db active-page-params :address)]
      {:dispatch [::graphql/query {:query query :variables {:address user-address}}]})))

;;
;; Registered Events
;;
(def create-assoc-handler (partial event.utils/create-assoc-handler state-key))

(re/reg-event-fx :page.profile/initialize-page initialize-page)
