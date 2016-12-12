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
  :eth/config
  (fn [db _]
    (:eth/config db)))

(reg-sub
  :db/active-user-id
  (fn [db]
    ((:address->user-id db) (:active-address db))))

(reg-sub
  :db/active-user
  :<- [:db/active-user-id]
  :<- [:app/users]
  (fn [[user-id users]]
    (users user-id)))

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
  :db/snackbar
  (fn [db]
    (:snackbar db)))

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

(defn job-id->job [job-id jobs users]
  (-> (get jobs job-id)
    (update :job/employer #(get users %))))

(defn job-ids->jobs-list [jobs-list jobs users]
  (-> jobs-list
    (update :items (partial u/sort-paginate-ids jobs-list))
    (update :items (partial map #(job-id->job % jobs users)))
    (u/list-filter-loaded :job/title)))

(reg-sub
  :job/detail
  :<- [:job/route-job-id]
  :<- [:app/jobs]
  :<- [:app/users]
  (fn [[job-id jobs users]]
    (job-id->job job-id jobs users)))

(reg-sub
  :list/employer-jobs-open
  :<- [:db]
  :<- [:app/jobs]
  :<- [:app/users]
  (fn [[db jobs users]]
    (job-ids->jobs-list (:list/employer-jobs-open db) jobs users)))

(reg-sub
  :list/employer-jobs-done
  :<- [:db]
  :<- [:app/jobs]
  :<- [:app/users]
  (fn [[db jobs users]]
    (job-ids->jobs-list (:list/employer-jobs-done db) jobs users)))

(reg-sub
  :db/active-freelancer-job-detail-contract
  :<- [:db/active-user-id]
  :<- [:job/route-job-id]
  :<- [:app/contracts]
  (fn [[user-id job-id contracts]]
    (medley/find-first #(and (= (:contract/job %) job-id)
                             (= (:contract/freelancer %) user-id))
                       (vals contracts))))

(defn create-list-ids-fn [list-key]
  (fn [db]
    (let [{:keys [items sort-dir]} (get db list-key)]
      (u/sort-in-dir sort-dir items))))

(reg-sub :list.ids/job-proposals (create-list-ids-fn :list/job-proposals))
(reg-sub :list.ids/job-invoices (create-list-ids-fn :list/job-invoices))
(reg-sub :list.ids/employer-invoices-pending (create-list-ids-fn :list/employer-invoices-pending))
(reg-sub :list.ids/employer-invoices-paid (create-list-ids-fn :list/employer-invoices-paid))
(reg-sub :list.ids/freelancer-invoices-pending (create-list-ids-fn :list/freelancer-invoices-pending))
(reg-sub :list.ids/freelancer-invoices-paid (create-list-ids-fn :list/freelancer-invoices-paid))
(reg-sub :list.ids/freelancer-invitations (create-list-ids-fn :list/freelancer-invitations))
(reg-sub :list.ids/freelancer-proposals (create-list-ids-fn :list/freelancer-proposals))
(reg-sub :list.ids/freelancer-contracts-open (create-list-ids-fn :list/freelancer-contracts-open))
(reg-sub :list.ids/freelancer-contracts-done (create-list-ids-fn :list/freelancer-contracts-done))
(reg-sub :list.ids/employer-jobs-open (create-list-ids-fn :list/employer-jobs-open))
(reg-sub :list.ids/employer-jobs-done (create-list-ids-fn :list/employer-jobs-done))
(reg-sub :list.ids/job-feedbacks (create-list-ids-fn :list/job-feedbacks))
(reg-sub :list.ids/contract-invoices (create-list-ids-fn :list/contract-invoices))
(reg-sub :list.ids/job-proposals (create-list-ids-fn :list/job-proposals))


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

(defn contract-id->contract [contract-id contracts jobs users]
  (-> (get contracts contract-id)
    (update :contract/job jobs)
    (update-in [:contract/job :job/employer] users)
    (update :contract/freelancer users)))

(defn contract-ids->contracts-list [contracts-list contracts jobs users & [non-empty-pred]]
  (-> contracts-list
    (update :items (partial u/sort-paginate-ids contracts-list))
    (update :items (partial map #(contract-id->contract % contracts jobs users)))
    (u/list-filter-loaded (or non-empty-pred (comp :user/name :contract/freelancer)))))

(def contract-ids->contracts-list-job #(contract-ids->contracts-list %1 %2 %3 %4 (comp :job/title :contract/job)))

(reg-sub
  :contract/detail
  :<- [:contract/route-contract-id]
  :<- [:app/contracts]
  :<- [:app/jobs]
  :<- [:app/users]
  :<- [:db/active-user-id]
  (fn [[contract-id contracts jobs users active-user-id]]
    (-> contract-id
      (contract-id->contract contracts jobs users)
      (remove-unallowed-contract-data active-user-id))))

(reg-sub
  :list/job-proposals
  :<- [:db]
  :<- [:app/contracts]
  :<- [:app/jobs]
  :<- [:app/users]
  (fn [[db contracts jobs users]]
    (contract-ids->contracts-list (:list/job-proposals db) contracts jobs users)))

(reg-sub
  :list/job-feedbacks
  :<- [:db]
  :<- [:app/contracts]
  :<- [:app/jobs]
  :<- [:app/users]
  (fn [[db contracts jobs users]]
    (contract-ids->contracts-list (:list/job-feedbacks db) contracts jobs users)))

(reg-sub
  :list/freelancer-my-open-contracts
  :<- [:db]
  :<- [:app/contracts]
  :<- [:app/jobs]
  :<- [:app/users]
  (fn [[db contracts jobs users]]
    (contract-ids->contracts-list-job (:list/freelancer-my-open-contracts db) contracts jobs users)))

(reg-sub
  :list/freelancer-invitations
  :<- [:db]
  :<- [:app/contracts]
  :<- [:app/jobs]
  :<- [:app/users]
  (fn [[db contracts jobs users]]
    (contract-ids->contracts-list-job (:list/freelancer-invitations db) contracts jobs users)))

(reg-sub
  :list/freelancer-proposals
  :<- [:db]
  :<- [:app/contracts]
  :<- [:app/jobs]
  :<- [:app/users]
  (fn [[db contracts jobs users]]
    (contract-ids->contracts-list-job (:list/freelancer-proposals db) contracts jobs users)))

(reg-sub
  :list/freelancer-contracts-open
  :<- [:db]
  :<- [:app/contracts]
  :<- [:app/jobs]
  :<- [:app/users]
  (fn [[db contracts jobs users]]
    (contract-ids->contracts-list-job (:list/freelancer-contracts-open db) contracts jobs users)))

(reg-sub
  :list/freelancer-contracts-done
  :<- [:db]
  :<- [:app/contracts]
  :<- [:app/jobs]
  :<- [:app/users]
  (fn [[db contracts jobs users]]
    (contract-ids->contracts-list-job (:list/freelancer-contracts-done db) contracts jobs users)))

(reg-sub
  :list/user-feedbacks
  :<- [:db]
  :<- [:app/contracts]
  :<- [:app/jobs]
  :<- [:app/users]
  (fn [[db contracts jobs users]]
    (contract-ids->contracts-list (:list/user-feedbacks db) contracts jobs users)))

(defn- remove-unallowed-invoice-data [invoice active-user-id]
  (if-not (or (= (get-in invoice [:invoice/contract :contract/freelancer :user/id]) active-user-id)
              (= (get-in invoice [:invoice/contract :contract/job :job/employer :user/id]) active-user-id))
    (merge invoice {:invoice/description ""})
    invoice))

(defn invoice-id->invoice [invoice-id invoices contracts jobs users]
  (-> (get invoices invoice-id)
    (update :invoice/contract contracts)
    (update-in [:invoice/contract :contract/freelancer] users)
    (update-in [:invoice/contract :contract/job] jobs)
    (update-in [:invoice/contract :contract/job :job/employer] users)))

(defn invoice-ids->invoices-list [invoices-list invoices contracts jobs users]
  (-> invoices-list
    (update :items (partial u/sort-paginate-ids invoices-list))
    (update :items (partial map #(invoice-id->invoice % invoices contracts jobs users)))
    (u/list-filter-loaded (comp :user/name :contract/freelancer :invoice/contract))))

(reg-sub
  :list/job-invoices
  :<- [:db]
  :<- [:app/invoices]
  :<- [:app/contracts]
  :<- [:app/jobs]
  :<- [:app/users]
  (fn [[db invoices contracts jobs users]]
    (invoice-ids->invoices-list (:list/job-invoices db) invoices contracts jobs users)))

(reg-sub
  :list/employer-invoices-pending
  :<- [:db]
  :<- [:app/invoices]
  :<- [:app/contracts]
  :<- [:app/jobs]
  :<- [:app/users]
  (fn [[db invoices contracts jobs users]]
    (invoice-ids->invoices-list (:list/employer-invoices-pending db) invoices contracts jobs users)))

(reg-sub
  :list/employer-invoices-paid
  :<- [:db]
  :<- [:app/invoices]
  :<- [:app/contracts]
  :<- [:app/jobs]
  :<- [:app/users]
  (fn [[db invoices contracts jobs users]]
    (invoice-ids->invoices-list (:list/employer-invoices-paid db) invoices contracts jobs users)))

(reg-sub
  :list/freelancer-invoices-pending
  :<- [:db]
  :<- [:app/invoices]
  :<- [:app/contracts]
  :<- [:app/jobs]
  :<- [:app/users]
  (fn [[db invoices contracts jobs users]]
    (invoice-ids->invoices-list (:list/freelancer-invoices-pending db) invoices contracts jobs users)))

(reg-sub
  :list/freelancer-invoices-paid
  :<- [:db]
  :<- [:app/invoices]
  :<- [:app/contracts]
  :<- [:app/jobs]
  :<- [:app/users]
  (fn [[db invoices contracts jobs users]]
    (invoice-ids->invoices-list (:list/freelancer-invoices-paid db) invoices contracts jobs users)))

(reg-sub
  :list/contract-invoices
  :<- [:db]
  :<- [:app/invoices]
  :<- [:app/contracts]
  :<- [:app/jobs]
  :<- [:app/users]
  (fn [[db invoices contracts jobs users]]
    (invoice-ids->invoices-list (:list/contract-invoices db) invoices contracts jobs users)))

(reg-sub
  :invoice/detail
  :<- [:invoice/route-invoice-id]
  :<- [:app/invoices]
  :<- [:app/contracts]
  :<- [:app/jobs]
  :<- [:app/users]
  :<- [:db/active-user-id]
  (fn [[invoice-id invoices contracts jobs users active-user-id]]
    (-> invoice-id
      (invoice-id->invoice invoices contracts jobs users)
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

(reg-sub
  :form.invoice/add-invoice
  (fn [db]
    (:form.invoice/add-invoice db)))

(reg-sub
  :form.contract/add-proposal
  (fn [db]
    (:form.contract/add-proposal db)))

(reg-sub
  :form.contract/add-contract
  (fn [db]
    (:form.contract/add-contract db)))

(reg-sub
  :form.contract/add-feedback
  (fn [db]
    (:form.contract/add-feedback db)))

(reg-sub
  :form.job/set-hiring-done
  (fn [db]
    (:form.job/set-hiring-done db)))

(reg-sub
  :form.job/add-job
  (fn [db]
    (:form.job/add-job db)))

