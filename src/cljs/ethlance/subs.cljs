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
  :app/jobs
  (fn [db]
    (:app/jobs db)))

(reg-sub
  :db/active-page
  (fn [db]
    (:active-page db)))

(reg-sub
  :db/active-user
  :<- [:db/active-user-id]
  :<- [:app/users]
  (fn [[active-user-id users]]
    (get users active-user-id)))

(reg-sub
  :form/search-jobs
  (fn [db]
    (:form/search-jobs db)))

(reg-sub
  :form/search-freelancers
  (fn [db]
    (:form/search-freelancers db)))

(reg-sub
  :list/search-jobs
  (fn [db]
    (let [jobs (:list/search-jobs db)]
      (-> jobs
        (update :items (partial map #(get-in db [:app/jobs %])))
        (update :items (partial map #(merge % (get-in db [:app/users (:job/employer %)]))))))))

(reg-sub
  :form/search-job-skills
  (fn [db]
    (:search/skills (:form/search-jobs db))))

(reg-sub
  :list/search-freelancers
  (fn [db]
    (let [jobs (:list/search-freelancers db)]
      (-> jobs
        (update :items (partial map #(get-in db [:app/users %])))))))

(reg-sub
  :form/search-freelancer-skills
  (fn [db]
    (:search/skills (:form/search-freelancers db))))

(reg-sub
  :app/skills
  (fn [db]
    (:app/skills db)))

(reg-sub
  :job/detail
  :<- [:db/active-page]
  :<- [:app/jobs]
  :<- [:app/users]
  (fn [[{:keys [route-params]} jobs users]]
    (-> (get jobs (js/parseInt (:job/id route-params)))
      (update :job/employer #(get users %)))))

