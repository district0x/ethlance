(ns ethlance.subs
  (:require
    [clojure.data :as data]
    [ethlance.db :refer [default-db]]
    [ethlance.ethlance-db :as ethlance-db]
    [ethlance.utils :as u]
    [medley.core :as medley]
    [re-frame.core :refer [reg-sub]]
    [cemerick.url :as url]
    [ethlance.constants :as constants]))

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
  :db/contracts-not-found?
  (fn [db _]
    (:contracts-not-found? db)))

(reg-sub
  :db/active-address
  (fn [db _]
    (:active-address db)))

(reg-sub
  :db/active-address-balance
  :<- [:blockchain/addresses]
  :<- [:db/active-address]
  (fn [[blockchain-addresses active-address]]
    (:address/balance (blockchain-addresses active-address))))

(reg-sub
  :location/form-query-string
  (fn [db [_ form-key]]
    (let [[changed-from-default] (data/diff (db form-key) (default-db form-key))]
      (when (seq changed-from-default)
        (str "?" (url/map->query (medley/map-keys constants/keyword->query changed-from-default)))))))

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
    (:user/id ((:blockchain/addresses db) (:active-address db)))))

(reg-sub
  :db/active-user
  :<- [:db/active-user-id]
  :<- [:app/users]
  (fn [[active-user-id users]]
    (get users active-user-id)))

(reg-sub
  :db/my-users-loading?
  (fn [db]
    (:loading? (:list/my-users db))))

(reg-sub
  :db/active-address-registered?
  (fn [db]
    (when-let [items (seq (get-in db [:list/my-users :items]))]
      (let [i (.indexOf (get-in db [:list/my-users :params :user/addresses]) (:active-address db))]
        (pos? (nth items i))))))

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
  :window/width-size
  (fn [db]
    (:window/width-size db)))

(reg-sub
  :app/invoices
  (fn [db]
    (:app/invoices db)))

(reg-sub
  :db/active-page
  (fn [db]
    (:active-page db)))

(reg-sub
  :blockchain/addresses
  (fn [db]
    (:blockchain/addresses db)))

(reg-sub
  :db/snackbar
  (fn [db]
    (:snackbar db)))

(reg-sub
  :form/search-jobs
  (fn [db]
    (:form/search-jobs db)))

(reg-sub
  :form/search-freelancers
  (fn [db]
    (:form/search-freelancers db)))

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
  :user/route-user-id
  :<- [:db/active-page]
  (fn [{:keys [route-params]}]
    (js/parseInt (:user/id route-params))))

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

(defn user-id->user [user-id users blockchain-addresses]
  (let [user (get users user-id)]
    (assoc user :user/balance (:address/balance (blockchain-addresses (:user/address user))))))

(defn job-id->job [job-id jobs users blockchain-addresses]
  (-> (get jobs job-id)
    (update :job/employer #(user-id->user % users blockchain-addresses))))

(reg-sub
  :user/detail
  :<- [:user/route-user-id]
  :<- [:app/users]
  :<- [:blockchain/addresses]
  (fn [[user-id users blockchain-addresses]]
    (user-id->user user-id users blockchain-addresses)))

(reg-sub
  :job/detail
  :<- [:job/route-job-id]
  :<- [:app/jobs]
  :<- [:app/users]
  :<- [:blockchain/addresses]
  (fn [[job-id jobs users blockchain-addresses]]
    (job-id->job job-id jobs users blockchain-addresses)))

(reg-sub
  :list/search-jobs
  :<- [:db]
  :<- [:app/jobs]
  :<- [:app/users]
  :<- [:blockchain/addresses]
  (fn [[db jobs users blockchain-addresses]]
    (let [jobs-list (:list/search-jobs db)]
      (-> jobs-list
        (update :items (partial map #(job-id->job % jobs users blockchain-addresses)))
        (u/list-filter-loaded :job/title)))))

(reg-sub
  :list/jobs
  :<- [:db]
  :<- [:app/jobs]
  :<- [:app/users]
  :<- [:blockchain/addresses]
  (fn [[db jobs users blockchain-addresses] [_ list-key]]
    (let [jobs-list (get db list-key)]
      (-> jobs-list
        (update :items (partial u/sort-paginate-ids jobs-list))
        (update :items (partial map #(job-id->job % jobs users blockchain-addresses)))
        (u/list-filter-loaded :job/title)))))

(reg-sub
  :db/active-freelancer-job-detail-contract
  :<- [:db/active-user-id]
  :<- [:job/route-job-id]
  :<- [:app/contracts]
  (fn [[user-id job-id contracts]]
    (medley/find-first #(and (= (:contract/job %) job-id)
                             (= (:contract/freelancer %) user-id))
                       (vals contracts))))

(reg-sub
  :list/ids
  (fn [db [_ list-key]]
    (let [{:keys [items sort-dir]} (get db list-key)]
      (u/sort-in-dir sort-dir items))))


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

(defn contract-id->contract [contract-id contracts jobs users blockchain-addresses]
  (-> (get contracts contract-id)
    (update :contract/job #(job-id->job % jobs users blockchain-addresses))
    (update :contract/freelancer #(user-id->user % users blockchain-addresses))))

(reg-sub
  :contract/detail
  :<- [:contract/route-contract-id]
  :<- [:app/contracts]
  :<- [:app/jobs]
  :<- [:app/users]
  :<- [:blockchain/addresses]
  :<- [:db/active-user-id]
  (fn [[contract-id contracts jobs users blockchain-addresses active-user-id]]
    (-> contract-id
      (contract-id->contract contracts jobs users blockchain-addresses)
      (remove-unallowed-contract-data active-user-id))))

(reg-sub
  :list/contracts
  :<- [:db]
  :<- [:app/contracts]
  :<- [:app/jobs]
  :<- [:app/users]
  :<- [:blockchain/addresses]
  (fn [[db contracts jobs users blockchain-addresses] [_ list-key {:keys [:loading-till-freelancer?]}]]
    (let [contracts-list (get db list-key)
          non-empty-pred (if loading-till-freelancer? (comp :user/name :contract/freelancer)
                                                      (comp :job/title :contract/job))]
      (-> contracts-list
        (update :items (partial u/sort-paginate-ids contracts-list))
        (update :items (partial map #(contract-id->contract % contracts jobs users blockchain-addresses)))
        (u/list-filter-loaded non-empty-pred)))))

(defn- remove-unallowed-invoice-data [invoice active-user-id]
  (if-not (or (= (get-in invoice [:invoice/contract :contract/freelancer :user/id]) active-user-id)
              (= (get-in invoice [:invoice/contract :contract/job :job/employer :user/id]) active-user-id))
    (merge invoice {:invoice/description ""})
    invoice))

(defn invoice-id->invoice [invoice-id invoices contracts jobs users blockchain-addresses]
  (-> (get invoices invoice-id)
    (update :invoice/contract #(contract-id->contract % contracts jobs users blockchain-addresses))))

(reg-sub
  :list/invoices
  :<- [:db]
  :<- [:app/invoices]
  :<- [:app/contracts]
  :<- [:app/jobs]
  :<- [:app/users]
  :<- [:blockchain/addresses]
  (fn [[db invoices contracts jobs users blockchain-addresses] [_ list-key]]
    (let [invoices-list (get db list-key)]
      (-> invoices-list
        (update :items (partial u/sort-paginate-ids invoices-list))
        (update :items (partial map #(invoice-id->invoice % invoices contracts jobs users blockchain-addresses)))
        (u/list-filter-loaded (comp :user/name :contract/freelancer :invoice/contract))))))

(reg-sub
  :invoice/detail
  :<- [:invoice/route-invoice-id]
  :<- [:app/invoices]
  :<- [:app/contracts]
  :<- [:app/jobs]
  :<- [:app/users]
  :<- [:blockchain/addresses]
  :<- [:db/active-user-id]
  (fn [[invoice-id invoices contracts jobs users blockchain-addresses active-user-id]]
    (-> invoice-id
      (invoice-id->invoice invoices contracts jobs users blockchain-addresses)
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
  :form.invoice/pay-invoice
  (fn [db]
    (:form.invoice/pay-invoice db)))

(reg-sub
  :form.invoice/cancel-invoice
  (fn [db]
    (:form.invoice/cancel-invoice db)))

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
  :form.contract/add-invitation
  (fn [db]
    (:form.contract/add-invitation db)))

(reg-sub
  :form.job/set-hiring-done
  (fn [db]
    (:form.job/set-hiring-done db)))

(reg-sub
  :form.job/add-job
  (fn [db]
    (:form.job/add-job db)))

(reg-sub
  :form.config/add-skills
  (fn [db]
    (:form.config/add-skills db)))

(reg-sub
  :form.user/register-employer
  (fn [db]
    (:form.user/register-employer db)))

(reg-sub
  :form.user/register-freelancer
  (fn [db]
    (:form.user/register-freelancer db)))

(reg-sub
  :form.user/set-user
  :<- [:db]
  :<- [:db/active-user]
  (fn [[db active-user]]
    (-> (:form.user/set-user db)
      (update :data (partial merge (select-keys active-user ethlance-db/set-user-args))))))

(reg-sub
  :form.user/set-freelancer
  :<- [:db]
  :<- [:db/active-user]
  (fn [[db active-user]]
    (-> (:form.user/set-freelancer db)
      (update :data (partial merge (if (:user/freelancer? active-user)
                                     (select-keys active-user ethlance-db/set-freelancer-args)
                                     (:data (:form.user/register-freelancer db))))))))

(reg-sub
  :form.user/set-employer
  :<- [:db]
  :<- [:db/active-user]
  (fn [[db active-user]]
    (-> (:form.user/set-employer db)
      (update :data (partial merge (if (:user/employer? active-user)
                                     (select-keys active-user ethlance-db/set-employer-args)
                                     (:data (:form.user/register-employer db))))))))

