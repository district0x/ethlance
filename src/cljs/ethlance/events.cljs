(ns ethlance.events
  (:require
    [ajax.core :as ajax]
    [akiroz.re-frame.storage :as re-frame-storage]
    [camel-snake-kebab.core :as cs :include-macros true]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs-web3.personal :as web3-personal]
    [cljs.spec :as s]
    [clojure.set :as set]
    [day8.re-frame.async-flow-fx]
    [day8.re-frame.http-fx]
    [ethlance.db :refer [default-db]]
    [ethlance.ethlance-db :as ethlance-db :refer [get-entity get-entities get-entities-field-items]]
    [ethlance.utils :as u]
    [goog.string :as gstring]
    [goog.string.format]
    [madvas.re-frame.google-analytics-fx]
    [madvas.re-frame.web3-fx]
    [ethlance.generate-db]
    [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx inject-cofx path trim-v after debug reg-fx console dispatch]]
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
    (when (and (seq records) (seq fields))
      [instance :get-entity-list records types [:entities-loaded (set ids) fields on-success] on-failure])))

(defn entities-fns
  ([instance ids fields on-success on-failure]
   (entities-fns (count ids) instance ids fields on-success on-failure))
  ([parts-count instance ids fields on-success on-failure]
   (for [part-ids (partition parts-count ids)]
     (get-entities-fn instance part-ids fields on-success on-failure))))

(defn entities-field-items-fn [instance id-counts field on-success on-failure]
  (let [[ids+sub-ids field records types] (ethlance-db/get-entities-field-items-args id-counts field)]
    (when (seq records)
      [instance :get-entity-list records types [:entities-field-items-loaded ids+sub-ids field on-success] on-failure])))

(defn all-contracts-loaded? [db]
  (every? #(and (:abi %) (if goog.DEBUG true (:bin %))) (vals (:eth/contracts db))))


(defn init-list-load [db db-path params]
  (cond-> db
    (not= (get-in db [db-path :params]) params)
    (assoc-in [db-path :items] [])

    true (update db-path merge {:loading? true :params params})))

(defn find-needed-fields [required-fields items ids & [editable-fields]]
  (reduce (fn [acc id]
            (let [item (get items id)
                  needed-fields (set/difference (set required-fields)
                                                (set/difference (set (keys item))
                                                                (set editable-fields)))]
              (cond-> acc
                (seq needed-fields) (update :ids conj id)
                true (update :fields set/union needed-fields))))
          {:fields #{}
           :ids #{}} ids))

(def log-used-gas
  (re-frame/->interceptor
    :id :log-used-gas
    :before (fn [{:keys [coeffects] :as context}]
              (let [event (:event coeffects)
                    {:keys [gas-used] :as receipt} (last event)
                    gas-limit (first event)]
                (let [gas-used-percent (* (/ gas-used gas-limit) 100)]
                  (console :log
                           (gstring/format "%.2f%" gas-used-percent)
                           "gas used:" gas-used
                           (second event)))
                (-> context
                  (update-in [:coeffects :event (dec (count event))]
                             merge
                             {:success? (< gas-used gas-limit)})
                  (update-in [:coeffects :event] #(-> % rest vec)))))))

(defn get-active-user [{:keys [:active-address :address->user-id :app/users]}]
  (users (address->user-id active-address)))

(defn get-my-users [{:keys [:my-addresses :address->user-id :app/users]}]
  (map (comp users address->user-id) my-addresses))

(reg-event-fx
  :initialize
  (inject-cofx :localstorage)
  (fn [{:keys [localstorage]} [deploy-contracts?]]
    (let [{:keys [web3 provides-web3?]} default-db]
      ;(.clear js/console)
      (merge
        {:db (merge-with (partial merge-with merge) default-db localstorage)
         :async-flow {:first-dispatch [:load-eth-contracts]
                      :rules [{:when :seen?
                               :events [:eth-contracts-loaded :blockchain/my-addresses-loaded]
                               :dispatch-n [[:contract.views/load-my-user-ids]]
                               :halt? true}]}}
        (when provides-web3?
          {:web3-fx.blockchain/fns
           {:web3 web3
            :fns [[web3-eth/accounts :blockchain/my-addresses-loaded :blockchain/on-error]]}})))))

(reg-event-fx
  :load-eth-contracts
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
          (when (all-contracts-loaded? new-db)
            {:dispatch [:eth-contracts-loaded]}))))))

(reg-event-fx
  :eth-contracts-loaded
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
                 [:contract/transaction-error values address]
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
  :after-eth-contracts-loaded
  interceptors
  (fn [{:keys [db]} load-dispatch]
    (if-not (all-contracts-loaded? db)
      {:async-flow {:first-dispatch [:do-nothing]
                    :rules [{:when :seen?
                             :events [:eth-contracts-loaded]
                             :dispatch load-dispatch
                             :halt? true}]}}
      {:dispatch load-dispatch})))


(reg-event-fx
  :after-my-users-loaded
  interceptors
  (fn [{:keys [db]} load-dispatch]
    (if (and (every? (comp pos? :user/status) (get-my-users db))
             (all-contracts-loaded? db))
      {:dispatch load-dispatch}
      {:async-flow {:first-dispatch [:do-nothing]
                    :rules [{:when :seen?
                             :events [:contract/users-loaded]
                             :dispatch (into [:after-my-users-loaded] load-dispatch)
                             :halt? true}]}})))

;; ============search-jobs

(reg-event-fx
  :contract.search/search-jobs
  interceptors
  (fn [{:keys [db]} [values]]
    (let [{:keys [web3]} db]
      {:db (assoc-in db [:list/search-jobs :loading?] true)
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
    (let [job-ids (u/big-nums->nums job-ids)]
      {:db (assoc db :list/search-jobs {:items job-ids :loading? false})
       :dispatch [:contract.db/load-jobs ethlance-db/job-schema job-ids]})))

(reg-event-fx
  :contract.db/load-jobs
  interceptors
  (fn [{:keys [db]} [schema job-ids]]
    (let [{:keys [fields ids]} (find-needed-fields
                                 (keys schema)
                                 (:app/jobs db)
                                 job-ids
                                 ethlance-db/job-editable-fields)]
      {:web3-fx.contract/constant-fns
       {:fns (entities-fns 1
                           (get-instance db :ethlance-db)
                           ids
                           fields
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
             (update :app/jobs (partial merge-with merge) jobs))
       :dispatch-n [[:contract.db/load-job-skills jobs]
                    [:contract.db/load-users
                     (merge (ethlance-db/without-strings ethlance-db/employer-schema)
                            ethlance-db/user-schema)
                     (map :job/employer (vals jobs))]]})))

(reg-event-fx
  :contract.db/load-job-skills
  interceptors
  (fn [{:keys [db]} [jobs]]
    {:web3-fx.contract/constant-fns
     {:fns [(entities-field-items-fn
              (get-instance db :ethlance-db)
              (medley/map-vals :job/skills-count jobs)
              :job/skills
              :contract/jobs-skills-loaded
              :log-error)]}}))

(reg-event-fx
  :contract/jobs-skills-loaded
  interceptors
  (fn [{:keys [db]} [jobs-skills]]
    {:db (-> db
           (update :app/jobs (partial merge-with merge) jobs-skills))}))

;; ============search-freelancers

(reg-event-fx
  :contract.search/search-freelancers
  interceptors
  (fn [{:keys [db]} [values]]
    (let [{:keys [web3 active-address eth/contracts]} db]
      {:db (assoc-in db [:list/search-freelancers :loading?] true)
       :web3-fx.contract/constant-fns
       {:fns [(concat
                [(get-instance db :ethlance-search)
                 :search-freelancers]
                (get-args values ethlance-db/search-freelancers-args)
                [:contract.search/search-freelancers-loaded
                 :log-error])]}})))

(reg-event-fx
  :contract.search/search-freelancers-loaded
  interceptors
  (fn [{:keys [db]} [user-ids]]
    (let [user-ids (u/big-nums->nums user-ids)]
      {:db (assoc db :list/search-freelancers {:items user-ids :loading? false})
       :dispatch [:contract.db/load-users
                  (dissoc ethlance-db/freelancer-schema :freelancer/description)
                  user-ids]})))

(reg-event-fx
  :contract.db/load-users
  interceptors
  (fn [{:keys [db]} [schema user-ids]]
    (let [user-ids (u/big-nums->nums user-ids)
          {:keys [fields ids]} (find-needed-fields (keys schema)
                                                   (:app/users db)
                                                   user-ids
                                                   ethlance-db/user-editable-fields)]
      {:web3-fx.contract/constant-fns
       {:fns (entities-fns (get-instance db :ethlance-db)
                           ids
                           fields
                           :contract/users-loaded
                           :log-error)}})))

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

;; ============users

(reg-event-fx
  :contract.views/my-user-ids-loaded
  interceptors
  (fn [{:keys [db]} [addresses user-ids]]
    (let [user-ids (u/big-nums->nums user-ids)
          address->user-id (medley/remove-vals zero? (zipmap addresses user-ids))
          instance (get-instance db :ethlance-db)
          user-ids (vals address->user-id)]
      {:db (update db :address->user-id merge address->user-id)
       :dispatch [:contract.db/load-users ethlance-db/user-schema user-ids]})))

(reg-event-fx
  :contract/users-loaded
  interceptors
  (fn [{:keys [db]} [users]]
    (let [users (->> users
                  (medley/remove-vals u/empty-user?)
                  (u/assoc-key-as-value :user/id))
          address->user-id (into {} (map (fn [[id user]]
                                           {(:user/address user) id})
                                         users))]
      {:db (-> db
             (update :app/users (partial merge-with merge) users)
             (update :address->user-id merge address->user-id))
       :web3-fx.contract/constant-fns
       {:fns [(entities-field-items-fn
                (get-instance db :ethlance-db)
                (->> users
                  (medley/map-vals :freelancer/skills-count)
                  (medley/remove-vals nil?))
                :freelancer/skills
                :contract/freelancer-skills-loaded
                :log-error)]}})))

(reg-event-fx
  :contract/freelancer-skills-loaded
  interceptors
  (fn [{:keys [db]} [freelancer-skills]]
    {:db (-> db
           (update :app/users (partial merge-with merge) freelancer-skills))}))

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
  :contract.job/set-hiring-done
  interceptors
  (fn [{:keys [db]} [form-data amount address]]
    (let [{:keys [:web3 :active-address :form.job/set-hiring-done]} db
          {:keys [:gas-limit]} set-hiring-done]
      {:web3-fx.contract/state-fns
       {:web3 web3
        :db-path [:contract/state-fns]
        :fns [(concat
                [(get-instance db :ethlance-job)
                 :set-job-hiring-done]
                (get-args form-data ethlance-db/job-set-hiring-done-args)
                [{:gas gas-limit
                  :from (or address active-address)}
                 [:form/start-loading :form.job/set-hiring-done]
                 :contract/transaction-error
                 [:contract.job/set-hiring-done-receipt gas-limit form-data]])]}})))

(reg-event-fx
  :contract.job/set-hiring-done-receipt
  [interceptors log-used-gas]
  (fn [{:keys [db]} [{:keys [:job/id]} {:keys [success?]}]]
    (merge
      {:db (cond-> db
             success? (assoc-in [:app/jobs id :job/status] 2)
             true (assoc-in [:form.job/set-hiring-done :loading?] false))}
      (when-not success?
        {:dispatch [:snackbar/show-error]}))))

(reg-event-fx
  :contract.job/add-invitation
  interceptors
  (fn [{:keys [db]} [values address]]
    (let [{:keys [web3 active-address eth/contracts]} db]
      {:web3-fx.contract/state-fns
       {:web3 web3
        :db-path [:contract/state-fns]
        :fns [(concat
                [(get-instance db :ethlance-contract)
                 :add-job-invitation]
                (get-args values ethlance-db/add-invitation-args)
                [{:gas max-gas
                  :from (or address active-address)}
                 :contract/transaction-sent
                 :contract/transaction-error
                 [:contract/transaction-receipt :add-job-invitation max-gas false :log-error]])]}})))

(reg-event-fx
  :contract.contract/add-proposal
  interceptors
  (fn [{:keys [db]} [form-data address]]
    (let [{:keys [:web3 :active-address :form.contract/add-proposal]} db
          {:keys [:gas-limit]} add-proposal]
      {:web3-fx.contract/state-fns
       {:web3 web3
        :db-path [:contract/state-fns]
        :fns [(concat
                [(get-instance db :ethlance-contract)
                 :add-job-proposal]
                (get-args form-data ethlance-db/add-proposal-args)
                [{:gas gas-limit
                  :from (or address active-address)}
                 [:form/start-loading :form.contract/add-proposal]
                 :contract/transaction-error
                 [:contract.contract/add-proposal-receipt gas-limit form-data]])]}})))

(reg-event-fx
  :contract.contract/add-proposal-receipt
  [interceptors log-used-gas]
  (fn [{:keys [db]} [form-data {:keys [success?]}]]
    (merge
      {:db (assoc-in db [:form.contract/add-proposal :loading?] false)}
      (if success?
        {:dispatch-n [[:snackbar/show-message "Your proposal was successfully sent!"]
                      [:contract.views/load-job-proposals (:params (:list/job-proposals db))]]}
        {:dispatch [:snackbar/show-error]}))))

(reg-event-fx
  :contract.contract/add-contract
  interceptors
  (fn [{:keys [db]} [form-data address]]
    (let [{:keys [:web3 :active-address :form.contract/add-contract]} db
          {:keys [:gas-limit]} add-contract]
      {:web3-fx.contract/state-fns
       {:web3 web3
        :db-path [:contract/state-fns]
        :fns [(concat
                [(get-instance db :ethlance-contract)
                 :add-job-contract]
                (get-args form-data ethlance-db/add-contract-args)
                [{:gas gas-limit
                  :from (or address active-address)}
                 [:form/start-loading :form.contract/add-contract]
                 :contract/transaction-error
                 [:contract.contract/add-contract-receipt gas-limit form-data]])]}})))

(reg-event-fx
  :contract.contract/add-contract-receipt
  [interceptors log-used-gas]
  (fn [{:keys [db]} [form-data {:keys [success?]}]]
    (merge
      {:db (assoc-in db [:form.contract/add-contract :loading?] false)}
      (if success?
        {:dispatch [:contract.db/load-contracts (select-keys ethlance-db/contract-schema
                                                             [:contract/description
                                                              :contract/created-on
                                                              :contract/status])
                    [(:contract/id form-data)]]}
        {:dispatch [:snackbar/show-error]}))))

(reg-event-fx
  :contract.contract/add-feedback
  interceptors
  (fn [{:keys [db]} [form-data address]]
    (let [{:keys [:web3 :active-address :form.contract/add-feedback]} db
          {:keys [:gas-limit]} add-feedback]
      {:web3-fx.contract/state-fns
       {:web3 web3
        :db-path [:contract/state-fns]
        :fns [(concat
                [(get-instance db :ethlance-contract)
                 :add-job-contract-feedback]
                (get-args form-data ethlance-db/add-contract-feedback-args)
                [{:gas gas-limit
                  :from (or address active-address)}
                 [:form/start-loading :form.contract/add-feedback]
                 :contract/transaction-error
                 [:contract.contract/add-feedback-receipt gas-limit form-data]])]}})))

(reg-event-fx
  :contract.contract/add-feedback-receipt
  [interceptors log-used-gas]
  (fn [{:keys [db]} [form-data {:keys [success?]}]]
    (let [{:keys [:contract/freelancer]} (get (:app/contracts db) (:contract/id form-data))
          schema (if (= freelancer (:user/id (get-active-user db)))
                   ethlance-db/freelancer-feedback-schema
                   ethlance-db/employer-feedback-schema)]
      (merge
        {:db (assoc-in db [:form.contract/add-feedback :loading?] false)}
        (if success?
          {:dispatch [:contract.db/load-contracts (merge schema (select-keys ethlance-db/contract-schema
                                                                             [:contract/status
                                                                              :contract/done-by-freelancer?]))
                      [(:contract/id form-data)]]}
          {:dispatch [:snackbar/show-error]})))))

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
  (fn [{:keys [db]} [form-data amount address]]
    (let [{:keys [:web3 :active-address :form.invoice/pay]} db
          {:keys [:gas-limit]} pay]
      {:web3-fx.contract/state-fns
       {:web3 web3
        :db-path [:contract/state-fns]
        :fns [(concat
                [(get-instance db :ethlance-invoice)
                 :pay-invoice]
                (get-args form-data ethlance-db/pay-invoice-args)
                [{:gas gas-limit
                  :value amount
                  :from (or address active-address)}
                 [:form/start-loading :form.invoice/pay]
                 :contract/transaction-error
                 [:contract.invoice/pay-receipt gas-limit form-data]])]}})))

(reg-event-fx
  :contract.invoice/cancel
  interceptors
  (fn [{:keys [db]} [form-data address]]
    (let [{:keys [:web3 :active-address :form.invoice/cancel]} db
          {:keys [:gas-limit]} cancel]
      {:web3-fx.contract/state-fns
       {:web3 web3
        :db-path [:contract/state-fns]
        :fns [(concat
                [(get-instance db :ethlance-invoice)
                 :cancel-invoice]
                (get-args form-data ethlance-db/cancel-invoice-args)
                [{:gas gas-limit
                  :from (or address active-address)}
                 [:form/start-loading :form.invoice/cancel]
                 :do-nothing
                 [:contract.invoice/cancel-receipt gas-limit form-data]])]}})))


(reg-event-fx
  :contract.invoice/cancel-receipt
  [interceptors log-used-gas]
  (fn [{:keys [db]} [{:keys [:invoice/id]} {:keys [success?]}]]
    (merge
      {:db (cond-> db
             success? (assoc-in [:app/invoices id :invoice/status] 3)
             true (assoc-in [:form.invoice/cancel :loading?] false))}
      (when-not success?
        {:dispatch [:snackbar/show-error]}))))

(reg-event-fx
  :contract.invoice/pay-receipt
  [interceptors log-used-gas]
  (fn [{:keys [db]} [{:keys [:invoice/id]} {:keys [success?]}]]
    (merge
      {:db (cond-> db
             success? (assoc-in [:app/invoices id :invoice/status] 2)
             true (assoc-in [:form.invoice/pay :loading?] false))}
      (when-not success?
        {:dispatch [:snackbar/show-error]}))))

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


;; ============contracts

(reg-event-fx
  :contract.views/load-job-proposals
  interceptors
  (fn [{:keys [db]} [values]]
    {:db (init-list-load db :list/job-proposals values)
     :web3-fx.contract/constant-fns
     {:fns [(concat
              [(get-instance db :ethlance-views)
               :get-job-contracts]
              (get-args values ethlance-db/get-job-contracts-args)
              [:contract/job-proposals-loaded
               :log-error])]}}))

(reg-event-fx
  :contract/job-proposals-loaded
  interceptors
  (fn [{:keys [db]} [contract-ids]]
    (let [contract-ids (u/big-nums->nums contract-ids)
          job-proposals-list (:list/job-proposals db)]
      {:db (update db :list/job-proposals merge {:items contract-ids :loading? false})
       :dispatch [:contract.db/load-contracts
                  (ethlance-db/without-strings ethlance-db/proposal+invitation-schema)
                  (u/sort-paginate-ids job-proposals-list contract-ids)]})))

(reg-event-fx
  :contract.views/load-job-feedbacks
  interceptors
  (fn [{:keys [db]} [values]]
    {:db (init-list-load db :list/job-feedbacks values)
     :web3-fx.contract/constant-fns
     {:fns [(concat
              [(get-instance db :ethlance-views)
               :get-job-contracts]
              (get-args values ethlance-db/get-job-contracts-args)
              [:contract/job-feedbacks-loaded
               :log-error])]}}))

(reg-event-fx
  :contract/job-feedbacks-loaded
  interceptors
  (fn [{:keys [db]} [contract-ids]]
    (let [contract-ids (u/big-nums->nums contract-ids)
          job-feedbacks-list (:list/job-feedbacks db)]
      {:db (update db :list/job-feedbacks merge {:items contract-ids :loading? false})
       :dispatch [:contract.db/load-contracts
                  ethlance-db/feedback-schema
                  (u/sort-paginate-ids job-feedbacks-list contract-ids)
                  1]})))

(reg-event-fx
  :contract.db/load-contracts
  interceptors
  (fn [{:keys [db]} [schema contract-ids parts-count]]
    (let [contract-ids (u/big-nums->nums contract-ids)
          {:keys [fields ids]} (find-needed-fields (keys schema)
                                                   (:app/contracts db)
                                                   contract-ids
                                                   ethlance-db/contract-editable-fields)]
      {:web3-fx.contract/constant-fns
       {:fns (entities-fns (if parts-count parts-count (count contract-ids))
                           (get-instance db :ethlance-db)
                           ids
                           fields
                           :contract/contracts-loaded
                           :log-error)}})))

(reg-event-fx
  :contract/contracts-loaded
  interceptors
  (fn [{:keys [db]} [contracts]]
    (let [contracts (->> contracts
                      (remove u/empty-contract?)
                      (u/assoc-key-as-value :contract/id))
          contract-vals (vals contracts)
          freelancer-ids (map :contract/freelancer contract-vals)
          job-ids (map :contract/job contract-vals)]
      {:db (-> db
             (update :app/contracts (partial merge-with merge) contracts))
       :dispatch-n [[:contract.db/load-users ethlance-db/user-schema freelancer-ids]
                    [:contract.db/load-jobs (dissoc ethlance-db/job-schema :job/description)
                     job-ids]]})))

(reg-event-fx
  :contract.views/load-my-users-contracts
  interceptors
  (fn [{:keys [db]} [values]]
    (let [user-ids (->> (get-my-users db)
                     (filter :user/freelancer?)
                     (map :user/id))]
      (when (seq user-ids)
        {:web3-fx.contract/constant-fns
         {:fns [(concat
                  [(get-instance db :ethlance-views)
                   :get-freelancers-job-contracts]
                  (get-args (merge {:user/ids user-ids} values) ethlance-db/load-my-users-contracts)
                  [[:contract.db/load-contracts
                    (select-keys ethlance-db/contract-all-schema [:contract/job
                                                                  :contract/freelancer
                                                                  :contract/status
                                                                  :contract/freelancer-feedback-on])]
                   :log-error])]}}))))

;; ============invoices

(reg-event-fx
  :contract.views/load-job-invoices
  interceptors
  (fn [{:keys [db]} [values]]
    {:db (init-list-load db :list/job-invoices values)
     :web3-fx.contract/constant-fns
     {:fns [(concat
              [(get-instance db :ethlance-views)
               :get-job-invoices]
              (get-args values ethlance-db/load-job-invoices-args)
              [:contract/job-invoices-loaded
               :log-error])]}}))

(reg-event-fx
  :contract/job-invoices-loaded
  interceptors
  (fn [{:keys [db]} [invoice-ids]]
    (let [invoice-ids (u/big-nums->nums invoice-ids)
          job-invoices-list (:list/job-invoices db)]
      {:db (update db :list/job-invoices merge {:items invoice-ids :loading? false})
       :dispatch [:contract.db/load-invoices
                  ethlance-db/invoices-table-schema
                  (u/sort-paginate-ids job-invoices-list invoice-ids)]})))

(reg-event-fx
  :contract.views/load-contract-invoices
  interceptors
  (fn [{:keys [db]} [values]]
    {:db (init-list-load db :list/contract-invoices values)
     :web3-fx.contract/constant-fns
     {:fns [(concat
              [(get-instance db :ethlance-views)
               :get-contract-invoices]
              (get-args values ethlance-db/load-contract-invoices-args)
              [:contract/contract-invoices-loaded
               :log-error])]}}))

(reg-event-fx
  :contract/contract-invoices-loaded
  interceptors
  (fn [{:keys [db]} [invoice-ids]]
    (let [invoice-ids (u/big-nums->nums invoice-ids)]
      {:db (update db :list/contract-invoices merge {:items invoice-ids :loading? false})
       :dispatch [:contract.db/load-invoices
                  ethlance-db/invoices-table-schema
                  (u/sort-paginate-ids (:list/contract-invoices db) invoice-ids)]})))

(reg-event-fx
  :contract.db/load-invoices
  interceptors
  (fn [{:keys [db]} [schema invoice-ids]]
    (let [{:keys [fields ids]} (find-needed-fields (keys schema)
                                                   (:app/invoices db)
                                                   invoice-ids
                                                   ethlance-db/invoice-editable-fields)]
      {:web3-fx.contract/constant-fns
       {:fns (entities-fns (get-instance db :ethlance-db)
                           ids
                           fields
                           :contract/invoices-loaded
                           :log-error)}})))

(reg-event-fx
  :contract/invoices-loaded
  interceptors
  (fn [{:keys [db]} [invoices]]
    (let [invoices (->> invoices
                     (remove u/empty-invoice?)
                     (u/assoc-key-as-value :invoice/id))
          contract-ids (map :invoice/contract (vals invoices))]
      {:db (update db :app/invoices (partial merge-with merge) invoices)
       :dispatch [:contract.db/load-contracts (ethlance-db/without-strings ethlance-db/contract-schema)
                  contract-ids]})))

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
  :form/search-jobs-changed
  interceptors
  (fn [{:keys [db]} [key value]]
    (let [new-db (assoc-in db [:form/search-jobs key] value)]
      {:db new-db
       :dispatch [:contract.search/search-jobs (:form/search-jobs new-db)]})))

(reg-event-fx
  :form/search-freelancers-changed
  interceptors
  (fn [{:keys [db]} [key value]]
    (let [new-db (assoc-in db [:form/search-freelancers key] value)]
      {:db new-db
       :dispatch [:contract.search/search-freelancers (:form/search-freelancers new-db)]})))

(reg-event-db
  :form/value-changed
  interceptors
  (fn [db [form-key field-key value]]
    (assoc-in db [form-key :data field-key] value)))

(reg-event-db
  :list/set-offset
  interceptors
  (fn [db [list-db-path offset]]
    (assoc-in db (conj list-db-path :offset) offset)))

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

(reg-event-db
  :form/start-loading
  interceptors
  (fn [db [form-key]]
    (assoc-in db [form-key :loading?] true)))

(reg-event-db
  :form/stop-loading
  interceptors
  (fn [db [form-key]]
    (assoc-in db [form-key :loading?] false)))

(reg-event-db
  :form/set-invalid
  interceptors
  (fn [db [form-key invalid?]]
    (assoc-in db [form-key :invalid?] invalid?)))

(reg-event-db
  :snackbar/show-error
  interceptors
  (fn [db [error-text]]
    (update db :snackbar merge
            {:open? true
             :message (or error-text "Oops, we got an error while saving to blockchain")})))

(reg-event-db
  :snackbar/show-message
  interceptors
  (fn [db [message]]
    (update db :snackbar merge
            {:open? true
             :message message})))

(reg-event-db
  :snackbar/close
  interceptors
  (fn [db _]
    (assoc-in db [:snackbar :open?] false)))

(reg-event-fx
  :contract/transaction-sent
  interceptors
  (fn [_ [tx-hash]]
    #_(console :log tx-hash)))

(reg-event-fx
  :contract/transaction-error
  interceptors
  (fn [_ errors]
    (apply console :error errors)))

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
    (.clear js/console)
    {:async-flow {:first-dispatch [:load-eth-contracts]
                  :rules [{:when :seen?
                           :events [:eth-contracts-loaded]
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
    (apply console :log (u/big-nums->nums result))))

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
  (dispatch [:contract/call :ethlance-db :get-u-int-value (u/sha3 :contract/count)])
  (dispatch [:contract/call :ethlance-db :get-u-int-value (u/sha3 :category/jobs-count 1)])
  (dispatch [:contract/call :ethlance-db :get-u-int-value (u/sha3 :skill/jobs-count 5)])
  (dispatch [:contract/call :ethlance-db :get-address-value (u/sha3 :user/address 1)])
  (dispatch [:contract/call :ethlance-db :get-bytes32-value (u/sha3 :user/name 1)])
  (dispatch [:contract/call :ethlance-db :get-u-int-value (u/sha3 :freelancer/hourly-rate 1)])
  (dispatch [:contract/call :ethlance-db :get-u-int-value (u/sha3 :contract/invoices-count 1)])
  (dispatch [:contract/call :ethlance-db :get-u-int-value (u/sha3 :contract/freelancer+job 1 1)])

  (dispatch [:contract/call :ethlance-user :diff #{10 11 12} #{1 2 3 4 5 6}])
  (dispatch [:contract/call :ethlance-user :diff [10 11 12] [1 2 3 4 5 6]])
  (dispatch [:contract/call :ethlance-user :diff [2] [1]])
  (dispatch [:contract/call :ethlance-user :diff [2 3] [1 3]])
  (dispatch [:contract/call :ethlance-user :sort (repeatedly 30 (partial rand-int 100))])
  (dispatch [:contract/call :ethlance-user :intersect [1 2 3 4] [2 8 1 6 5 4]])

  (dispatch [:contract/call :ethlance-db :get-u-int-value (storage-keys 6)])
  (get-entity 3 (keys ethlance-db/account-schema) (get-ethlance-db))

  (get-entity 2 (keys ethlance-db/account-schema) (get-ethlance-db))
  (get-entities (range 1 10) (keys (dissoc ethlance-db/account-schema
                                           :freelancer/description
                                           :employer/description)) (get-ethlance-db))

  (get-entity 1 (keys ethlance-db/proposal+invitation-schema) (get-ethlance-db))
  (get-entity 1 (keys ethlance-db/job-schema) (get-ethlance-db))

  (get-entity 1 (keys ethlance-db/contract-schema) (get-ethlance-db))

  (get-entity 1 (keys ethlance-db/invoice-schema) (get-ethlance-db))
  (get-entities [1] (keys ethlance-db/contract-schema) (get-ethlance-db))

  (get-entity 1 [:freelancer/skills-count] (get-ethlance-db))

  (get-entities (range 1 10) (keys (dissoc ethlance-db/job-schema :job/description)) (get-ethlance-db))
  (get-entities [12] (keys ethlance-db/skill-schema) (get-ethlance-db))
  (get-entities-field-items {5 10} :skill/freelancers
                            (get-in @re-frame.db/app-db [:eth/contracts :ethlance-db :instance]))

  (get-entities-field-items {1 6} :freelancer/skills
                            (get-in @re-frame.db/app-db [:eth/contracts :ethlance-db :instance]))

  (get-entities-field-items {13 1} :skill/freelancers-keys
                            (get-in @re-frame.db/app-db [:eth/contracts :ethlance-db :instance]))


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

  (dispatch [:contract.search/search-freelancers {:search/category 0
                                                  :search/skills [12]
                                                  :search/min-avg-rating 0
                                                  :search/min-freelancer-ratings-count 0
                                                  :search/min-hourly-rate 0
                                                  :search/max-hourly-rate 0
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

  (dispatch [:contract.search/search-jobs {:search/category 0
                                           :search/skills []
                                           :search/payment-types [1 2]
                                           :search/experience-levels [1 2 3]
                                           :search/estimated-durations [1 2 3 4]
                                           :search/hours-per-weeks [1 2]
                                           :search/min-budget 0
                                           :search/min-employer-avg-rating 0
                                           :search/min-employer-ratings-count 0
                                           :search/country 0
                                           :search/language 0
                                           :search/offset 0
                                           :search/limit 10}])
  (dispatch [:contract/state-call :ethlance-user :test-db 5])

  (dispatch [:contract.views/get-freelancer-contracts {:user/id 1
                                                       :contract/status 1
                                                       :job/status 1}])

  (dispatch [:contract.views/get-freelancer-invoices {:user/id 1 :invoice/status 1}])
  (dispatch [:contract.views/get-freelancer-contracts {:user/id 1 :contract/done? true}])
  (dispatch [:contract.views/get-job-contracts {:job/id 1}])
  (dispatch [:contract.views/load-contracts {:job/id 1}])
  (dispatch [:contract.views/get-job-invoices {:job/id 1 :invoice/status 3}])
  (dispatch [:contract.views/get-employer-jobs {:user/id 2 :job/status 2}])
  (dispatch [:contract.views/load-my-user-ids {:user/addresses (:my-addresses @re-frame.db/app-db)}])

  )