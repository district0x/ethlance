(ns ethlance.events
  (:require
    [ajax.core :as ajax]
    [akiroz.re-frame.storage :as re-frame-storage]
    [camel-snake-kebab.core :as cs :include-macros true]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs-web3.personal :as web3-personal]
    [cljs.spec :as s]
    [clojure.data :as data]
    [clojure.set :as set]
    [day8.re-frame.async-flow-fx]
    [day8.re-frame.http-fx]
    [ethlance.db :refer [default-db]]
    [ethlance.ethlance-db :as ethlance-db :refer [get-entity get-entities get-entities-field-items]]
    [ethlance.generate-db]
    [ethlance.utils :as u]
    [ethlance.window-fx]
    [goog.string :as gstring]
    [goog.string.format]
    [madvas.re-frame.google-analytics-fx]
    [madvas.re-frame.web3-fx]
    [medley.core :as medley]
    [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx inject-cofx path trim-v after debug reg-fx console
                                        dispatch dispatch-sync]]
    [ethlance.constants :as constants]))

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

(defn args-map->vec [values args-order]
  (mapv (fn [arg-key]
          (let [val (get values arg-key)]
            (if (sequential? arg-key)
              (map #(get values %) arg-key)
              val)))
        args-order))

(defn get-ethlance-db []
  (get-in @re-frame.db/app-db [:eth/contracts :ethlance-db :instance]))

(defn entities-fns
  ([instance ids fields on-success on-failure]
   (entities-fns (count (set ids)) instance ids fields on-success on-failure))
  ([load-per instance ids fields on-success on-failure]
   (let [ids (distinct (filter pos? ids))
         parts-count (if (and load-per (< load-per (count ids))) load-per (count ids))]
     (if (and (seq ids) (seq fields))
       (for [part-ids (partition parts-count ids)]
         (let [[fields records types] (ethlance-db/get-entities-args part-ids fields)]
           [instance :get-entity-list records types [:entities-loaded part-ids fields on-success] on-failure]))
       []))))

(defn entities-field-items-fn [instance id-counts field on-success on-failure]
  (let [[ids+sub-ids field records types] (ethlance-db/get-entities-field-items-args id-counts field)]
    (when (seq records)
      [instance :get-entity-list records types [:entities-field-items-loaded ids+sub-ids field on-success] on-failure])))

(defn all-contracts-loaded? [db]
  (every? #(and (:abi %) (if goog.DEBUG true (:bin %))) (vals (:eth/contracts db))))


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
           :ids []} ids))

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

(defn get-active-user [{:keys [:active-address :blockchain/addresses :app/users]}]
  (users (:user/id (addresses active-address))))

(defn get-my-users [{:keys [:my-addresses :blockchain/addresses :app/users]}]
  (map (comp users :user/id addresses) my-addresses))

(defn merge-data-from-query [db {:keys [:handler]} query-data]
  (if-let [form (constants/handler->form handler)]
    (assoc db form (merge (default-db form) query-data))
    db))

(defn calculate-search-seed [args on-load-seed]
  (as-> args a
        (dissoc a :search/offset :search/limit)
        (pr-str a)
        (u/md5-bytes a)
        (reduce + a)
        (* on-load-seed a)
        ))

(reg-fx
  :location/set-hash
  (fn [[route route-params]]
    (u/nav-to! route route-params)))

(reg-fx
  :location/add-to-query
  (fn [[query-params]]
    (u/add-to-location-query! query-params)))

(reg-fx
  :location/set-query
  (fn [[query-params]]
    (u/set-location-query! query-params)))

(reg-event-fx
  :window/scroll-to-top
  interceptors
  (fn []
    {:window/scroll-to-top true}))

(reg-event-fx
  :window/on-resize
  interceptors
  (fn [{:keys [db]} [width]]
    (let [width-size (cond
                       (>= width 1200) 3
                       (>= width 1024) 2
                       (>= width 768) 1
                       :else 0)]
      {:db (assoc db :window/width-size width-size)})))

(reg-event-fx
  :location/set-query
  interceptors
  (fn [_ args]
    {:location/set-query args}))

(reg-event-fx
  :location/set-hash
  interceptors
  (fn [_ args]
    {:location/set-hash args}))

(reg-event-fx
  :initialize
  (inject-cofx :localstorage)
  (fn [{:keys [localstorage]} [deploy-contracts?]]
    (let [{:keys [:web3 :provides-web3? :active-page]} default-db]
      (merge
        {:db (as-> default-db db
                   (merge-data-from-query db active-page (u/current-url-query))
                   (merge-with (partial merge-with merge) db localstorage)
                   (assoc db :on-load-seed (rand-int 99999)))
         :async-flow {:first-dispatch [:load-eth-contracts]
                      :rules [{:when :seen?
                               :events [:eth-contracts-loaded :blockchain/my-addresses-loaded]
                               :dispatch-n [[:contract.config/get-configs {:config/keys (keys (:eth/config default-db))}]
                                            [:contract.views/load-my-users]]
                               :halt? true}]}
         :window/on-resize {:dispatch [:window/on-resize]
                            :resize-interval 166}}
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
  (fn [{:keys [db]} [{:keys [:handler] :as match}]]
    (merge
      {:db (-> db
             (assoc :active-page match)
             (merge-data-from-query match (u/current-url-query)))}
      (when-not (= handler (:handler (:active-page db)))
        {:window/scroll-to-top true})
      #_(when-let [form (constants/handler->form handler)]
          (let [[changed-from-default] (data/diff (db form) (default-db form))]
            (when changed-from-default
              {:location/add-to-query [changed-from-default]}))))))

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
               (assoc :active-address active-address))
         :web3-fx.blockchain/balances
         {:web3 (:web3 db)
          :watch? true
          :blockchain-filter-opts "latest"
          :db-path [:blockchain :balances]
          :addresses addresses
          :dispatches [:blockchain/address-balance-loaded :blockchain/on-error]}}))))

(reg-event-fx
  :blockchain/address-balance-loaded
  interceptors
  (fn [{:keys [db]} [balance address]]
    {:db (assoc-in db [:blockchain/addresses address :address/balance] balance)}))

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
    {:dispatch [:contract.views/load-skill-names]}))

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
  :contract.config/get-configs
  interceptors
  (fn [{:keys [db]} [values]]
    (let [fn-key :ethlance-config/get-configs]
      {:web3-fx.contract/constant-fns
       {:fns [[(get-instance db (keyword (namespace fn-key)))
               fn-key
               (:config/keys values)
               [:contract.config/get-configs-loaded (:config/keys values)]
               :log-error]]}})))

(reg-event-fx
  :contract.config/get-configs-loaded
  interceptors
  (fn [{:keys [db]} [config-keys config-values]]
    (if (seq config-values)
      {:db (update db :eth/config merge (zipmap config-keys (u/big-nums->nums config-values)))}
      {:db (assoc db :contracts-not-found? true)})))

(reg-event-fx
  :contract.config/add-skills
  interceptors
  (fn [{:keys [db]} [form-data address]]
    {:dispatch [:form/submit
                {:form-data form-data
                 :address address
                 :fn-key :ethlance-config/add-skills
                 :form-key :form.config/add-skills
                 :receipt-dispatch-n [[:snackbar/show-message "Skills were successfully added!"]
                                      [:contract.views/load-skill-names]
                                      [:form/set-value :form.config/add-skills :skill/names [] false]]}]}))

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
  :contract.views/my-new-user-id-loaded
  interceptors
  (fn [{:keys [db]} [route address [user-id]]]
    (let [user-id (u/big-num->num user-id)
          i (.indexOf (get-in db [:list/my-users :params :user/addresses]) address)]
      {:db (update-in db [:list/my-users :items] (comp #(assoc % i user-id) vec))
       :location/set-hash [route {:user/id user-id}]})))

(reg-event-fx
  :contract.user/register-freelancer
  interceptors
  (fn [{:keys [db]} [form-data address]]
    (let [address (or address (:active-address db))]
      {:dispatch [:form/submit
                  {:form-data form-data
                   :address address
                   :fn-key :ethlance-user/register-freelancer
                   :form-key :form.user/register-freelancer
                   :receipt-dispatch-n [[:snackbar/show-message "Your freelancer profile was successfully created"]
                                        [:contract.views/load-user-id-by-address address
                                         [:contract.views/my-new-user-id-loaded :freelancer/detail address]]]}]})))

(reg-event-fx
  :contract.user/register-employer
  interceptors
  (fn [{:keys [db]} [form-data address]]
    (let [address (or address (:active-address db))]
      {:dispatch [:form/submit
                  {:form-data form-data
                   :address address
                   :fn-key :ethlance-user/register-employer
                   :form-key :form.user/register-employer
                   :receipt-dispatch-n [[:snackbar/show-message "Your employer profile was successfully created"]
                                        [:contract.views/load-user-id-by-address address
                                         [:contract.views/my-new-user-id-loaded :employer/detail address]]]}]})))

(reg-event-fx
  :contract.user/set-user
  interceptors
  (fn [{:keys [db]} [form-data address]]
    {:dispatch [:form/submit
                {:form-data form-data
                 :address address
                 :fn-key :ethlance-user/set-user
                 :form-key :form.user/set-user
                 :receipt-dispatch-n [[:snackbar/show-message "Your user profile was successfully updated"]
                                      [:contract.db/load-users ethlance-db/user-schema [(:user/id (get-active-user db))]]]}]}))

(reg-event-fx
  :contract.user/set-freelancer
  interceptors
  (fn [{:keys [db]} [form-data address]]
    {:dispatch [:form/submit
                {:form-data form-data
                 :address address
                 :fn-key :ethlance-user/set-freelancer
                 :form-key :form.user/set-freelancer
                 :receipt-dispatch-n [[:snackbar/show-message "Your freelancer profile was successfully updated"]
                                      [:contract.db/load-users
                                       (merge ethlance-db/freelancer-schema
                                              (select-keys ethlance-db/user-schema [:user/freelancer?]))
                                       [(:user/id (get-active-user db))]]]}]}))

(reg-event-fx
  :contract.user/set-employer
  interceptors
  (fn [{:keys [db]} [form-data address]]
    {:dispatch [:form/submit
                {:form-data form-data
                 :address address
                 :fn-key :ethlance-user/set-employer
                 :form-key :form.user/set-employer
                 :receipt-dispatch-n [[:snackbar/show-message "Your employer profile was successfully updated"]
                                      [:contract.db/load-users
                                       (merge ethlance-db/employer-schema
                                              (select-keys ethlance-db/user-schema [:user/employer?]))
                                       [(:user/id (get-active-user db))]]]}]}))

(reg-event-fx
  :after-eth-contracts-loaded
  interceptors
  (fn [{:keys [db]} [load-dispatch]]
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
  (fn [{:keys [db]} [load-dispatch]]
    (if (and (every? (comp pos? :user/status) (get-my-users db))
             (all-contracts-loaded? db))
      {:dispatch load-dispatch}
      {:async-flow {:first-dispatch [:do-nothing]
                    :rules [{:when :seen?
                             :events [:contract/users-loaded]
                             :dispatch (conj [:after-my-users-loaded] load-dispatch)
                             :halt? true}]}})))

;; ============jobs


(reg-event-fx
  :contract.search/search-jobs
  interceptors
  (fn [{:keys [db]} [args]]
    {:dispatch [:list/load-ids {:list-key :list/search-jobs
                                :fn-key :ethlance-search/search-jobs
                                :load-dispatch-key :contract.db/load-jobs
                                :schema ethlance-db/job-schema
                                :args args
                                :keep-items? true}]}))

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
       {:fns (entities-fns (when (contains? fields :job/description) 1)
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
       :dispatch-n [[:contract.db/load-field-items {:items jobs
                                                    :count-key :job/skills-count
                                                    :items-key :app/jobs
                                                    :field-key :job/skills}]
                    [:contract.db/load-users
                     (merge (ethlance-db/without-strs ethlance-db/employer-schema)
                            ethlance-db/user-schema
                            ethlance-db/user-balance-schema)
                     (map :job/employer (vals jobs))]]})))

;; ============users

(reg-event-fx
  :contract.search/search-freelancers
  interceptors
  (fn [{:keys [db]} [args]]
    (println (calculate-search-seed args (:on-load-seed db)) (mod (calculate-search-seed args (:on-load-seed db)) 8))
    {:dispatch [:list/load-ids {:list-key :list/search-freelancers
                                :fn-key :ethlance-search/search-freelancers
                                :load-dispatch-key :contract.db/load-users
                                :schema (dissoc ethlance-db/freelancer-schema :freelancer/description)
                                :args (assoc args :search/seed (calculate-search-seed args (:on-load-seed db)))
                                :keep-items? true}]}))

(reg-event-fx
  :contract.views/load-my-users
  interceptors
  (fn [{:keys [db]} [addresses]]
    (let [addrs (or addresses (:my-addresses db))]
      {:dispatch [:list/load-ids {:list-key :list/my-users
                                  :fn-key :ethlance-views/get-users
                                  :load-dispatch-key :contract.db/load-users
                                  :schema (dissoc ethlance-db/account-schema
                                                  :freelancer/description
                                                  :employer/description)
                                  :args {:user/addresses addrs}}]})))

(reg-event-fx
  :contract.views/load-user-id-by-address
  interceptors
  (fn [{:keys [db]} [address loaded-dispatch]]
    (let [fn-key :ethlance-views/get-users]
      {:web3-fx.contract/constant-fns
       {:fns [[(get-instance db (keyword (namespace fn-key)))
               fn-key
               [address]
               loaded-dispatch
               :log-error]]}})))

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
                           (remove #{:user/balance} fields)
                           [:contract/users-loaded fields]
                           :log-error)}})))

(reg-event-fx
  :contract/users-loaded
  interceptors
  (fn [{:keys [db]} [fields users]]
    (let [users (->> users
                  (medley/remove-vals u/empty-user?)
                  (u/assoc-key-as-value :user/id))
          address->user-id (into {} (map (fn [[id user]]
                                           {(:user/address user) {:user/id id}})
                                         users))]
      {:db (-> db
             (update :app/users (partial merge-with merge) users)
             (update :blockchain/addresses (partial merge-with merge) address->user-id))
       :dispatch-n (into [[:contract.db/load-freelancer-skills users]]
                         (when (contains? (set fields) :user/balance)
                           [[:blockchain/load-user-balances users]]))})))

(reg-event-fx
  :contract.db/load-field-items
  interceptors
  (fn [{:keys [db]} [{:keys [items count-key items-key field-key]}]]
    {:web3-fx.contract/constant-fns
     {:fns [(entities-field-items-fn
              (get-instance db :ethlance-db)
              (->> items
                (medley/map-vals count-key)
                (medley/remove-vals nil?))
              field-key
              [:contract/field-items-loaded items-key]
              :log-error)]}}))

(reg-event-fx
  :contract.db/load-freelancer-skills
  interceptors
  (fn [{:keys [db]} [users]]
    {:dispatch [:contract.db/load-field-items {:items users
                                               :count-key :freelancer/skills-count
                                               :items-key :app/users
                                               :field-key :freelancer/skills}]}))

(reg-event-fx
  :contract.db/load-freelancer-categories
  interceptors
  (fn [{:keys [db]} [users]]
    {:dispatch [:contract.db/load-field-items {:items users
                                               :count-key :freelancer/categories-count
                                               :items-key :app/users
                                               :field-key :freelancer/categories}]}))

(reg-event-fx
  :contract.db/load-user-languages
  interceptors
  (fn [{:keys [db]} [users]]
    {:dispatch [:contract.db/load-field-items {:items users
                                               :count-key :user/languages-count
                                               :items-key :app/users
                                               :field-key :user/languages}]}))

(reg-event-fx
  :blockchain/load-user-balances
  interceptors
  (fn [{:keys [db]} [users]]
    (when-let [addresses (->> (vals users)
                           (map :user/id)
                           (select-keys (:app/users db))
                           vals
                           (map :user/address)
                           set)]
      {:web3-fx.blockchain/balances {:web3 (:web3 db)
                                     :blockchain-filter-opts "latest"
                                     :addresses addresses
                                     :dispatches [:blockchain/address-balance-loaded :blockchain/on-error]}})))

(reg-event-fx
  :contract/field-items-loaded
  interceptors
  (fn [{:keys [db]} [items-key loaded-items]]
    {:db (-> db
           (update items-key (partial merge-with merge) loaded-items))}))

;;============jobs

(reg-event-fx
  :contract.job/add-job
  interceptors
  (fn [{:keys [db]} [form-data address]]
    {:dispatch [:form/submit
                {:form-data form-data
                 :address address
                 :fn-key :ethlance-job/add-job
                 :form-key :form.job/add-job
                 :receipt-dispatch-n [[:snackbar/show-message "Job has been successfully created"]
                                      [:location/set-hash :employer/jobs]]}]}))

(reg-event-fx
  :contract.job/set-hiring-done
  interceptors
  (fn [{:keys [db]} [form-data address]]
    {:dispatch [:form/submit
                {:form-data form-data
                 :address address
                 :fn-key :ethlance-job/set-job-hiring-done
                 :form-key :form.job/set-hiring-done
                 :receipt-dispatch [:contract.db/load-jobs (select-keys ethlance-db/job-schema [:job/status
                                                                                                :job/hiring-done-on])
                                    [(:job/id form-data)]]}]}))

;; ============contracts

(reg-event-fx
  :contract.db/load-contracts
  interceptors
  (fn [{:keys [db]} [schema contract-ids {:keys [:load-per]}]]
    (let [contract-ids (u/big-nums->nums contract-ids)
          {:keys [fields ids]} (find-needed-fields (keys schema)
                                                   (:app/contracts db)
                                                   contract-ids
                                                   ethlance-db/contract-editable-fields)]
      {:web3-fx.contract/constant-fns
       {:fns (entities-fns load-per
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
  :contract.views/load-my-freelancers-contracts-for-job
  interceptors
  (fn [{:keys [db]} [values]]
    (let [user-ids (->> (get-my-users db)
                     (filter :user/freelancer?)
                     (map :user/id))]
      (when (seq user-ids)
        {:web3-fx.contract/constant-fns
         {:fns [(concat
                  [(get-instance db :ethlance-views)
                   :ethlance-views/get-freelancers-job-contracts]
                  (args-map->vec (merge {:user/ids user-ids} values) ethlance-db/get-freelancers-job-contracts-args)
                  [[:contract.db/load-contracts
                    (select-keys ethlance-db/contract-all-schema [:contract/job
                                                                  :contract/freelancer
                                                                  :contract/status
                                                                  :contract/freelancer-feedback-on])]
                   :log-error])]}}))))

(reg-event-fx
  :contract.contract/add-job-invitation
  interceptors
  (fn [{:keys [db]} [form-data address]]
    {:dispatch [:form/submit
                {:form-data form-data
                 :address address
                 :fn-key :ethlance-contract/add-job-invitation
                 :form-key :form.contract/add-invitation
                 :receipt-dispatch-n [[:snackbar/show-message "Your invitation was successfully sent!"]
                                      [:contract.views/load-employer-jobs-for-freelancer-invite
                                       {:employer/id (:user/id (get-active-user db))
                                        :freelancer/id (:contract/freelancer form-data)}]
                                      [:form/set-value :form.contract/add-invitation :contract/job 0 false]]}]}))

(reg-event-fx
  :contract.contract/add-job-proposal
  interceptors
  (fn [{:keys [db]} [form-data address]]
    {:dispatch [:form/submit
                {:form-data form-data
                 :address address
                 :fn-key :ethlance-contract/add-job-proposal
                 :form-key :form.contract/add-proposal
                 :receipt-dispatch-n [[:snackbar/show-message "Your proposal was successfully sent!"]
                                      [:contract.views/load-job-proposals (:params (:list/job-proposals db))]]}]}))

(reg-event-fx
  :contract.views/load-job-proposals
  [interceptors]
  (fn [{:keys [db]} [args]]
    {:dispatch [:list/load-ids {:list-key :list/job-proposals
                                :fn-key :ethlance-views/get-job-contracts
                                :load-dispatch-key :contract.db/load-contracts
                                :schema (ethlance-db/without-strs ethlance-db/proposal+invitation-schema)
                                :args args}]}))

(reg-event-fx
  :contract.views/load-employer-jobs-for-freelancer-invite
  [interceptors]
  (fn [{:keys [db]} [args]]
    {:dispatch [:list/load-ids {:list-key :list/employer-jobs-open-select-field
                                :fn-key :ethlance-views/get-employer-jobs-for-freelancer-invite
                                :load-dispatch-key :contract.db/load-jobs
                                :schema (select-keys ethlance-db/job-schema [:job/title])
                                :args args}]}))

(reg-event-fx
  :contract.contract/add-contract
  interceptors
  (fn [{:keys [db]} [form-data address]]
    {:dispatch [:form/submit
                {:form-data form-data
                 :address address
                 :fn-key :ethlance-contract/add-job-contract
                 :form-key :form.contract/add-contract
                 :receipt-dispatch [:contract.db/load-contracts (select-keys ethlance-db/contract-schema
                                                                             [:contract/description
                                                                              :contract/created-on
                                                                              :contract/status])
                                    [(:contract/id form-data)]]}]}))

(reg-event-fx
  :contract.contract/add-feedback
  interceptors
  (fn [{:keys [db]} [form-data address]]
    (let [{:keys [:contract/freelancer]} (get (:app/contracts db) (:contract/id form-data))
          schema (if (= freelancer (:user/id (get-active-user db)))
                   ethlance-db/freelancer-feedback-schema
                   ethlance-db/employer-feedback-schema)]
      {:dispatch [:form/submit
                  {:form-data form-data
                   :address address
                   :fn-key :ethlance-contract/add-job-contract-feedback
                   :form-key :form.contract/add-feedback
                   :receipt-dispatch [:contract.db/load-contracts
                                      (merge schema
                                             (select-keys ethlance-db/contract-schema [:contract/status
                                                                                       :contract/done-by-freelancer?]))
                                      [(:contract/id form-data)]]}]})))


;; ============invoices

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
       :dispatch [:contract.db/load-contracts (ethlance-db/without-strs ethlance-db/contract-schema)
                  contract-ids]})))

(reg-event-fx
  :contract.invoice/add-invoice
  interceptors
  (fn [{:keys [db]} [form-data address]]
    {:dispatch [:form/submit
                {:form-data form-data
                 :address address
                 :fn-key :ethlance-invoice/add-invoice
                 :form-key :form.invoice/add-invoice
                 :receipt-dispatch-n [[:snackbar/show-message "Invoice has been successfully created"]
                                      [:location/set-hash :freelancer/invoices]]}]}))

(reg-event-fx
  :contract.invoice/pay-invoice
  interceptors
  (fn [{:keys [db]} [form-data amount address]]
    {:dispatch [:form/submit
                {:form-data form-data
                 :address address
                 :value amount
                 :fn-key :ethlance-invoice/pay-invoice
                 :form-key :form.invoice/pay-invoice
                 :receipt-dispatch [:contract.db/load-invoices (select-keys ethlance-db/invoice-schema
                                                                            [:invoice/status :invoice/paid-on])
                                    [(:invoice/id form-data)]]}]}))

(reg-event-fx
  :contract.invoice/cancel-invoice
  interceptors
  (fn [{:keys [db]} [form-data address]]
    {:dispatch [:form/submit
                {:form-data form-data
                 :address address
                 :fn-key :ethlance-invoice/cancel-invoice
                 :form-key :form.invoice/cancel-invoice
                 :receipt-dispatch [:contract.db/load-invoices (select-keys ethlance-db/invoice-schema
                                                                            [:invoice/status :invoice/cancelled-on])
                                    [(:invoice/id form-data)]]}]}))

(reg-event-fx
  :list/load-ids
  interceptors
  (fn [{:keys [db]} [{:keys [:list-key :fn-key :load-dispatch-key :schema :args :load-per :keep-items?]}]]
    {:db (cond-> db
           (and (not keep-items?) (not= (get-in db [list-key :params]) args))
           (assoc-in [list-key :items] [])

           true (update list-key merge {:loading? true :params args}))
     :web3-fx.contract/constant-fns
     {:fns [(concat
              [(get-instance db (keyword (namespace fn-key)))
               fn-key]
              (args-map->vec args (ethlance-db/eth-contracts-fns fn-key))
              [[:contract.views/ids-loaded list-key load-dispatch-key schema load-per]
               :log-error])]}}))

(reg-event-fx
  :contract.views/ids-loaded
  interceptors
  (fn [{:keys [db]} [list-key load-dispatch-key schema load-per ids]]
    (let [ids (u/big-nums->nums ids)
          items-list (get db list-key)]
      {:db (update db list-key merge {:items ids :loading? false})
       :dispatch [load-dispatch-key schema (u/sort-paginate-ids items-list ids) load-per]})))

(reg-event-fx
  :contract.views/load-skill-names
  interceptors
  (fn [{:keys [db]} [values]]
    {:web3-fx.contract/constant-fns
     {:fns [[(get-instance db :ethlance-views)
             :ethlance-views/get-skill-names
             :contract.views/skill-names-loaded
             :log-error]]}}))

(reg-event-db
  :contract.views/skill-names-loaded
  interceptors
  (fn [db [[ids names]]]
    (update db :app/skills merge (zipmap (u/big-nums->nums ids)
                                         (map (comp (partial hash-map :skill/name) u/remove-zero-chars web3/to-ascii) names)))))

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
  :form.search/set-value
  interceptors
  (fn [{:keys [db]} [field-key field-value]]
    {:location/add-to-query [(merge {field-key field-value}
                                    (when-not (= field-key :search/offset)
                                      {:search/offset 0}))]}))

(reg-event-fx
  :form/submit
  interceptors
  (fn [{:keys [db]} [{:keys [:form-key :fn-key :form-data :value :address] :as props}]]
    (let [form (get db form-key)
          {:keys [:web3 :active-address]} db
          {:keys [:gas-limit]} form]
      {:web3-fx.contract/state-fns
       {:web3 web3
        :db-path [:contract/state-fns]
        :fns [(concat
                [(get-instance db (keyword (namespace fn-key)))
                 fn-key]
                (args-map->vec form-data (ethlance-db/eth-contracts-fns fn-key))
                [(merge
                   {:gas gas-limit
                    :from (or address active-address)}
                   (when value
                     {:value value}))
                 [:form/start-loading form-key]
                 :contract/transaction-error
                 [:form/submit-receipt gas-limit props]])]}})))

(reg-event-fx
  :form/submit-receipt
  [interceptors log-used-gas]
  (fn [{:keys [db]} [{:keys [:receipt-dispatch :receipt-dispatch-n :form-data :form-key]} {:keys [success?]}]]
    (merge
      {:db (assoc-in db [form-key :loading?] false)}
      (when (and success? receipt-dispatch)
        {:dispatch (conj receipt-dispatch form-data)})
      (when (and success? receipt-dispatch-n)
        {:dispatch-n (map #(conj % form-data) receipt-dispatch-n)})
      (when-not success?
        {:dispatch [:snackbar/show-error]}))))

(reg-event-db
  :form/set-value
  interceptors
  (fn [db [form-key field-key value & [validator]]]
    (let [validator (cond
                      (fn? validator) validator
                      (boolean? validator) (constantly validator)
                      :else validator)]
      (cond-> db
        true (assoc-in [form-key :data field-key] value)

        (or (and validator (validator value))
            (nil? validator))
        (update-in [form-key :errors] (comp set (partial remove #{field-key})))

        (and validator (not (validator value)))
        (update-in [form-key :errors] conj field-key)))))

(reg-event-db
  :form/clear-data
  interceptors
  (fn [db [form-key]]
    (assoc-in db [form-key :data] {})))

(reg-event-db
  :form/add-value
  interceptors
  (fn [db [form-key field-key value & [validator]]]
    (let [existing-values (get-in db [form-key :data field-key])]
      {:dispatch [:form/set-value form-key field-key (into [] (conj existing-values value)) validator]})))

(reg-event-db
  :form/remove-value
  interceptors
  (fn [db [form-key field-key value & [validator]]]
    (let [existing-values (get-in db [form-key :data field-key])]
      {:dispatch [:form/set-value form-key field-key (into [] (remove (partial = value) existing-values)) validator]})))

(reg-event-db
  :form/add-error
  interceptors
  (fn [db [form-key error]]
    (update-in db [form-key :errors] conj error)))

(reg-event-db
  :form/remove-error
  interceptors
  (fn [db [form-key error]]
    (update-in db [form-key :errors] (comp set (partial remove #{error})))))

(reg-event-db
  :list/set-offset
  interceptors
  (fn [db [list-db-path offset]]
    (assoc-in db (conj list-db-path :offset) offset)))

(reg-event-db
  :list/set-limit
  interceptors
  (fn [db [list-db-path limit]]
    (assoc-in db (conj list-db-path :limit) limit)))

(reg-event-db
  :list/set-offset-limit
  interceptors
  (fn [db [list-db-path offset limit]]
    (update-in db list-db-path merge {:limit limit :offset offset})))

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
  :form/set-open?
  interceptors
  (fn [db [form-key open?]]
    (assoc-in db [form-key :open?] open?)))

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

(reg-event-db
  :reset-on-load-seed
  interceptors
  (fn [db [number]]
    (assoc db :on-load-seed (or number (rand-int 99999)))))

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

  (get-entity 3 [:user/address] (get-ethlance-db))

  (get-entities (range 1 10) (keys (dissoc ethlance-db/job-schema :job/description)) (get-ethlance-db))
  (get-entities [1] [:user/address] (get-ethlance-db))
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


  (do
    (dispatch-sync [:reset-on-load-seed 0])
    (dispatch-sync [:contract.search/search-freelancers {:search/category 0
                                                         :search/skills []
                                                         :search/min-avg-rating 0
                                                         :search/min-freelancer-ratings-count 0
                                                         :search/min-hourly-rate 0 #_(web3/to-wei 3 :ether)
                                                         :search/max-hourly-rate 0
                                                         :search/country 0
                                                         :search/language 0
                                                         :search/offset 0
                                                         :search/limit 5}]))

  (dispatch [:contract.job/add-job {:job/title "This is Job 1"
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
  (dispatch [:contract.views/load-my-users {:user/addresses (:my-addresses @re-frame.db/app-db)}])

  )