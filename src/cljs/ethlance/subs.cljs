(ns ethlance.subs
  (:require
    [cemerick.url :as url]
    [clojure.data :as data]
    [ethlance.constants :as constants]
    [ethlance.db :refer [default-db]]
    [ethlance.ethlance-db :as ethlance-db]
    [ethlance.utils :as u]
    [goog.string :as gstring]
    [goog.string.format]
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
  :db/selected-currency
  (fn [db _]
    (:selected-currency db)))

(reg-sub
  :db/conversion-rates
  (fn [db [_ timestamp]]
    (if timestamp
      (get-in db [:conversion-rates-historical timestamp] {})
      (:conversion-rates db))))

(reg-sub
  :db/conversion-rates-historical
  (fn [db _]
    (:conversion-rates-historical db)))

(reg-sub
  :db/conversion-rate-tampered?
  :<- [:db/conversion-rates-historical]
  (fn [conversion-rates-historical [_ timestamp conversion-rate currency]]
    (when conversion-rate
      (when-let [rate-historical (get-in conversion-rates-historical [timestamp currency])]
        [(< 0.02 (js/Math.abs (- 1 (/ rate-historical conversion-rate)))) rate-historical]))))

(reg-sub
  :db/skills-loaded?
  (fn [db _]
    (:skills-loaded? db)))

(reg-sub
  :selected-currency/converted-value
  :<- [:db/selected-currency]
  :<- [:db/conversion-rates]
  (fn [[selected-curency conversion-rates] [_ value {:keys [:value-currency] :as opts
                                                     :or {value-currency 0}}]]
    (let [value (u/big-num->num value)]
      (if (and (not= selected-curency 0)
               (not (conversion-rates selected-curency)))
        (u/with-currency-symbol "" selected-curency)
        (if (= value-currency selected-curency)
          (u/format-currency value value-currency opts)
          (if (or (conversion-rates value-currency)
                  (= 0 value-currency))
            (let [value (u/parse-float value)]
              (-> (if (and value (not (js/isNaN value))) value 0)
                (u/currency->ether value-currency conversion-rates)
                (u/ether->currency selected-curency conversion-rates)
                (u/format-currency selected-curency opts)))
            (u/with-currency-symbol "" selected-curency)))))))

(reg-sub
  :db/search-freelancers-filter-open?
  (fn [db _]
    (:search-freelancers-filter-open? db)))

(reg-sub
  :db/search-jobs-filter-open?
  (fn [db _]
    (:search-jobs-filter-open? db)))

(reg-sub
  :db/contracts-not-found?
  (fn [db _]
    (:contracts-not-found? db)))

(reg-sub
  :db/active-address
  (fn [db _]
    (:active-address db)))

(reg-sub
  :db/active-setters?
  (fn [db _]
    (:active-setters? db)))

(reg-sub
  :db/last-transaction-gas-used
  (fn [db _]
    (when-let [gas-used (:last-transaction-gas-used db)]
      (gstring/format "%.0f" gas-used))))

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
  :window/lg-width?
  (fn [db]
    (= (:window/width-size db) 3)))

(reg-sub
  :window/xs-width?
  (fn [db]
    (= (:window/width-size db) 0)))

(reg-sub
  :window/xs-sm-width?
  (fn [db]
    (<= (:window/width-size db) 1)))

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
  :eth/contracts
  (fn [db]
    (->> (:eth/contracts db)
      (medley/map-vals #(assoc % :github-page (gstring/format u/github-contracts-path (u/uncapitalize (:name %))))))))

(reg-sub
  :form/search-jobs
  (fn [db]
    (:form/search-jobs db)))

(reg-sub
  :blockchain/connection-error?
  (fn [db]
    (:blockchain/connection-error? db)))

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
  :form.invoice/add-invoice-localstorage
  (fn [db]
    (:form.invoice/add-invoice-localstorage db)))

(reg-sub
  :form.invoice/add-invoice-prefilled
  :<- [:list/contracts :list/freelancer-my-open-contracts]
  :<- [:form.invoice/add-invoice]
  :<- [:form.invoice/add-invoice-localstorage]
  :<- [:db/conversion-rates]
  (fn [[contracts-list form localstorage conversion-rates]]
    (let [contract-id (get-in form [:data :invoice/contract])
          contract (medley/find-first #(= (:contract/id %) contract-id) (:items contracts-list))
          {:keys [:contract/job]} contract
          {:keys [:job/reference-currency :job/payment-type]} job
          reference-currency (or reference-currency 0)
          conversion-rate (if (zero? reference-currency) 1 (conversion-rates reference-currency))
          needs-conversion? (pos? reference-currency)
          {:keys [:invoice/worked-hours :invoice/worked-minutes :invoice/rate]} (get localstorage contract-id)
          worked-hours (u/ensure-number worked-hours)
          worked-minutes (u/ensure-number worked-minutes)
          invoice-rate (or rate (if-not (= (:job/payment-type job) 3)
                                  (:proposal/rate contract)
                                  0))
          invoice-rate-parsed (u/ensure-number (u/parse-float (u/big-num->num invoice-rate)))
          total-amount (if (< 1 payment-type)
                         invoice-rate-parsed
                         (* (u/hours-decimal worked-hours worked-minutes) invoice-rate-parsed))
          invoice-amount (if (or (and (zero? reference-currency)
                                      (< 1 payment-type))
                                 (zero? contract-id))
                           (get-in form [:data :invoice/amount])
                           (u/currency->ether total-amount reference-currency conversion-rates))]
      (-> form
        (merge {:contract contract
                :needs-conversion? needs-conversion?
                :hourly-rate? (= 1 payment-type)
                :total-amount total-amount})
        (update :data merge {:invoice/conversion-rate conversion-rate
                             :invoice/rate invoice-rate
                             :invoice/amount invoice-amount
                             :invoice/worked-hours worked-hours
                             :invoice/worked-minutes worked-minutes})))))

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

