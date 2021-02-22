(ns ethlance.ui.page.profile.events
  (:require [district.ui.router.effects :as router.effects]
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
     :dispatch [:query-job-roles]}]})

(re/reg-event-fx
  :query-job-roles
  (fn [coeff val]
    (let [query "query JobRoleSearch($address: ID!) {
                jobRoleSearch(user_address: $address) {items {job {job_title} role}}}"
          ; TODO: Take from active page url, e.g. /user/:address/profile
          user-address "0xc238fa6ccc9d226e2c49644b36914611319fc3ff"]
      {:dispatch [::graphql/query {:query query :variables {:address user-address}}]})))

;;
;; Registered Events
;;
(def create-assoc-handler (partial event.utils/create-assoc-handler state-key))

(re/reg-event-fx :page.profile/initialize-page initialize-page)
