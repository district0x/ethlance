(ns ethlance.events
  (:require
    [ajax.core :as ajax]
    [akiroz.re-frame.storage :as re-frame-storage]
    [camel-snake-kebab.core :as cs :include-macros true]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs-web3.personal :as web3-personal]
    [cljs.spec :as s]
    [day8.re-frame.async-flow-fx]
    [day8.re-frame.http-fx]
    [ethlance.db :refer [default-db]]
    [ethlance.ethlance-db :as ethlance-db :refer [get-entity get-entities]]
    [ethlance.utils :as u]
    [goog.string :as gstring]
    [goog.string.format]
    [madvas.re-frame.google-analytics-fx]
    [madvas.re-frame.web3-fx]
    [ethlance.generate-db]
    [re-frame.core :refer [reg-event-db reg-event-fx inject-cofx path trim-v after debug reg-fx console dispatch]]
    [medley.core :as medley]))

#_(if goog.DEBUG
    (require '[ethlance.generate-db]))

(re-frame-storage/reg-co-fx! :ethlance {:fx :localstorage :cofx :localstorage})

(def interceptors [;check-spec-interceptor
                   ;(when ^boolean goog.DEBUG debug)
                   trim-v])

(defn contract-xhrio [contract-name code-type on-success on-failure]
  {:method :get
   :uri (gstring/format "./contracts/build/%s.%s?_=%s" contract-name (name code-type) (.getTime (new js/Date)))
   :timeout 6000
   :response-format (if (= code-type :abi) (ajax/json-response-format) (ajax/text-response-format))
   :on-success on-success
   :on-failure on-failure})

(def max-gas 4700000)

(defn get-contract [db key]
  (get-in db [:eth/contracts key]))

(defn get-instance [db key]
  (get-in db [:eth/contracts key :instance]))

(defn storage-keys [& args]
  (apply web3-eth/contract-call (get-instance @re-frame.db/app-db :ethlance-db) :storage-keys args))

(defn get-args [values args-order]
  (into [] ((apply juxt args-order) values)))

(defn get-ethlance-db []
  (get-in @re-frame.db/app-db [:eth/contracts :ethlance-db :instance]))

(defn get-entity-fn [instance id fields on-success on-failure]
  (let [[fields records types] (ethlance-db/get-entity-args id fields)]
    [instance :get-entity records types [:entity-loaded fields on-success] on-failure]))

(defn get-entities-fn [instance ids fields on-success on-failure]
  (let [[fields records types] (ethlance-db/get-entities-args ids fields)]
    [instance :get-entity-list records types [:entities-loaded ids fields on-success] on-failure]))

(defn get-entities-fns
  ([instance ids fields on-success on-failure]
   (get-entities-fns (count ids) instance ids fields on-success on-failure))
  ([parts-count instance ids fields on-success on-failure]
   (for [part-ids (partition parts-count ids)]
     (get-entities-fn instance part-ids fields on-success on-failure))))

(defn get-entities-field-items-fn [instance id-counts field on-success on-failure]
  (let [[ids+sub-ids field records types] (ethlance-db/get-entities-field-items-args id-counts field)]
    [instance :get-entity-list records types [:entities-field-items-loaded ids+sub-ids field on-success] on-failure]))

(reg-event-fx
  :initialize
  (inject-cofx :localstorage)
  (fn [{:keys [localstorage]} [deploy-contracts?]]
    (let [{:keys [web3 provides-web3?]} default-db]
      ;(.clear js/console)
      (merge
        {:db (merge-with (partial merge-with merge) default-db localstorage)
         :async-flow {:first-dispatch [:load-contracts]
                      :rules [{:when :seen?
                               :events [:contracts-loaded :blockchain/my-addresses-loaded]
                               :dispatch-n [[:contract.views/load-my-user-ids]]
                               :halt? true}]}}
        (when provides-web3?
          {:web3-fx.blockchain/fns
           {:web3 web3
            :fns [[web3-eth/accounts :blockchain/my-addresses-loaded :blockchain/on-error]]}})))))

(reg-event-fx
  :load-contracts
  interceptors
  (fn [{:keys [db]}]
    {:http-xhrio
     (for [[key {:keys [name]}] (:eth/contracts db)]
       (for [code-type (if goog.DEBUG [:abi :bin] [:abi])]
         (contract-xhrio name code-type [:contract/loaded key code-type] [:log-error])))}))

(reg-event-fx
  :deploy-contracts
  interceptors
  (fn [{:keys [db]}]
    (let [ethance-db (get-in db [:eth/contracts :ethlance-db])]
      {:web3-fx.blockchain/fns
       {:web3 (:web3 db)
        :fns [[web3-eth/contract-new
               (:abi ethance-db)
               {:gas max-gas
                :data (:bin ethance-db)
                :from (:active-address db)}
               :contract/deployed-ethlance-db
               :log-error]]}})))

(reg-event-fx
  :contract/deployed-ethlance-db
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [db localstorage]} [instance]]
    (when-let [db-address (aget instance "address")]
      (console :log :ethlance-db " deployed at " db-address)
      {:db (update-in db [:eth/contracts :ethlance-db] merge {:address db-address :instance instance})
       :localstorage (assoc-in localstorage [:eth/contracts :ethlance-db] {:address db-address})
       :web3-fx.blockchain/fns
       {:web3 (:web3 db)
        :fns (for [[key {:keys [abi bin]}] (dissoc (:eth/contracts db) :ethlance-db)]
               [web3-eth/contract-new
                abi
                db-address
                {:gas max-gas
                 :data bin
                 :from (:active-address db)}
                [:contract/deployed key]
                [:log-error key]])}})))

(reg-event-fx
  :contract/deployed
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [db localstorage]} [key instance]]
    (when-let [contract-address (aget instance "address")]
      (console :log key " deployed at " contract-address)
      (merge
        {:db (update-in db [:eth/contracts key] merge {:address contract-address
                                                       :instance instance})
         :localstorage (assoc-in localstorage [:eth/contracts key] {:address contract-address})}
        (when (:setter? (get-contract db key))
          {:dispatch [:contract.db/add-allowed-contracts [key]]})))))

(reg-event-fx
  :estimate-contracts
  interceptors
  (fn [{:keys [db]}]
    {:web3-fx.blockchain/fns
     {:web3 (:web3 db)
      :fns (for [[key {:keys [abi bin]}] (:eth/contracts db)]
             [web3-eth/estimate-gas
              {:data bin}
              [:contract/estimate-gas-result key]
              [:log-error key]])}}))

(reg-event-fx
  :contract/estimate-gas-result
  interceptors
  (fn [{:keys [db]} [key result]]
    (console :log key result)
    {}))

(reg-event-fx
  :set-active-page
  interceptors
  (fn [{:keys [db]} [match]]
    {:db (assoc db :active-page match)}))

(reg-event-fx
  :set-active-address
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [db localstorage]} [address]]
    {:db (-> db
           (assoc :active-address address))
     :localstorage (assoc localstorage :active-address address)}))

(reg-event-fx
  :blockchain/my-addresses-loaded
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [db localstorage]} [addresses]]
    (let [addresses-map (reduce #(assoc %1 %2 {:address %2}) {} addresses)
          active-address (if (contains? (set addresses) (:active-address localstorage))
                           (:active-address localstorage)
                           (first addresses))]
      (merge
        {:db (-> db
               (assoc :my-addresses addresses)
               (assoc :active-address active-address)
               (update :accounts merge addresses-map))
         :web3-fx.blockchain/balances
         {:web3 (:web3 db)
          :watch? true
          :blockchain-filter-opts "latest"
          :db-path [:blockchain :balances]
          :addresses addresses
          :dispatches [:blockchain/balance-loaded :blockchain/on-error]}}))))

(reg-event-db
  :blockchain/balance-loaded
  interceptors
  (fn [db [balance address]]
    (assoc-in db [:accounts address :balance] balance)))

(reg-event-fx
  :contract/loaded
  interceptors
  (fn [{:keys [db]} [contract-key code-type code]]
    (let [code (if (= code-type :abi) (clj->js code) code)]
      (let [new-db (cond-> db
                     true
                     (assoc-in [:eth/contracts contract-key code-type] code)

                     (= code-type :abi)
                     (assoc-in [:eth/contracts contract-key :instance]
                               (when-let [address (:address (get-contract db contract-key))]
                                 (web3-eth/contract-at (:web3 db) code address))))]
        (merge
          {:db new-db}
          (when (every? #(and (:abi %) (:bin %)) (vals (:eth/contracts new-db)))
            {:dispatch [:contracts-loaded]}))))))

(reg-event-fx
  :contracts-loaded
  interceptors
  (fn [{:keys [db]}]
    {:dispatch [:contract.views/get-skill-names]}))

(reg-event-fx
  :contract.config/set-configs
  interceptors
  (fn [{:keys [db]}]
    (let [{:keys [eth/config web3 active-address]} db]
      {:web3-fx.contract/state-fns
       {:web3 web3
        :db-path [:contract/state-fns]
        :fns [[(get-instance db :ethlance-config)
               :set-configs
               (keys config)
               (vals config)
               {:gas max-gas
                :from active-address}
               :contract/transaction-sent
               :contract/transaction-error
               [:contract/transaction-receipt :set-configs max-gas :generate-db false]]]}})))

(reg-event-fx
  :contract.config/add-skills
  interceptors
  (fn [{:keys [db]} [values address]]
    (let [{:keys [web3 active-address eth/contracts]} db]
      {:web3-fx.contract/state-fns
       {:web3 web3
        :db-path [:contract/state-fns]
        :fns [(concat
                [(get-instance db :ethlance-config)
                 :add-skills]
                (get-args values ethlance-db/add-skills-args)
                [{:gas max-gas
                  :from (or address active-address)}
                 :contract/transaction-sent
                 :contract/transaction-error
                 [:contract/transaction-receipt :add-skills max-gas false :log-error]])]}})))

(reg-event-fx
  :contract.db/add-allowed-contracts
  interceptors
  (fn [{:keys [db]} [contract-keys]]
    (let [contract-keys (if-not contract-keys (keys (medley/filter-vals :setter? (:eth/contracts db)))
                                              contract-keys)]
      (let [{:keys [web3 active-address eth/contracts]} db]
        {:web3-fx.contract/state-fns
         {:web3 web3
          :db-path [:contract/state-fns]
          :fns [[(get-instance db :ethlance-db)
                 :add-allowed-contracts
                 (map :address (vals (select-keys contracts contract-keys)))
                 {:gas max-gas
                  :from active-address}
                 :contract/transaction-sent
                 :contract/transaction-error
                 (if (contains? (set contract-keys) :ethlance-config)
                   :contract.config/set-configs
                   :do-nothing)
                 ;[:contract/transaction-receipt max-gas false false]
                 ]]}}))))

(reg-event-fx
  :contract.user/register-freelancer
  interceptors
  (fn [{:keys [db]} [values address]]
    (let [{:keys [web3 active-address eth/contracts]} db]
      {:web3-fx.contract/state-fns
       {:web3 web3
        :db-path [:contract/state-fns]
        :fns [(concat
                [(get-instance db :ethlance-user)
                 :register-freelancer]
                (remove nil? (get-args values ethlance-db/register-freelancer-args))
                [{:gas max-gas
                  :from (or address active-address)}
                 :contract/transaction-sent
                 :contract/transaction-error
                 [:contract/transaction-receipt :register-freelancer max-gas false :log-error]])]}})))

(reg-event-fx
  :contract.user/register-employer
  interceptors
  (fn [{:keys [db]} [values address]]
    (let [{:keys [web3 active-address eth/contracts]} db
          args (remove nil? (get-args values ethlance-db/register-employer-args))
          method (if (= (count args) (count ethlance-db/register-employer-args))
                   :register-employer
                   :set-employer)]
      {:web3-fx.contract/state-fns
       {:web3 web3
        :db-path [:contract/state-fns]
        :fns [(concat
                [(get-instance db :ethlance-user)
                 method]
                (remove nil? args)
                [{:gas max-gas
                  :from (or address active-address)}
                 :contract/transaction-sent
                 :contract/transaction-error
                 [:contract/transaction-receipt method max-gas false :log-error]])]}})))

(reg-event-fx
  :contract.search/search-freelancers
  interceptors
  (fn [{:keys [db]} [values]]
    (let [{:keys [web3 active-address eth/contracts]} db]
      {:web3-fx.contract/constant-fns
       {:fns [(concat
                [(get-instance db :ethlance-search)
                 :search-freelancers]
                (get-args values ethlance-db/search-freelancers-args)
                [:log :log-error])]}})))

(reg-event-fx
  :contract.search/search-jobs-initiate
  interceptors
  (fn [{:keys [db]} [values]]
    {:async-flow {:first-dispatch [:do-nothing]
                  :rules [{:when :seen?
                           :events [:contracts-loaded]
                           :dispatch [:contract.search/search-jobs values]
                           :halt? true}]}}))

(reg-event-fx
  :contract.search/search-jobs
  interceptors
  (fn [{:keys [db]} [values]]
    (let [{:keys [web3]} db]
      {:db (assoc-in db [:list/search-job :loading?] true)
       :web3-fx.contract/constant-fns
       {:fns [(concat
                [(get-instance db :ethlance-search)
                 :search-jobs]
                (conj (get-args values ethlance-db/search-jobs-args)
                      (get-args values ethlance-db/search-jobs-nested-args))
                [:contract.search/search-jobs-loaded
                 :log-error])]}})))

(reg-event-fx
  :contract.search/search-jobs-loaded
  interceptors
  (fn [{:keys [db]} [job-ids]]
    (let [job-ids (u/big-nums->nums job-ids)
          jobs-to-load (remove #(get-in db [:app/jobs %]) job-ids)]
      {:db (assoc db :list/search-job {:items job-ids
                                       :loading? (boolean (seq jobs-to-load))})
       :web3-fx.contract/constant-fns
       {:fns (get-entities-fns 1
                               (get-instance db :ethlance-db)
                               jobs-to-load
                               (keys ethlance-db/job-schema)
                               :contract/jobs-loaded
                               :log-error)}})))

(reg-event-fx
  :contract/jobs-loaded
  interceptors
  (fn [{:keys [db]} [jobs]]
    (let [jobs (->> jobs
                 (remove u/empty-job?)
                 (u/assoc-key-as-value :job/id))]
      {:db (-> db
             (update :app/jobs merge jobs))
       :web3-fx.contract/constant-fns
       {:fns [(get-entities-field-items-fn
                (get-instance db :ethlance-db)
                (medley/map-vals :job/skills-count jobs)
                :job/skills
                :contract/jobs-skills-loaded
                :log-error)
              (get-entities-fn
                (get-instance db :ethlance-db)
                (set (vals (medley/map-vals :job/employer jobs)))
                (keys (dissoc ethlance-db/employer-schema :employer/description))
                :contract/jobs-employers-loaded
                :log-error)]}})))

(reg-event-fx
  :contract/jobs-skills-loaded
  interceptors
  (fn [{:keys [db]} [jobs-skills]]
    {:db (-> db
           (update :app/jobs (partial merge-with merge) jobs-skills)
           (assoc-in [:list/search-job :loading?] false))}))

(reg-event-fx
  :contract/jobs-employers-loaded
  interceptors
  (fn [{:keys [db]} [employers]]
    {:db (-> db
           (update :app/users merge employers)
           (assoc-in [:list/search-job :loading?] false))}))

(reg-event-fx
  :contract.job/add
  interceptors
  (fn [{:keys [db]} [values address]]
    (let [{:keys [web3 active-address eth/contracts]} db
          args (get-args values ethlance-db/add-job-args)
          nested-args (get-args values ethlance-db/add-job-nested-args)]
      {:web3-fx.contract/state-fns
       {:web3 web3
        :db-path [:contract/state-fns]
        :fns [(concat
                [(get-instance db :ethlance-job)
                 :add-job]
                (conj args nested-args)
                [{:gas max-gas
                  :from (or address active-address)}
                 :contract/transaction-sent
                 :contract/transaction-error
                 [:contract/transaction-receipt :add-job max-gas false :log-error]])]}})))

(reg-event-fx
  :contract.job/add-invitation
  interceptors
  (fn [{:keys [db]} [values address]]
    (let [{:keys [web3 active-address eth/contracts]} db]
      {:web3-fx.contract/state-fns
       {:web3 web3
        :db-path [:contract/state-fns]
        :fns [(concat
                [(get-instance db :ethlance-job)
                 :add-job-invitation]
                (get-args values ethlance-db/add-invitation-args)
                [{:gas max-gas
                  :from (or address active-address)}
                 :contract/transaction-sent
                 :contract/transaction-error
                 [:contract/transaction-receipt :add-job-invitation max-gas false :log-error]])]}})))

(reg-event-fx
  :contract.job/add-proposal
  interceptors
  (fn [{:keys [db]} [values address]]
    (let [{:keys [web3 active-address eth/contracts]} db]
      {:web3-fx.contract/state-fns
       {:web3 web3
        :db-path [:contract/state-fns]
        :fns [(concat
                [(get-instance db :ethlance-job)
                 :add-job-proposal]
                (get-args values ethlance-db/add-proposal-args)
                [{:gas max-gas
                  :from (or address active-address)}
                 :contract/transaction-sent
                 :contract/transaction-error
                 [:contract/transaction-receipt :add-job-proposal max-gas false :log-error]])]}})))

(reg-event-fx
  :contract.contract/add
  interceptors
  (fn [{:keys [db]} [values address]]
    (let [{:keys [web3 active-address eth/contracts]} db]
      {:web3-fx.contract/state-fns
       {:web3 web3
        :db-path [:contract/state-fns]
        :fns [(concat
                [(get-instance db :ethlance-contract)
                 :add-job-contract]
                (get-args values ethlance-db/add-contract-args)
                [{:gas max-gas
                  :from (or address active-address)}
                 :contract/transaction-sent
                 :contract/transaction-error
                 [:contract/transaction-receipt :add-job-contract max-gas false :log-error]])]}})))

(reg-event-fx
  :contract.contract/add-feedback
  interceptors
  (fn [{:keys [db]} [values address]]
    (let [{:keys [web3 active-address eth/contracts]} db]
      {:web3-fx.contract/state-fns
       {:web3 web3
        :db-path [:contract/state-fns]
        :fns [(concat
                [(get-instance db :ethlance-contract)
                 :add-job-contract-feedback]
                (get-args values ethlance-db/add-contract-feedback-args)
                [{:gas max-gas
                  :from (or address active-address)}
                 :contract/transaction-sent
                 :contract/transaction-error
                 [:contract/transaction-receipt :add-job-contract-feedback max-gas false :log-error]])]}})))

(reg-event-fx
  :contract.invoice/add
  interceptors
  (fn [{:keys [db]} [values address]]
    (let [{:keys [web3 active-address eth/contracts]} db]
      {:web3-fx.contract/state-fns
       {:web3 web3
        :db-path [:contract/state-fns]
        :fns [(concat
                [(get-instance db :ethlance-invoice)
                 :add-invoice]
                (get-args values ethlance-db/add-invoice-args)
                [{:gas max-gas
                  :from (or address active-address)}
                 :contract/transaction-sent
                 :contract/transaction-error
                 [:contract/transaction-receipt :add-invoice max-gas false :log-error]])]}})))

(reg-event-fx
  :contract.invoice/pay
  interceptors
  (fn [{:keys [db]} [values amount address]]
    (let [{:keys [web3 active-address eth/contracts]} db]
      {:web3-fx.contract/state-fns
       {:web3 web3
        :db-path [:contract/state-fns]
        :fns [(concat
                [(get-instance db :ethlance-invoice)
                 :pay-invoice]
                (get-args values ethlance-db/pay-invoice-args)
                [{:gas max-gas
                  :value amount
                  :from (or address active-address)}
                 :contract/transaction-sent
                 :contract/transaction-error
                 [:contract/transaction-receipt :pay-invoice max-gas false :log-error]])]}})))

(reg-event-fx
  :contract.invoice/cancel
  interceptors
  (fn [{:keys [db]} [values address]]
    (let [{:keys [web3 active-address eth/contracts]} db]
      {:web3-fx.contract/state-fns
       {:web3 web3
        :db-path [:contract/state-fns]
        :fns [(concat
                [(get-instance db :ethlance-invoice)
                 :cancel-invoice]
                (get-args values ethlance-db/cancel-invoice-args)
                [{:gas max-gas
                  :from (or address active-address)}
                 :contract/transaction-sent
                 :contract/transaction-error
                 [:contract/transaction-receipt :cancel-invoice max-gas false :log-error]])]}})))

(reg-event-fx
  :contract.views/get-freelancer-job-actions
  interceptors
  (fn [{:keys [db]} [values]]
    {:web3-fx.contract/constant-fns
     {:fns [(concat
              [(get-instance db :ethlance-views)
               :get-freelancer-job-actions]
              (get-args values ethlance-db/get-freelancer-job-actions-args)
              [:log :log-error])]}}))

(reg-event-fx
  :contract.views/get-freelancer-invoices
  interceptors
  (fn [{:keys [db]} [values]]
    {:web3-fx.contract/constant-fns
     {:fns [(concat
              [(get-instance db :ethlance-views)
               :get-freelancer-invoices]
              (get-args values ethlance-db/get-freelancer-invoices-args)
              [:log :log-error])]}}))

(reg-event-fx
  :contract.views/get-freelancer-contracts
  interceptors
  (fn [{:keys [db]} [values]]
    {:web3-fx.contract/constant-fns
     {:fns [(concat
              [(get-instance db :ethlance-views)
               :get-freelancer-contracts]
              (get-args values ethlance-db/get-freelancer-contracts-args)
              [:log :log-error])]}}))

(reg-event-fx
  :contract.views/get-job-contracts
  interceptors
  (fn [{:keys [db]} [values]]
    {:web3-fx.contract/constant-fns
     {:fns [(concat
              [(get-instance db :ethlance-views)
               :get-job-contracts]
              (get-args values ethlance-db/get-job-contracts-args)
              [:log :log-error])]}}))

(reg-event-fx
  :contract.views/get-job-proposals
  interceptors
  (fn [{:keys [db]} [values]]
    {:web3-fx.contract/constant-fns
     {:fns [(concat
              [(get-instance db :ethlance-views)
               :get-job-proposals]
              (get-args values ethlance-db/get-job-proposals-args)
              [:log :log-error])]}}))

(reg-event-fx
  :contract.views/get-job-invoices
  interceptors
  (fn [{:keys [db]} [values]]
    {:web3-fx.contract/constant-fns
     {:fns [(concat
              [(get-instance db :ethlance-views)
               :get-job-invoices]
              (get-args values ethlance-db/get-job-invoices-args)
              [:log :log-error])]}}))

(reg-event-fx
  :contract.views/get-employer-jobs
  interceptors
  (fn [{:keys [db]} [values]]
    {:web3-fx.contract/constant-fns
     {:fns [(concat
              [(get-instance db :ethlance-views)
               :get-employer-jobs]
              (get-args values ethlance-db/get-employer-jobs-args)
              [:log :log-error])]}}))

(reg-event-fx
  :contract.views/get-skill-names
  interceptors
  (fn [{:keys [db]} [values]]
    {:web3-fx.contract/constant-fns
     {:fns [[(get-instance db :ethlance-views)
             :get-skill-names
             :contract.views/skill-names-loaded
             :log-error]]}}))

(reg-event-db
  :contract.views/skill-names-loaded
  interceptors
  (fn [db [[ids names]]]
    (update db :app/skills merge (zipmap (u/big-nums->nums ids)
                                         (map (comp (partial hash-map :skill/name) web3/to-ascii) names)))))

(reg-event-fx
  :contract.views/load-my-user-ids
  interceptors
  (fn [{:keys [db]}]
    (let [addresses (:my-addresses db)]
      {:web3-fx.contract/constant-fns
       {:fns [[(get-instance db :ethlance-views)
               :get-users
               addresses
               [:contract.views/my-user-ids-loaded addresses]
               :log-error]]}})))

(reg-event-fx
  :contract.views/my-user-ids-loaded
  interceptors
  (fn [{:keys [db]} [addresses user-ids]]
    (let [user-ids (u/big-nums->nums user-ids)
          address->user-id (medley/remove-vals zero? (zipmap addresses user-ids))
          instance (get-instance db :ethlance-db)
          user-ids (vals address->user-id)]
      {:db (update db :address->user-id merge address->user-id)
       :web3-fx.contract/constant-fns
       {:fns (get-entities-fns instance
                               user-ids
                               (keys ethlance-db/user-schema)
                               :contract/users-loaded
                               :log-error)}})))

(reg-event-fx
  :contract/users-loaded
  interceptors
  (fn [{:keys [db]} [users]]
    {:db
     (let [users (->> users
                   (medley/remove-vals u/empty-user?)
                   (u/assoc-key-as-value :user/id))
           address->user-id (into {} (map (fn [[id user]]
                                            {(:user/address user) id})
                                          users))]
       (-> db
         (update :app/users merge users)
         (update :address->user-id merge address->user-id)))}))

(reg-event-fx
  :contract/call
  interceptors
  (fn [{:keys [db]} [contract-key & args]]
    {:web3-fx.contract/constant-fns
     {:fns [(concat [(get-instance db contract-key)] args [:log :log-error])]}}))

(reg-event-fx
  :contract/state-call
  interceptors
  (fn [{:keys [db]} [contract-key method & args]]
    {:web3-fx.contract/state-fns
     {:web3 (:web3 db)
      :db-path [:contract/state-fns]
      :fns [(concat [(get-instance db contract-key)
                     method]
                    args
                    [{:gas max-gas
                      :from (:active-address db)}
                     :contract/transaction-sent
                     :contract/transaction-error
                     [:contract/transaction-receipt method max-gas nil nil]])]}}))

(reg-event-fx
  :form/search-job-changed
  interceptors
  (fn [{:keys [db]} [key value]]
    (let [new-db (assoc-in db [:form/search-job key] value)]
      {:db new-db
       :dispatch [:contract.search/search-jobs (:form/search-job new-db)]})))

(reg-event-fx
  :entity-loaded
  interceptors
  (fn [_ [fields on-success result]]
    {:dispatch (conj (u/ensure-vec on-success) (ethlance-db/parse-entity fields result))}))

(reg-event-fx
  :entities-loaded
  interceptors
  (fn [_ [ids fields on-success result]]
    {:dispatch (conj (u/ensure-vec on-success) (ethlance-db/parse-entities ids fields result))}))

(reg-event-fx
  :entities-field-items-loaded
  interceptors
  (fn [_ [ids+sub-ids field on-success result]]
    {:dispatch (conj (u/ensure-vec on-success) (ethlance-db/parse-entities-field-items ids+sub-ids field result))}))

(reg-event-fx
  :contract/transaction-sent
  interceptors
  (fn [_ [tx-hash]]
    #_(console :log tx-hash)))

(reg-event-fx
  :contract/transaction-error
  interceptors
  (fn [_ [error]]
    (console :error error)))

(reg-event-fx
  :contract/transaction-receipt
  interceptors
  (fn [_ [event-name gas-limit on-success on-out-of-gas {:keys [gas-used] :as receipt}]]
    (let [gas-used-percent (* (/ gas-used gas-limit) 100)]
      (console :log event-name (gstring/format "%.2f%" gas-used-percent) "gas used:" gas-used)
      (if (<= gas-limit gas-used)
        (when on-out-of-gas
          {:dispatch [on-out-of-gas receipt]})
        (when on-success
          {:dispatch [on-success receipt]})))))

(reg-event-fx
  :clean-localstorage
  interceptors
  (fn [_]
    {:localstorage nil}))

(reg-event-fx
  :reintialize
  interceptors
  (fn [_ [initialize?]]
    {:async-flow {:first-dispatch [:load-contracts]
                  :rules [{:when :seen?
                           :events [:contracts-loaded]
                           :dispatch-n [[:deploy-contracts]]
                           :halt? true}]}}))



(reg-event-db
  :print-db
  interceptors
  (fn [db]
    (print.foo/look db)
    db))

(reg-event-fx
  :print-localstorage
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [db localstorage]}]
    (print.foo/look localstorage)
    nil))

(reg-event-fx
  :log
  interceptors
  (fn [db result]
    (apply console :log (map #(if (aget % "toNumber") (.toNumber %) %) result))))

(reg-event-fx
  :do-nothing
  interceptors
  (fn [db result]
    ))

(reg-event-db
  :log-error
  interceptors
  (fn [db errors]
    (apply console :error errors)
    db))




(comment
  (dispatch [:initialize])
  (dispatch [:print-db])
  (dispatch [:deploy-contracts])
  (dispatch [:estimate-contracts])
  (dispatch [:clean-localstorage true])
  (dispatch [:print-localstorage])
  (dispatch [:contract.config/set-configs])
  (dispatch [:contract.db/add-allowed-contracts])
  (dispatch [:contract/call :ethlance-db :get-allowed-contracts])
  (dispatch [:contract/call :ethlance-db :allowed-contracts-keys 5])
  (dispatch [:contract/call :ethlance-user :get-config "max-user-languages"])
  (dispatch [:contract/call :ethlance-db :get-u-int-value (u/sha3 :user/count)])
  (dispatch [:contract/call :ethlance-db :get-u-int-value (u/sha3 :job/count)])
  (dispatch [:contract/call :ethlance-db :get-u-int-value (u/sha3 :category/jobs-count 1)])
  (dispatch [:contract/call :ethlance-db :get-u-int-value (u/sha3 :skill/jobs-count 5)])
  (dispatch [:contract/call :ethlance-db :get-address-value (u/sha3 :user/address 1)])
  (dispatch [:contract/call :ethlance-db :get-bytes32-value (u/sha3 :user/name 1)])
  (dispatch [:contract/call :ethlance-db :get-u-int-value (u/sha3 :freelancer/hourly-rate 1)])
  (dispatch [:contract/call :ethlance-db :get-string-value (u/sha3 :freelancer/description 1)])
  (dispatch [:contract/call :ethlance-db :get-u-int-value (u/sha3 :job-action/freelancer-job 1 1)])
  (dispatch [:contract/call :ethlance-db :get-u-int-value (storage-keys 6)])
  (get-entity 3 (keys ethlance-db/account-schema) (get-ethlance-db))

  (get-entity 1 (keys ethlance-db/account-schema) (get-ethlance-db))

  (get-entity 1 (keys ethlance-db/job-action-schema) (get-ethlance-db))
  (get-entity 1 (keys ethlance-db/job-schema) (get-ethlance-db))

  (get-entity 1 (keys ethlance-db/contract-schema) (get-ethlance-db))

  (get-entity 1 (keys ethlance-db/invoice-schema) (get-ethlance-db))

  (get-entity 1 [:freelancer/job-title :user/name :user/gravatar] (get-ethlance-db))

  (get-entities (range 1 10) (keys (dissoc ethlance-db/job-schema :job/description)) (get-ethlance-db))

  (get-entities [2 3 1 1 1 1] [:skill/name] (get-ethlance-db))

  (dispatch [:contract.user/register-freelancer {:user/name "Mataaa"
                                                 :user/gravatar "abc"
                                                 :user/country 1
                                                 :user/languages [1]
                                                 :freelancer/available? true
                                                 :freelancer/job-title "Cljs dev"
                                                 :freelancer/hourly-rate 10
                                                 :freelancer/categories [1 2]
                                                 :freelancer/skills [3 4 5]
                                                 :freelancer/description "asdasdasd" #_(doall (reduce str (range 100)))}])

  (dispatch [:contract.user/register-employer {:user/name "Mataaa"
                                               :user/gravatar "abc"
                                               :user/country 1
                                               :user/languages [1]
                                               :employer/description "employdescribptions"}])

  (dispatch [:contract.search/search-freelancers {:search/category 1
                                                  :search/skills [3]
                                                  :search/min-avg-rating 0
                                                  :search/min-contracts-count 0
                                                  :search/min-hourly-rate 0
                                                  :search/max-hourly-rate 1
                                                  :search/country 0
                                                  :search/language 0
                                                  :search/offset 0
                                                  :search/limit 10}])

  (dispatch [:contract.job/add {:job/title "This is Job 1"
                                :job/description "Asdkaas  aspokd aps asopdk ap"
                                :job/skills [3 4 5]
                                :job/budget 10
                                :job/language 1
                                :job/category 1
                                :job/payment-type 1
                                :job/experience-level 1
                                :job/estimated-duration 1
                                :job/hours-per-week 1
                                :job/freelancers-needed 2}])

  (dispatch [:contract.search/search-jobs {:search/category 1
                                           :search/skills [2]
                                           :search/payment-types []
                                           :search/experience-levels []
                                           :search/estimated-durations []
                                           :search/hours-per-weeks []
                                           :search/min-budget 0
                                           :search/min-employer-avg-rating 0
                                           :search/min-employer-ratings-count 0
                                           :search/country 0
                                           :search/language 0
                                           :search/offset 0
                                           :search/limit 10}])
  (dispatch [:contract/state-call :ethlance-user :test-db 5])

  (dispatch [:contract.views/get-freelancer-job-actions {:user/id 1
                                                         :job-action/status 1
                                                         :job/status 1}])

  (dispatch [:contract.views/get-freelancer-invoices {:user/id 1 :invoice/status 1}])
  (dispatch [:contract.views/get-freelancer-contracts {:user/id 1 :contract/done? true}])
  (dispatch [:contract.views/get-job-contracts {:job/id 1}])
  (dispatch [:contract.views/get-job-proposals {:job/id 1}])
  (dispatch [:contract.views/get-job-invoices {:job/id 1 :invoice/status 3}])
  (dispatch [:contract.views/get-employer-jobs {:user/id 2 :job/status 2}])
  (dispatch [:contract.views/load-my-user-ids {:user/addresses (:my-addresses @re-frame.db/app-db)}])

  )