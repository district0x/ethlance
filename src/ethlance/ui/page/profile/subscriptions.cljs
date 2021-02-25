(ns ethlance.ui.page.profile.subscriptions
  (:require [ethlance.ui.page.profile.events :as profile.events]
            [ethlance.ui.subscription.utils :as subscription.utils]
            [re-frame.core :as re]))

(def create-get-handler #(subscription.utils/create-get-handler profile.events/state-key %))

(re/reg-sub
  ::job-roles
  (fn [db _] (:job-role-search db)))

(defn- prepare-role-data [job-role]
  (select-keys job-role [:job-id :start-date :title :status]))

(re/reg-sub
  ::candidate-jobs
  :<- [::job-roles]
  (fn [job-roles [_ candidate-id]]
    (let [roles (get-in job-roles [candidate-id "CANDIDATE"])]
      (map prepare-role-data roles))))

(re/reg-sub
  ::employer-jobs
  :<- [::job-roles]
  (fn [job-roles [_ employer-id]]
    (let [roles (get-in job-roles [employer-id "EMPLOYER"])]
          (map prepare-role-data roles))))

(re/reg-sub
  ::arbiter-jobs
  :<- [::job-roles]
  (fn [job-roles [_ arbiter-id]]
    (let [roles (get-in job-roles [arbiter-id "ARBITER"])]
          (map prepare-role-data roles))))
