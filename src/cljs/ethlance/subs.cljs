(ns ethlance.subs
  (:require
    [ethlance.utils :as u]
    [medley.core :as medley]
    [re-frame.core :refer [reg-sub]]))

(reg-sub
  :db
  (fn [db]
    db))

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
  :app/contracts
  (fn [db]
    (:app/contracts db)))

(reg-sub
  :app/invoices
  (fn [db]
    (:app/invoices db)))

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
        (update :items (partial map #(merge % (get-in db [:app/users (:job/employer %)]))))
        (u/list-filter-loaded :job/title)))))

(reg-sub
  :form/search-job-skills
  (fn [db]
    (:search/skills (:form/search-jobs db))))

(reg-sub
  :list/search-freelancers
  (fn [db]
    (let [jobs (:list/search-freelancers db)]
      (-> jobs
        (update :items (partial map #(get-in db [:app/users %])))
        (u/list-filter-loaded :freelancer/skills)))))

(reg-sub
  :form/search-freelancer-skills
  (fn [db]
    (:search/skills (:form/search-freelancers db))))

(reg-sub
  :app/skills
  (fn [db]
    (:app/skills db)))

(reg-sub
  :job/route-job-id
  :<- [:db/active-page]
  (fn [{:keys [route-params]}]
    (js/parseInt (:job/id route-params))))

(reg-sub
  :invoice/route-invoice-id
  :<- [:db/active-page]
  (fn [{:keys [route-params]}]
    (js/parseInt (:invoice/id route-params))))

(reg-sub
  :job/my-job?
  :<- [:job/route-job-id]
  :<- [:db/active-user-id]
  :<- [:app/jobs]
  (fn [[job-id user-id jobs]]
    (when-let [employer-id (get-in jobs [job-id :job/employer])]
      (= employer-id user-id))))

(reg-sub
  :job/detail
  :<- [:job/route-job-id]
  :<- [:app/jobs]
  :<- [:app/users]
  (fn [[job-id jobs users]]
    (-> (get jobs job-id)
      (update :job/employer #(get users %)))))

(reg-sub
  :list.ids/job-contracts
  (fn [db]
    (get-in db [:list/job-contracts :items])))

(reg-sub
  :list.ids/job-invoices
  (fn [db]
    (get-in db [:list/job-invoices :items])))

(reg-sub
  :list/job-contracts
  :<- [:db]
  :<- [:app/contracts]
  :<- [:app/users]
  (fn [[db contracts users]]
    (let [job-contracts (:list/job-contracts db)
          {:keys [offset limit]} job-contracts]
      (-> job-contracts
        (update :items #(u/paginate % offset limit))
        (update :items (partial map contracts))
        (update :items (partial map #(update % :contract/freelancer users)))
        (u/list-filter-loaded (comp :user/name :contract/freelancer))))))

(reg-sub
  :list/job-invoices
  :<- [:db]
  :<- [:app/invoices]
  :<- [:app/contracts]
  :<- [:app/users]
  (fn [[db invoices contracts users]]
    (let [job-invoices (:list/job-invoices db)
          {:keys [offset limit]} job-invoices]
      (-> job-invoices
        (update :items #(u/paginate % offset limit))
        (update :items (partial map invoices))
        (update :items (partial map #(update % :invoice/contract (partial get contracts))))
        (update :items (partial map #(update-in % [:invoice/contract :contract/freelancer]
                                                (partial get users))))
        (u/list-filter-loaded (comp :user/name :contract/freelancer :invoice/contract))))))

(reg-sub
  :contract/route-contract-id
  :<- [:db/active-page]
  (fn [{:keys [route-params]}]
    (js/parseInt (:contract/id route-params))))

(defn- remove-unallowed-contract-data [contract active-user-id]
  (if-not (or (= (get-in contract [:contract/freelancer :user/id]) active-user-id)
              (= (get-in contract [:contract/job :job/employer :user/id]) active-user-id))
    (merge contract {:invitation/description ""
                     :proposal/description ""
                     :contract/description ""})
    contract))

(reg-sub
  :contract/detail
  :<- [:contract/route-contract-id]
  :<- [:app/contracts]
  :<- [:app/jobs]
  :<- [:app/users]
  :<- [:db/active-user-id]
  (fn [[contract-id contracts jobs users active-user-id]]
    (-> (get contracts contract-id)
      (update :contract/job jobs)
      (update-in [:contract/job :job/employer] users)
      (update :contract/freelancer users)
      (remove-unallowed-contract-data active-user-id))))

(defn- remove-unallowed-invoice-data [invoice active-user-id]
  (if-not (or (= (get-in invoice [:invoice/contract :contract/freelancer :user/id]) active-user-id)
              (= (get-in invoice [:invoice/contract :contract/job :job/employer :user/id]) active-user-id))
    (merge invoice {:invoice/description ""})
    invoice))

(reg-sub
  :invoice/detail
  :<- [:invoice/route-invoice-id]
  :<- [:app/invoices]
  :<- [:app/contracts]
  :<- [:app/jobs]
  :<- [:app/users]
  :<- [:db/active-user-id]
  (fn [[invoice-id invoices contracts jobs users active-user-id]]
    (-> (get invoices invoice-id)
      (update :invoice/contract contracts)
      (update-in [:invoice/contract :contract/freelancer] users)
      (update-in [:invoice/contract :contract/job] jobs)
      (update-in [:invoice/contract :contract/job :job/employer] users)
      (remove-unallowed-invoice-data active-user-id))))

(reg-sub
  :invoice/by-me?
  :<- [:invoice/detail]
  :<- [:db/active-user-id]
  (fn [[invoice active-user-id]]
    (and active-user-id
         (= active-user-id (get-in invoice [:invoice/contract :contract/freelancer :user/id])))))

(reg-sub
  :invoice/for-me?
  :<- [:invoice/detail]
  :<- [:db/active-user-id]
  (fn [[invoice active-user-id]]
    (and active-user-id
         (= active-user-id (get-in invoice [:invoice/contract :contract/job :job/employer :user/id])))))

(reg-sub
  :form.invoice/pay
  (fn [db]
    (:form.invoice/pay db)))

(reg-sub
  :form.invoice/cancel
  (fn [db]
    (:form.invoice/cancel db)))