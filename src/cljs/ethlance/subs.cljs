(ns ethlance.subs
  (:require
    [ethlance.utils :as u]
    [medley.core :as medley]
    [re-frame.core :refer [reg-sub]]))

(reg-sub
  :name
  (fn [db]
    (:name db)))

(reg-sub
  :db/current-page
  (fn [db _]
    (:active-page db)))

(reg-sub
  :db/drawer-open?
  (fn [db _]
    (:drawer-open? db)))

(reg-sub
  :db/active-address
  (fn [db _]
    (:active-address db)))

(reg-sub
  :db/my-addresses
  (fn [db _]
    (:my-addresses db)))

(reg-sub
  :db/active-user-id
  (fn [db]
    ((:address->user-id db) (:active-address db))))

(reg-sub
  :app/users
  (fn [db]
    (:app/users db)))

(reg-sub
  :db/active-user
  :<- [:db/active-user-id]
  :<- [:app/users]
  (fn [[active-user-id users]]
    (get users active-user-id)))

(reg-sub
  :form/search-job
  (fn [db]
    (:form/search-job db)))

(reg-sub
  :list/search-job
  (fn [db]
    (let [jobs (:list/search-job db)]
      (-> jobs
        (update :items (partial map #(get-in db [:app/jobs %])))
        (update :items (partial filter :job/id))))))

(reg-sub
  :form/search-job-skills
  (fn [db]
    (:search/skills (:form/search-job db))))

(reg-sub
  :app/skills
  (fn [db]
    (:app/skills db)))