(ns ethlance.events
  (:require
   [ajax.core :as ajax]
   [ajax.edn :as ajax-edn]
   [akiroz.re-frame.storage :as re-frame-storage]
   [cljs-web3.core :as web3]
   [cljs-web3.eth :as web3-eth]
   [cljs-web3.personal :as web3-personal]
   [cljs-web3.utils :as web3-utils]
   [cljs.spec.alpha :as s]
   [clojure.data :as data]
   [clojure.set :as set]
   [day8.re-frame.async-flow-fx]
   [day8.re-frame.http-fx]
   [ethlance.components.confirm-dialog :as confirm-dialog]
   [ethlance.constants :as constants]
   [ethlance.db :refer [default-db generate-mode?]]
   [ethlance.debounce-fx]
   [ethlance.ethlance-db :as ethlance-db :refer [get-entities get-entities-field-items]]
   [ethlance.generate-db]
   [ethlance.interval-fx]
   [ethlance.web3-fx :as web3-fx]
   [ethlance.utils :as u]
   [ethlance.window-fx]
   [goog.string :as gstring]
   [goog.string.format]
   [madvas.re-frame.google-analytics-fx]
   [madvas.re-frame.web3-fx]
   [medley.core :as medley]
   [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx inject-cofx path trim-v after debug reg-fx console dispatch dispatch-sync]]
   [clojure.string :as string]))

(re-frame-storage/reg-co-fx! :ethlance {:fx :localstorage :cofx :localstorage})

(defn check-and-throw
  [a-spec db]
  (when goog.DEBUG
    (when-not (s/valid? a-spec db)
      (.error js/console (s/explain-str a-spec db))
      (throw "Spec check failed"))))

(def check-spec-interceptor (after (partial check-and-throw :ethlance.db/db)))

(def interceptors [check-spec-interceptor
                   #_(when ^boolean goog.DEBUG debug)
                   trim-v])

(defn contract-xhrio [contract-name code-type on-success on-failure]
  {:method :get
   :uri (gstring/format "./contracts/build/%s.%s?v=%s" contract-name (name code-type) constants/contracts-version)
   :timeout 6000
   :response-format (if (= code-type :abi) (ajax/json-response-format) (ajax/text-response-format))
   :on-success on-success
   :on-failure on-failure})

(defn get-contract [db key]
  (get-in db [:eth/contracts key]))

(defn get-instance [db key]
  (get-in db [:eth/contracts key :instance]))

(defn get-contract-class [db key]
  (get-in db [:eth/contracts key :class]))

(defn get-max-gas-limit [db]
  (get-in db [:eth/config :max-gas-limit]))

(defn storage-keys [& args]
  (apply web3-eth/contract-call (get-instance @re-frame.db/app-db :ethlance-db) :storage-keys args))

(defn arg-eth->wei [value arg-key]
  (if (contains? ethlance-db/wei-args arg-key)
    (if (sequential? value)
      (map u/num->wei value)
      (u/num->wei value))
    value))

(defn args-map->vec [values args-order]
  (mapv (fn [arg-key]
          (if (sequential? arg-key)
            (map #(-> (get values %)
                    (arg-eth->wei %))
                 arg-key)
            (-> (get values arg-key)
              (arg-eth->wei arg-key))))
        args-order))

(defn get-ethlance-db []
  (get-in @re-frame.db/app-db [:eth/contracts :ethlance-db :instance]))

(defn all-contracts-loaded? [db]
  (every? #(and (:abi %) (if goog.DEBUG (:bin %) true)) (vals (:eth/contracts db))))

(defn all-contracts-deployed? [db]
  (every? #(and (:instance %) (:address %)) (vals (:eth/contracts db))))


(defn filter-needed-fields [required-fields items ids & [editable-fields]]
  (reduce (fn [acc id]
            (let [item (get items id)
                  needed-fields (set/difference (set required-fields)
                                                (set/difference (set (keys (medley/remove-vals nil? item)))
                                                                editable-fields))]
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
                (let [gas-used-percent (* (/ gas-used gas-limit) 100)
                      gas-used-percent-str (gstring/format "%.2f%" gas-used-percent)]
                  (console :log "gas used:" gas-used-percent-str gas-used (second event))
                  (-> context
                    (update-in [:coeffects :event (dec (count event))]
                               merge
                               {:success? (< gas-used gas-limit)
                                :gas-used-percent gas-used-percent-str})
                    (update-in [:coeffects :event] #(-> % rest vec))
                    (assoc-in [:coeffects :db :last-transaction-gas-used] gas-used-percent)))))
    :after (fn [context]
             (let [event (:event (:coeffects context))]
               (update context :effects merge
                       {:ga/event ["log-used-gas"
                                   (name (:fn-key (first event)))
                                   (str (select-keys (last event) [:gas-used :gas-used-percent :transaction-hash
                                                                   :success?]))]})))))

(defn get-active-user [{:keys [:active-address :app/users]}]
  (users active-address))

(defn get-my-users [{:keys [:my-addresses :app/users]}]
  (select-keys users my-addresses))

(defn filter-contract-setters [db]
  (medley/filter-vals :setter? (:eth/contracts db)))

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
        (* on-load-seed a)))

(defn active-page-this-contract-detail? [{:keys [:active-page]} contract-id]
  (and (= (:handler active-page) :contract/detail)
       (= (js/parseInt (:contract/id (:route-params active-page))) contract-id)))

(defn active-page-this-invoice-detail? [{:keys [:active-page]} invoice-id]
  (and (= (:handler active-page) :invoice/detail)
       (= (js/parseInt (:invoice/id (:route-params active-page))) invoice-id)))

(defn active-page-this-job-detail? [{:keys [:active-page]} job-id]
  (and (= (:handler active-page) :job/detail)
       (= (js/parseInt (:job/id (:route-params active-page))) job-id)))

(comment
  (dispatch [:blockchain/unlock-account "0x98bc90f9bde18341304bd551d693b708e895a2a5" "m"])
  (dispatch [:blockchain/unlock-account "0x8eb34c6197963a0a8756ec43cb6de9bd8d276b14" "m"]))

(reg-fx
  :location/set-hash
  (fn [[route route-params]]
    (when-not generate-mode?
      (u/nav-to! route route-params))))

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
    {:db (assoc db :window/width-size (u/get-window-width-size width))}))

(reg-event-fx
  :location/set-query
  interceptors
  (fn [_ args]
    {:location/set-query args}))

(reg-event-fx
  :location/set-hash
  interceptors
  (fn [{:keys [db]} args]
    {:location/set-hash args}))

(defn select-db-users [{:keys [:app/users]} loaded-users]
  (select-keys users (keys loaded-users)))

(defn select-db-jobs [{:keys [:app/jobs]} loaded-jobs]
  (select-keys jobs (keys loaded-jobs)))

(defn select-db-contracts [{:keys [:app/contracts]} loaded-contracts]
  (select-keys contracts (keys loaded-contracts)))

(defn select-db-invoices [{:keys [:app/invoices]} loaded-invoices]
  (select-keys invoices (keys loaded-invoices)))

(defn migrate-localstorage [localstorage]
  (update localstorage :selected-currency #(if (keyword? %) (constants/currencies-backward-comp %)
                                                            (or % (:selected-currency default-db)))))

(defn assoc-search-skills-form-open [db form-key open-key]
  (assoc db open-key (boolean (seq (get-in db [form-key :search/skills-or])))))

(reg-event-fx
 :initialize
 []
 (fn [_ [deploy-contracts?]]
   {::web3-fx/authorize-ethereum-provider
    {:on-accept [:initialize-rest deploy-contracts?]
     :on-reject [:initialize-rest deploy-contracts?]
     :on-error [:initialize-rest deploy-contracts?]
     :on-legacy [:initialize-rest deploy-contracts?]}}))

(reg-event-fx
 :initialize-rest
 (inject-cofx :localstorage)
 (fn [{:keys [localstorage]} [deploy-contracts?]]
   (let [provides-web3? (boolean (aget js/window "web3"))
         current-provider (and provides-web3? (web3/current-provider (aget js/window "web3")))
         web3 (if provides-web3?
                (new (aget js/window "Web3") current-provider)
                (web3/create-web3 (:node-url default-db)))
         {:keys [:active-page]} default-db
         localstorage (migrate-localstorage localstorage)
         db (as-> default-db db
              (merge-data-from-query db active-page (u/current-url-query))
              (merge-with #(if (map? %1) (merge-with merge %1 %2) %2) db localstorage)
              (assoc db :provides-web3? provides-web3?)
              (assoc db :web3 web3)
              (assoc db :web3-read-only (if (aget current-provider "isMetaMask")
                                          (web3/create-web3 (:node-url default-db))
                                          web3))
              (assoc db :on-load-seed (rand-int 99999))
              (assoc db :drawer-open? (> (:window/width-size db) 2))
              (assoc-search-skills-form-open db :form/search-freelancers :search-freelancers-skills-open?)
              (assoc-search-skills-form-open db :form/search-jobs :search-jobs-skills-open?))]
     (merge
      {:db db
       :dispatch-n [[:load-conversion-rates]
                    [:load-initial-skills]]
       :async-flow {:first-dispatch [:load-eth-contracts]
                    :rules [{:when :seen?
                             :events [:eth-contracts-loaded :blockchain/my-addresses-loaded]
                             :dispatch-n [[:contract.config/get-configs {:config/keys (keys (:eth/config default-db))}]
                                          [:contract.views/load-my-users]]
                             :halt? true}]}
       :window/on-resize {:dispatch [:window/on-resize]
                          :resize-interval 166}
       :ga/page-view [(u/current-location-hash)]
       :dispatch-interval {:dispatch [:load-conversion-rates]
                           :ms 60000
                           :db-path [:load-all-conversion-rates-interval]}}
      (if (or provides-web3? (:load-node-addresses? default-db))
        {:web3-fx.blockchain/fns
         {:web3 web3
          :fns [[web3-eth/accounts :blockchain/my-addresses-loaded [:blockchain/on-error :initialize]]]}}
        {:dispatch [:blockchain/my-addresses-loaded []]})))))

(reg-event-db
  :drawer/set
  interceptors
  (fn [db [open?]]
    (assoc db :drawer-open? open?)))

(reg-event-fx
  :load-eth-contracts
  interceptors
  (fn [{:keys [db]}]
    {:http-xhrio
     (flatten
       (for [[key {:keys [name]}] (:eth/contracts db)]
         (for [code-type (if goog.DEBUG [:abi :bin] [:abi])]
           (contract-xhrio name code-type [:contract/loaded key code-type] [:log-error :load-eth-contracts]))))}))

(reg-event-fx
  :contracts/deploy-all
  interceptors
  (fn [{:keys [db]} [address-index]]
    (let [ethance-db (get-in db [:eth/contracts :ethlance-db])]
      {:web3-fx.blockchain/fns
       {:web3 (:web3 db)
        :fns [[web3-eth/contract-new
               (:abi ethance-db)
               {:gas u/max-gas-limit
                :data (:bin ethance-db)
                :from (if address-index
                        (nth (:my-addresses db) address-index)
                        (:active-address db))}
               [:contract/ethlance-db-deployed address-index]
               [:log-error :contracts/deploy-all]]]}})))

(reg-event-fx
  :contract/ethlance-db-deployed
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [db localstorage]} [address-index instance]]
    (when-let [db-address (aget instance "address")]
      (console :log :ethlance-db " deployed at " db-address)
      {:db (update-in db [:eth/contracts :ethlance-db] merge {:address db-address :instance instance})
       :localstorage (assoc-in localstorage [:eth/contracts :ethlance-db] {:address db-address})
       :dispatch [:contracts/deploy (keys (dissoc (:eth/contracts db) :ethlance-db)) address-index]})))

(reg-event-fx
  :contracts/deploy
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [db localstorage]} [contract-keys address-index]]
    {:web3-fx.blockchain/fns
     {:web3 (:web3 db)
      :fns (for [[key {:keys [abi bin]}] (select-keys (:eth/contracts db) contract-keys)]
             (remove nil?
                     [web3-eth/contract-new
                      abi
                      (when-not (= key :ethlance-sponsor-wallet)
                        (:address (get-contract db :ethlance-db)))
                      {:gas u/max-gas-limit
                       :data bin
                       :from (if address-index
                               (nth (:my-addresses db) address-index)
                               (:active-address db))}
                      [:contract/deployed key contract-keys address-index]
                      [:log-error :contracts/deploy key]]))}}))

(reg-event-fx
  :contract/deployed
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [db localstorage]} [key contract-keys address-index instance]]
    (when-let [contract-address (aget instance "address")]
      (console :log key " deployed at " contract-address)
      (let [new-db (update-in db [:eth/contracts key] merge {:address contract-address :instance instance})]
        (merge
          {:db new-db
           :localstorage (assoc-in localstorage [:eth/contracts key] {:address contract-address})}
          (when (:setter? (get-contract db key))
            {:dispatch [:contract.db/add-allowed-contracts [key] address-index]})
          (when (all-contracts-deployed? new-db)
            {:dispatch-n [[:eth-contracts-deployed contract-keys address-index]]}))))))

(reg-event-fx
  :eth-contracts-deployed
  interceptors
  (fn [{:keys [db]} [contract-keys address-index]]
    (let [ethlance-invoice-address (:address (get-contract db :ethlance-invoice))
          ethlance-sponsor-address (:address (get-contract db :ethlance-sponsor))
          ethlance-sponsor-wallet-address (:address (get-contract db :ethlance-sponsor-wallet))
          contract-keys (set contract-keys)
          address-from (if address-index
                         (nth (:my-addresses db) address-index)
                         (:active-address db))
          transaction-opts {:from address-from
                            :gas 500000}]
      {:dispatch-n
       (remove
         nil?
         (concat
           (when (or (contains? contract-keys :ethlance-sponsor-wallet)
                     (contains? contract-keys :ethlance-invoice))
             [[:contract/state-call {:contract-key :ethlance-sponsor-wallet
                                     :contract-method :set-ethlance-invoice-contract
                                     :args [ethlance-invoice-address]
                                     :transaction-opts transaction-opts}]
              [:contract/state-call {:contract-key :ethlance-invoice
                                     :contract-method :set-ethlance-sponsor-wallet-contract
                                     :args [ethlance-sponsor-wallet-address]
                                     :transaction-opts transaction-opts}]])
           (when (or (contains? contract-keys :ethlance-sponsor-wallet)
                     (contains? contract-keys :ethlance-sponsor))
             [[:contract/state-call {:contract-key :ethlance-sponsor-wallet
                                     :contract-method :set-ethlance-sponsor-contract
                                     :args [ethlance-sponsor-address]
                                     :transaction-opts transaction-opts}]
              [:contract/state-call {:contract-key :ethlance-sponsor
                                     :contract-method :set-ethlance-sponsor-wallet-contract
                                     :args [ethlance-sponsor-wallet-address]
                                     :transaction-opts transaction-opts}]])))})))

(reg-event-fx
  :load-conversion-rates
  interceptors
  (fn [{:keys [db]}]
    {:http-xhrio {:method :get
                  :uri "https://min-api.cryptocompare.com/data/price?fsym=ETH&tsyms=USD,EUR,RUB,GBP,CNY,JPY"
                  :timeout 20000
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [:conversion-rates-loaded]
                  :on-failure [:log-error :load-conversion-rates]}}))

(reg-event-fx
  :load-conversion-rates-historical
  interceptors
  (fn [{:keys [db]} [timestamp]]
    (when-not (get-in db [:conversion-rates-historical timestamp])
      {:http-xhrio {:method :get
                    :uri (str "https://min-api.cryptocompare.com/data/pricehistorical?fsym=ETH&tsyms=USD,EUR,RUB,GBP,CNY,JPY&ts="
                              (/ timestamp 1000))
                    :timeout 20000
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success [:conversion-rates-loaded-historical timestamp]
                    :on-failure [:log-error :load-conversion-rates-historical]}})))

(reg-event-db
  :conversion-rates-loaded-historical
  interceptors
  (fn [db [timestamp {:keys [:ETH]}]]
    (update-in db [:conversion-rates-historical timestamp] merge (medley/map-keys constants/currency-code->id ETH))))

(reg-event-db
  :conversion-rates-loaded
  interceptors
  (fn [db [response]]
    (update db :conversion-rates merge (medley/map-keys constants/currency-code->id response))))

(reg-event-fx
  :load-initial-skills
  interceptors
  (fn [{:keys [db]} []]
    {:http-xhrio {:method :get
                  :uri (gstring/format "./edn/skills.edn?v=%s" constants/skills-version)
                  :timeout 10000
                  :response-format (ajax-edn/edn-response-format)
                  :on-success [:initial-skills-loaded]
                  :on-failure [:log-error :load-initial-skills]}}))

(reg-event-fx
  :initial-skills-loaded
  interceptors
  (fn [{:keys [db]} [skills]]
    (set! constants/skills skills)
    {:db (assoc db :skills-loaded? true)}
    #_{:db (update db :app/skills merge (medley/map-vals (partial hash-map :skill/name) skills))}))

(reg-event-fx
  :selected-currency/set
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [db localstorage]} [currency]]
    {:db (assoc db :selected-currency currency)
     :localstorage (assoc localstorage :selected-currency currency)}))

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
              [:log-error :estimate-contracts key]])}}))

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
             (assoc :drawer-open? false)
             (merge-data-from-query match (u/current-url-query)))
       :ga/page-view [(u/current-location-hash)]}
      (when-not (= handler (:handler (:active-page db)))
        {:window/scroll-to-top true}))))

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
    (let [addresses (if (seq (:my-addresses-forced db)) (:my-addresses-forced db) addresses)
          active-address (if (contains? (set addresses) (:active-address localstorage))
                           (:active-address localstorage)
                           (first addresses))]
      (merge
        {:db (-> db
               (assoc :my-addresses addresses)
               (assoc :active-address active-address))}
        (when (seq addresses)
          {:web3-fx.blockchain/balances
           {:web3 (:web3 db)
            :watch? true
            :blockchain-filter-opts "latest"
            :db-path [:blockchain :balances]
            :addresses addresses
            :dispatches [:blockchain/address-balance-loaded [:blockchain/on-error :blockchain/my-addresses-loaded]]}})))))

(reg-event-fx
  :blockchain/address-balance-loaded
  interceptors
  (fn [{:keys [db]} [balance address]]
    {:db (assoc-in db [:app/users address :user/balance] (web3/from-wei balance :ether))}))

(reg-event-fx
  :contract/loaded
  interceptors
  (fn [{:keys [db]} [contract-key code-type code]]
    (let [code (if (= code-type :abi) (clj->js code) (str "0x" code))
          contract (get-contract db contract-key)
          contract-address (:address contract)
          web3 (if (contains? #{:ethlance-search-freelancers :ethlance-search-jobs} contract-key)
                 (:web3-read-only db)
                 (:web3 db))]
      (let [new-db (cond-> db
                     true
                     (assoc-in [:eth/contracts contract-key code-type] code)

                     (= code-type :abi)
                     (update-in [:eth/contracts contract-key] merge
                                (when contract-address
                                  {:instance (web3-eth/contract-at web3 code contract-address)})))]
        (merge
          {:db new-db
           :dispatch-n (remove nil?
                               [(when (all-contracts-loaded? new-db)
                                  [:eth-contracts-loaded])
                                (when (and (= code-type :abi)
                                           (= contract-key :ethlance-config))
                                  [:contract.config/setup-listeners])
                                (when (and (= code-type :abi) (:setter? contract) contract-address)
                                  [:contract/load-and-listen-setter-status contract-key])])})))))

(reg-event-fx
  :eth-contracts-loaded
  interceptors
  (fn [{:keys [db]}]
    ))

(reg-event-fx
  :contract/load-and-listen-setter-status
  interceptors
  (fn [{:keys [db]} [contract-key]]
    {:dispatch-n [[:contract/load-setter-status contract-key]
                  [:contract/setup-setter-status-listener contract-key]]}))

(reg-event-fx
  :contract/load-setter-status
  interceptors
  (fn [{:keys [db]} [contract-key]]
    {:web3-fx.contract/constant-fns
     {:fns [[(get-instance db contract-key)
             :smart-contract-status
             [:contract/setter-status-loaded contract-key]
             [:log-error :contract/load-setter-status contract-key]]]}}))

(reg-event-fx
  :contract/setup-setter-status-listener
  interceptors
  (fn [{:keys [db]} [contract-key]]
    {:web3-fx.contract/events
     {:db db
      :db-path [:ethlance-config-events]
      :events [[(get-instance db contract-key)
                (str :on-smart-contract-status-set "-" contract-key)
                :on-smart-contract-status-set {} "latest"
                [:contract.config/on-smart-contract-status-set contract-key]
                [:log-error :contract/setup-setter-status-listener contract-key]]]}}))

(reg-event-fx
  :contract.config/on-smart-contract-status-set
  interceptors
  (fn [{:keys [db]} [contract-key {:keys [:status]}]]
    {:dispatch [:contract/setter-status-loaded contract-key status]}))

(reg-event-fx
  :contract.setter/set-smart-contract-status
  interceptors
  (fn [{:keys [db]} [status address]]
    (let [{:keys [:web3 :active-address]} db
          contract-keys (keys (filter-contract-setters db))]
      {:web3-fx.contract/state-fns
       {:web3 web3
        :db-path [:contract/state-fns]
        :fns (for [contract-key contract-keys]
               [(get-instance db contract-key)
                :set-smart-contract-status
                status
                {:gas u/max-gas-limit
                 :from (or address active-address)}
                [:do-nothing]
                [:log-error :contract/deactivate-all-setters contract-key]
                [:form/submit-receipt u/max-gas-limit {}]])}})))

(reg-event-fx
  :contract.setter/set-smart-contract-status-at-address
  interceptors
  (fn [{:keys [db]} [status contract-address abi]]
    (let [{:keys [:web3 :active-address]} db]
      {:web3-fx.contract/state-fns
       {:web3 web3
        :db-path [:contract/state-fns]
        :fns [[(web3-eth/contract-at (:web3 db) abi contract-address)
               :set-smart-contract-status
               status
               {:gas 500000
                :from active-address}
               [:do-nothing]
               [:log-error :contract.setter/set-smart-contract-status2]
               [:form/submit-receipt u/max-gas-limit {}]]]}})))

(reg-event-fx
  :contract/setter-status-loaded
  interceptors
  (fn [{:keys [db]} [contract-key status]]
    {:db (assoc db :active-setters? (not (pos? (u/big-num->num status))))}))

(reg-event-fx
  :contract.config/setup-listeners
  interceptors
  (fn [{:keys [db]}]
    (when-not (:contracts-not-found? db)
      (let [config-instance (get-instance db :ethlance-config)]
        {:web3-fx.contract/events
         {:db db
          :db-path [:ethlance-config-events]
          :events [#_[config-instance :on-skills-added {} "latest"
                      :contract.config/on-skills-added [:log-error :on-skills-added]]
                   #_[config-instance :on-skills-blocked {} "latest"
                      :contract.config/on-skills-blocked [:log-error :on-skills-blocked]]
                   #_[config-instance :on-skill-name-set {} "latest"
                      :contract.config/on-skill-name-set [:log-error :on-skill-name-set]]
                   [config-instance :on-configs-changed {} "latest"
                    :contract.config/on-configs-changed [:log-error :on-configs-changed]]]}}))))


(reg-event-fx
  :contract.config/on-skills-added
  interceptors
  (fn [{:keys [db]} [{:keys [:skill-ids]}]]
    {:dispatch [:contract.views/skill-count-loaded (apply max (u/big-nums->nums skill-ids))]}))

(reg-event-fx
  :contract.config/on-skills-blocked
  interceptors
  (fn [{:keys [db]} [{:keys [:skill-ids]}]]
    {:db (update db :app/skills #(apply dissoc % (u/big-nums->nums skill-ids)))}))

(reg-event-fx
  :contract.config/on-skill-name-set
  interceptors
  (fn [{:keys [db]} [{:keys [:skill-id :name]}]]
    {:db (assoc-in db [:app/skills (u/big-num->num skill-id) :skill/name] (u/remove-zero-chars (web3/to-ascii name)))}))

(reg-event-fx
  :contract.config/on-configs-changed
  interceptors
  (fn [{:keys [db]} [args]]
    (when (seq (:keys args))
      {:dispatch [:contract.config/get-configs
                  {:config/keys (map (comp keyword u/remove-zero-chars web3/to-ascii) (:keys args))}]})))

(reg-event-fx
  :contract.config/set-skill-name
  interceptors
  (fn [{:keys [db]} [form-data address]]
    {:dispatch [:form/submit
                {:form-data form-data
                 :address address
                 :fn-key :ethlance-config/set-skill-name
                 :form-key :form.config/set-skill-name}]}))

(reg-event-fx
  :contract.config/block-skills
  interceptors
  (fn [{:keys [db]} [form-data address]]
    {:dispatch [:form/submit
                {:form-data form-data
                 :address address
                 :fn-key :ethlance-config/block-skills
                 :form-key :form.config/block-skills}]}))

(reg-event-fx
  :contract.config/set-default-configs
  interceptors
  (fn [{:keys [db]} [address-index]]
    (let [config (:eth/config default-db)]
      {:dispatch [:contract.config/set-configs
                  {:config/keys (keys config)
                   :config/values (vals config)}
                  (when address-index
                    (nth (:my-addresses db) address-index))]})))

(reg-event-fx
  :contract.config/set-configs
  interceptors
  (fn [{:keys [db]} [form-data address]]
    {:dispatch [:form/submit
                {:form-data form-data
                 :address address
                 :fn-key :ethlance-config/set-configs
                 :form-key :form.config/set-configs
                 :receipt-dispatch-n [(if generate-mode? [:generate-db] [:do-nothing])]}]}))

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
               [:log-error :contract.config/get-configs values]]]}})))

(reg-event-fx
  :contract.config/get-configs-loaded
  interceptors
  (fn [{:keys [db]} [config-keys config-values]]
    (if (seq config-values)
      {:db (update db :eth/config merge (zipmap config-keys (u/big-nums->nums config-values)))}
      {:db (assoc db :contracts-not-found? true)})))

(reg-event-fx
  :contract.config/owner-add-skills
  interceptors
  (fn [{:keys [db]} [form-data address]]
    {:db (assoc-in db [:form.config/add-skills :gas-limit] u/max-gas-limit)
     :dispatch [:contract.config/add-skills]}))

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
                                      [:contract.views/load-skill-count]
                                      [:form/set-value :form.config/add-skills :skill/names [] false]]}]}))

(reg-event-fx
  :contract.db/add-allowed-contracts
  interceptors
  (fn [{:keys [db]} [contract-keys address-index]]
    (let [contract-keys (if-not contract-keys
                          (keys (filter-contract-setters db))
                          contract-keys)]
      (let [{:keys [:web3 :active-address :eth/contracts]} db]
        {:web3-fx.contract/state-fns
         {:web3 web3
          :db-path [:contract/state-fns]
          :fns [[(get-instance db :ethlance-db)
                 :add-allowed-contracts
                 (map :address (vals (select-keys contracts contract-keys)))
                 {:gas 500000
                  :from (if address-index
                          (nth (:my-addresses db) address-index)
                          active-address)}
                 :contract/transaction-sent
                 [:contract/transaction-error :contract.db/add-allowed-contracts]
                 (if (contains? (set contract-keys) :ethlance-config)
                   [:contract.config/set-default-configs address-index]
                   [:do-nothing])]]}}))))

(defn clear-invalid-country-state [form-data]
  (cond-> form-data
    (not (u/united-states? (:user/country form-data)))
    (assoc :user/state 0)))

(reg-event-fx
  :contract.user/register-freelancer
  interceptors
  (fn [{:keys [db]} [form-data address]]
    (let [address (or address (:active-address db))]
      {:dispatch [:form/submit
                  {:form-data (clear-invalid-country-state form-data)
                   :address address
                   :fn-key :ethlance-user/register-freelancer
                   :form-key :form.user/register-freelancer
                   :receipt-dispatch-n [[:snackbar/show-message "Your freelancer profile was successfully created"]
                                        [:contract.db/load-users #{:user/email} [address]]
                                        [:location/set-hash :freelancer/detail {:user/id address}]]}]})))

(reg-event-fx
  :contract.user/register-employer
  interceptors
  (fn [{:keys [db]} [form-data address]]
    (let [address (or address (:active-address db))]
      {:dispatch [:form/submit
                  {:form-data (clear-invalid-country-state form-data)
                   :address address
                   :fn-key :ethlance-user/register-employer
                   :form-key :form.user/register-employer
                   :receipt-dispatch-n [[:snackbar/show-message "Your employer profile was successfully created"]
                                        [:contract.db/load-users #{:user/email} [address]]
                                        [:location/set-hash :employer/detail {:user/id address}]]}]})))

(reg-event-fx
  :contract.user/set-user
  interceptors
  (fn [{:keys [db]} [form-data address]]
    {:dispatch [:form/submit
                {:form-data (clear-invalid-country-state form-data)
                 :address address
                 :fn-key :ethlance-user/set-user
                 :form-key :form.user/set-user
                 :receipt-dispatch-n [[:snackbar/show-message "Your user profile was successfully updated"]
                                      [:contract.db/load-users
                                       (set/union ethlance-db/user-entity-fields #{:user/email})
                                       [(:user/id (get-active-user db))]]]}]}))

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
                                       (set/union ethlance-db/freelancer-entity-fields #{:user/freelancer?})
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
                                       (set/union ethlance-db/employer-entity-fields
                                                  #{:user/employer?})
                                       [(:user/id (get-active-user db))]]]}]}))

(reg-event-fx
  :contract.user2/set-user-notifications
  interceptors
  (fn [{:keys [db]} [form-data address]]
    {:dispatch [:form/submit
                {:form-data form-data
                 :address address
                 :fn-key :ethlance-user2/set-user-notifications
                 :form-key :form.user2/set-user-notifications
                 :receipt-dispatch-n [[:snackbar/show-message "Your notification settings were successfully updated"]
                                      [:contract.db/load-users
                                       ethlance-db/user-notifications-fields
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
    (if (:my-users-loaded? db)
      {:dispatch load-dispatch}
      {:async-flow {:first-dispatch [:do-nothing]
                    :rules [{:when :seen?
                             :events [:my-users-loaded]
                             :dispatch (conj [:after-my-users-loaded] load-dispatch)
                             :halt? true}]}})))

;; ============jobs


(reg-event-fx
  :contract.search/search-jobs-deb
  interceptors
  (fn [{:keys [db]} args]
    {:dispatch-debounce {:key :contract.search/search-jobs
                         :event (into [:contract.search/search-jobs] args)
                         :delay 300}}))

(reg-event-fx
  :contract.search/search-jobs
  interceptors
  (fn [{:keys [db]} [args]]
    (let [{:keys [:conversion-rates]} db
          {:keys [:search/min-budget :search/min-budget-currency]} args
          min-budgets (u/value-in-all-currencies (u/parse-float min-budget) min-budget-currency conversion-rates)]
      {:dispatch [:list/load-ids {:list-key :list/search-jobs
                                  :fn-key :ethlance-search-jobs/search-jobs
                                  :load-dispatch-key :contract.db/load-jobs
                                  :fields ethlance-db/search-jobs-fields
                                  :args (assoc args :search/min-budgets min-budgets)
                                  :keep-items? true}]})))

(reg-event-fx
  :contract.views/get-sponsorable-jobs
  interceptors
  (fn [{:keys [db]} [args]]
    {:dispatch [:list/load-ids {:list-key :list/sponsorable-jobs
                                :fn-key :ethlance-views/get-sponsorable-jobs
                                :load-dispatch-key :contract.db/load-jobs
                                :fields ethlance-db/get-sponsorable-jobs-fields
                                :args args
                                :keep-items? true}]}))

(reg-event-fx
  :contract.db/load-jobs
  interceptors
  (fn [{:keys [db]} [all-fields job-ids]]
    (let [{:keys [fields ids]} (filter-needed-fields
                                 (u/filter-by-namespaces
                                   (u/distinct-namespaces ethlance-db/job-entity-fields)
                                   all-fields)
                                 (:app/jobs db)
                                 job-ids
                                 ethlance-db/job-editable-fields)]
      {:ethlance-db/entities
       {:instance (get-instance db :ethlance-db)
        :ids ids
        :fields fields
        :partitions 5
        :on-success [:contract/jobs-loaded all-fields]
        :on-error [:log-error :contract.db/load-jobs]}})))

(reg-event-fx
  :contract/jobs-loaded
  interceptors
  (fn [{:keys [db]} [fields jobs]]
    (let [jobs (->> jobs
                 (medley/remove-keys (complement pos?))
                 (u/assoc-key-as-value :job/id))
          new-db (update db :app/jobs (partial merge-with merge) jobs)]
      {:db new-db
       :dispatch-n (remove nil?
                           [(when (contains? fields :job/skills)
                              [:contract.db/load-job-skills jobs])
                            (when (contains? fields :job/allowed-users)
                              [:contract.db/load-job-allowed-users jobs])
                            (when (contains? fields :job/employer)
                              [:contract.db/load-users fields (->> (select-db-jobs new-db jobs)
                                                                vals
                                                                (map :job/employer))])])})))

;; ============users

(reg-event-fx
  :contract.search/search-freelancers-deb
  interceptors
  (fn [{:keys [db]} args]
    {:dispatch-debounce {:key :contract.search/search-freelancers
                         :event (into [:contract.search/search-freelancers] args)
                         :delay 300}}))

(reg-event-fx
  :contract.search/search-freelancers
  interceptors
  (fn [{:keys [db]} [args]]
    (let [{:keys [:conversion-rates]} db
          {:keys [:search/min-hourly-rate :search/max-hourly-rate :search/hourly-rate-currency]} args
          min-hourly-rates (u/value-in-all-currencies (u/parse-float min-hourly-rate)
                                                      hourly-rate-currency
                                                      conversion-rates)
          max-hourly-rates (u/value-in-all-currencies (u/parse-float max-hourly-rate)
                                                      hourly-rate-currency
                                                      conversion-rates)]
      {:dispatch [:list/load-ids {:list-key :list/search-freelancers
                                  :fn-key :ethlance-search-freelancers/search-freelancers
                                  :load-dispatch-key :contract.db/load-users
                                  :fields ethlance-db/search-freelancers-fields
                                  :args (merge args {:search/seed (calculate-search-seed args (:on-load-seed db))
                                                     :search/min-hourly-rates min-hourly-rates
                                                     :search/max-hourly-rates max-hourly-rates})
                                  :keep-items? true}]})))

(reg-event-fx
  :contract.views/load-my-users
  interceptors
  (fn [{:keys [db]} [addresses]]
    (let [addrs (or addresses (:my-addresses db))]
      (if-not (:contracts-not-found? db)
        {:dispatch [:contract.db/load-users
                    (-> ethlance-db/account-entitiy-fields
                      (set/difference #{:freelancer/description :employer/description
                                        :user/github :user/linkedin})
                      (set/union #{:user/email}))
                    addrs
                    {:dispatch-after-loaded [:my-users-loaded]}]}))))

(reg-event-fx
  :my-users-loaded
  interceptors
  (fn [{:keys [db]}]
    {:db (assoc db :my-users-loaded? true)}))

(reg-event-fx
  :contract.db/load-users
  interceptors
  (fn [{:keys [db]} [all-fields user-ids load-dispatch-opts]]
    (let [{:keys [fields ids]} (filter-needed-fields (u/filter-by-namespaces
                                                       (u/distinct-namespaces ethlance-db/account-entitiy-fields)
                                                       all-fields)
                                                     (:app/users db)
                                                     user-ids
                                                     ethlance-db/user-editable-fields)]
      (merge
        {:ethlance-db/entities
         {:instance (get-instance db :ethlance-db)
          :ids ids
          :fields (set/difference fields #{:user/balance})
          :partitions 5
          :on-success [:contract/users-loaded all-fields load-dispatch-opts]
          :on-error [:log-error :contract.db/load-users]}}
        (when (contains? (set all-fields) :user/balance)
          {:dispatch [:blockchain/load-user-balances user-ids]})))))

(reg-event-fx
  :contract/users-loaded
  interceptors
  (fn [{:keys [db]} [fields load-dispatch-opts users]]
    (let [users (->> users
                  (medley/remove-keys (complement pos?))
                  (u/assoc-key-as-value :user/id))]
      {:db (-> db
             (update :app/users (partial merge-with merge) users))
       :dispatch-n (concat
                     (when (contains? (set fields) :freelancer/skills)
                       [[:contract.db/load-freelancer-skills users]])
                     (when-let [dispatch-after-loaded (:dispatch-after-loaded load-dispatch-opts)]
                       [dispatch-after-loaded]))})))

(reg-event-fx
  :contract.db/load-job-skills
  interceptors
  (fn [{:keys [db]} [jobs]]
    {:ethlance-db/entities-field-items {:instance (get-instance db :ethlance-db)
                                        :items (select-db-jobs db jobs)
                                        :count-key :job/skills-count
                                        :field-key :job/skills
                                        :on-success [:contract/field-items-loaded :app/jobs]
                                        :on-error [:log-error :contract.db/load-job-skills]}}))

(reg-event-fx
  :contract.db/load-job-allowed-users
  interceptors
  (fn [{:keys [db]} [jobs]]
    {:ethlance-db/entities-field-items {:instance (get-instance db :ethlance-db)
                                        :items (select-db-jobs db jobs)
                                        :count-key :job/allowed-users-count
                                        :field-key :job/allowed-users
                                        :on-success [:contract/field-items-loaded :app/jobs]
                                        :on-error [:log-error :contract.db/load-job-allowed-users]}}))

(reg-event-fx
  :contract.db/load-freelancer-skills
  interceptors
  (fn [{:keys [db]} [users]]
    {:ethlance-db/entities-field-items {:instance (get-instance db :ethlance-db)
                                        :items (select-db-users db users)
                                        :count-key :freelancer/skills-count
                                        :field-key :freelancer/skills
                                        :on-success [:contract/field-items-loaded :app/users]
                                        :on-error [:log-error :contract.db/load-freelancer-skills]}}))

(reg-event-fx
  :contract.db/load-freelancer-categories
  interceptors
  (fn [{:keys [db]} [users]]
    {:ethlance-db/entities-field-items {:instance (get-instance db :ethlance-db)
                                        :items (select-db-users db users)
                                        :count-key :freelancer/categories-count
                                        :field-key :freelancer/categories
                                        :on-success [:contract/field-items-loaded :app/users]
                                        :on-error [:log-error :contract.db/load-freelancer-categories]}}))

(reg-event-fx
  :contract.db/load-user-languages
  interceptors
  (fn [{:keys [db]} [users]]
    {:ethlance-db/entities-field-items {:instance (get-instance db :ethlance-db)
                                        :items (select-db-users db users)
                                        :count-key :user/languages-count
                                        :field-key :user/languages
                                        :on-success [:contract/field-items-loaded :app/users]
                                        :on-error [:log-error :contract.db/load-user-languages]}}))

(reg-event-fx
  :blockchain/load-user-balances
  interceptors
  (fn [{:keys [db]} [user-ids]]
    {:web3-fx.blockchain/balances {:web3 (:web3 db)
                                   :blockchain-filter-opts "latest"
                                   :addresses (set user-ids)
                                   :dispatches [:blockchain/address-balance-loaded
                                                [:blockchain/on-error :blockchain/load-user-balances]]}}))

(reg-event-fx
  :contract/field-items-loaded
  interceptors
  (fn [{:keys [db]} [items-key loaded-items]]
    {:db (-> db
           (update items-key (partial merge-with merge) loaded-items))}))

(reg-event-fx
  :contract.db/load-user-addresses-by-legacy-ids
  interceptors
  (fn [{:keys [db]} [user-ids]]
    {:ethlance-db/entities
     {:instance (get-instance db :ethlance-db)
      :ids user-ids
      :fields #{:user/address}
      :on-success [:contract/user-addresses-by-legacy-ids-loaded]
      :on-error [:log-error :contract.db/load-user-addresses-by-legacy-ids]}}))

(reg-event-fx
  :contract/user-addresses-by-legacy-ids-loaded
  interceptors
  (fn [{:keys [db]} [users]]
    (let [users (->> users
                  (medley/remove-keys (complement pos?)))]
      {:db (-> db
             (update :legacy-user-ids (partial merge-with merge) users))})))

;;============jobs

(reg-event-fx
  :contract.job/add-job
  interceptors
  (fn [{:keys [db]} [form-data address]]
    {:dispatch [:contract.job/set-job (assoc form-data :job/id 0) :form.job/add-job address]}))

(reg-event-fx
  :contract.job/set-job
  interceptors
  (fn [{:keys [db]} [form-data form-key address]]
    {:dispatch [:form/submit
                {:form-data form-data
                 :address address
                 :fn-key :ethlance-job/set-job
                 :form-key form-key
                 :receipt-dispatch-n [[:snackbar/show-message (str "Job has been successfully "
                                                                   (if (pos? (:job/id form-data))
                                                                     "updated"
                                                                     "created"))]
                                      (if (pos? (:job/id form-data))
                                        [:location/set-hash :job/detail (select-keys form-data [:job/id])]
                                        [:location/set-hash :employer/jobs])]}]}))

(reg-event-fx
  :form.job.add-job/set-budget-enabled?
  interceptors
  (fn [{:keys [db]} [form-key budget-enabled?]]
    {:db (-> db
           (assoc-in [form-key :budget-enabled?] budget-enabled?)
           (assoc-in [form-key :data :job/budget] 0))}))

(reg-event-fx
  :form.job.add-job/add-me-as-allowed
  interceptors
  (fn [{:keys [db]} [form-key allowed-users]]
    {:dispatch [:form/set-value form-key :job/allowed-users (into [] (conj allowed-users (:active-address db)))
                #(<= (count %) (:max-job-allowed-users (:eth/config db)))]}))

(reg-event-fx
  :contract.job/set-hiring-done
  interceptors
  (fn [{:keys [db]} [form-data address]]
    {:dispatch [:form/submit
                {:form-data form-data
                 :address address
                 :fn-key :ethlance-job/set-job-hiring-done
                 :form-key :form.job/set-hiring-done
                 :receipt-dispatch [:contract.db/load-jobs #{:job/status :job/hiring-done-on} [(:job/id form-data)]]}]}))

(reg-event-fx
  :contract.job/approve-sponsorable-job
  interceptors
  (fn [{:keys [db]} [form-data address]]
    {:dispatch [:form/submit
                {:form-data form-data
                 :address address
                 :fn-key :ethlance-job/approve-sponsorable-job
                 :form-key :form.job/approve-sponsorable-job
                 :receipt-dispatch-n [[:contract.db/load-jobs #{:job/status} [(:job/id form-data)]]
                                      [:contract.views/load-job-approvals (select-keys form-data [:job/id])]]}]}))

(reg-event-fx
  :contract.views/load-job-approvals
  interceptors
  (fn [{:keys [db]} [values]]
    {:web3-fx.contract/constant-fns
     {:fns [(concat
              [(get-instance db :ethlance-views)
               :ethlance-views/get-job-approvals]
              (args-map->vec values ethlance-db/get-job-approvals-args)
              [[:contract/job-approvals-loaded values]
               [:log-error :contract.views/load-job-approvals]])]}}))

(reg-event-fx
  :contract/job-approvals-loaded
  interceptors
  (fn [{:keys [db]} [{:keys [:job/id]} [allowed-users approvals]]]
    {:dispatch [:contract/jobs.allowed-users-loaded
                #{:job.allowed-user/approved?}
                (reduce (fn [acc [i allowed-user]]
                          (assoc-in acc [[id allowed-user] :job.allowed-user/approved?] (nth approvals i)))
                        {}
                        (medley/indexed allowed-users))]}))

(reg-event-fx
  :contract/jobs.allowed-users-loaded
  interceptors
  (fn [{:keys [db]} [fields jobs-allowed-users]]
    (let [jobs-allowed-users (->> jobs-allowed-users
                               (medley/remove-vals u/empty-job-allowed-user?))]
      {:db (update db :app/jobs.allowed-users (partial merge-with merge) jobs-allowed-users)})))

;; ============contracts

(reg-event-fx
  :contract.db/load-contracts
  interceptors
  (fn [{:keys [db]} [all-fields contract-ids {:keys [:load-per]}]]
    (let [contract-ids (u/big-nums->nums contract-ids)
          {:keys [fields ids]} (filter-needed-fields (u/filter-by-namespaces
                                                       (u/distinct-namespaces ethlance-db/proposal+invitation-entitiy-fields)
                                                       all-fields)
                                                     (:app/contracts db)
                                                     contract-ids
                                                     ethlance-db/contract-editable-fields)]
      {:ethlance-db/entities
       {:instance (get-instance db :ethlance-db)
        :ids ids
        :fields fields
        :on-success [:contract/contracts-loaded all-fields]
        :on-error [:log-error :contract.db/load-contracts]}})))

(reg-event-fx
  :contract/contracts-loaded
  interceptors
  (fn [{:keys [db]} [fields contracts]]
    (let [contracts (->> contracts
                      (medley/remove-keys (complement pos?))
                      (u/assoc-key-as-value :contract/id))
          new-db (update db :app/contracts (partial merge-with merge) contracts)]
      {:db new-db
       :dispatch-n (remove nil? [(when (contains? fields :contract/freelancer)
                                   [:contract.db/load-users fields (->> (select-db-contracts new-db contracts)
                                                                     vals
                                                                     (map :contract/freelancer))])
                                 (when (contains? fields :contract/job)
                                   [:contract.db/load-jobs fields (->> (select-db-contracts new-db contracts)
                                                                    vals
                                                                    (map :contract/job))])])})))

(reg-event-fx
  :contract.views/load-my-freelancers-contracts-for-job
  interceptors
  (fn [{:keys [db]} [values]]
    (let [user-ids (->> (get-my-users db)
                     (filter :user/freelancer?)
                     keys)]
      (when (seq user-ids)
        {:web3-fx.contract/constant-fns
         {:fns [(concat
                  [(get-instance db :ethlance-views)
                   :ethlance-views/get-freelancers-job-contracts]
                  (args-map->vec (merge {:user/ids user-ids} values) ethlance-db/get-freelancers-job-contracts-args)
                  [[:contract.db/load-contracts
                    #{:contract/job :contract/freelancer :contract/status :contract/freelancer-feedback-on}]
                   [:log-error :contract.views/load-my-freelancers-contracts-for-job]])]}}))))

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
                                      [:contract.views/load-job-proposals (:params (:list/job-proposals db))]
                                      [:contract.db/load-jobs #{:job/contracts-count} [(:contract/job form-data)]]]}]}))

(reg-event-fx
  :contract.views/load-job-proposals
  [interceptors]
  (fn [{:keys [db]} [args]]
    {:dispatch [:list/load-ids {:list-key :list/job-proposals
                                :fn-key :ethlance-views/get-job-contracts
                                :load-dispatch-key :contract.db/load-contracts
                                :fields ethlance-db/job-proposals-list-fields
                                :args args}]}))

(reg-event-fx
  :contract.views/load-employer-jobs-for-freelancer-invite
  [interceptors]
  (fn [{:keys [db]} [args]]
    {:dispatch [:list/load-ids {:list-key :list/employer-jobs-open-select-field
                                :fn-key :ethlance-views/get-employer-jobs-for-freelancer-invite
                                :load-dispatch-key :contract.db/load-jobs
                                :fields #{:job/title}
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
                 :receipt-dispatch [:contract.db/load-contracts #{:contract/description
                                                                  :contract/created-on
                                                                  :contract/status}
                                    [(:contract/id form-data)]]}]}))

(reg-event-fx
  :contract.contract/cancel-contract
  interceptors
  (fn [{:keys [db]} [form-data address]]
    {:dispatch [:form/submit
                {:form-data form-data
                 :address address
                 :fn-key :ethlance-contract/cancel-job-contract
                 :form-key :form.contract/cancel-contract
                 :receipt-dispatch [:contract.db/load-contracts #{:contract/cancel-description
                                                                  :contract/cancelled-on
                                                                  :contract/status}
                                    [(:contract/id form-data)]]}]}))

(reg-event-fx
  :contract.contract/add-feedback
  interceptors
  (fn [{:keys [db]} [form-data address]]
    (let [{:keys [:contract/freelancer]} (get (:app/contracts db) (:contract/id form-data))
          fields (if (= freelancer (:user/id (get-active-user db)))
                   ethlance-db/freelancer-feedback-entity-fields
                   ethlance-db/employer-feedback-entity-fields)]
      {:dispatch [:form/submit
                  {:form-data form-data
                   :address address
                   :fn-key :ethlance-feedback/add-job-contract-feedback
                   :form-key :form.contract/add-feedback
                   :receipt-dispatch [:contract.db/load-contracts
                                      (set/union fields #{:contract/status :contract/done-by-freelancer?})
                                      [(:contract/id form-data)]]}]})))

;; ============invoices

(reg-event-fx
  :contract.db/load-invoices
  interceptors
  (fn [{:keys [db]} [all-fields invoice-ids]]
    (let [invoice-ids (u/big-nums->nums invoice-ids)
          invoice-fields (u/filter-by-namespaces (u/distinct-namespaces ethlance-db/invoice-entity-fields) all-fields)
          {:keys [fields ids]} (filter-needed-fields invoice-fields
                                                     (:app/invoices db)
                                                     invoice-ids
                                                     ethlance-db/invoice-editable-fields)]
      {:ethlance-db/entities
       {:instance (get-instance db :ethlance-db)
        :ids ids
        :fields fields
        :on-success [:contract/invoices-loaded all-fields]
        :on-error [:log-error :contract.db/load-invoices]}})))

(reg-event-fx
  :contract/invoices-loaded
  interceptors
  (fn [{:keys [db]} [fields invoices]]
    (let [invoices (->> invoices
                     (medley/remove-keys (complement pos?))
                     (u/assoc-key-as-value :invoice/id))
          new-db (update db :app/invoices (partial merge-with merge) invoices)]
      (merge
        {:db new-db}
        (when (contains? fields :invoice/contract)
          {:dispatch [:contract.db/load-contracts fields (->> (select-db-invoices new-db invoices)
                                                           vals
                                                           (map :invoice/contract))]})))))

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
                                      [:location/set-hash :freelancer/invoices]
                                      [:contract.db/load-users
                                       #{:freelancer/total-invoiced}
                                       [(:user/id (get-active-user db))]]]}]}))

(reg-event-fx
  :form.invoice/add-invoice-localstorage
  [interceptors (inject-cofx :localstorage)]
  (fn [{:keys [db localstorage]} [contract-id field-key value validator]]
    (let [new-db (assoc-in db [:form.invoice/add-invoice-localstorage contract-id field-key] value)]
      {:db new-db
       :localstorage (update localstorage
                             :form.invoice/add-invoice-localstorage
                             merge
                             (:form.invoice/add-invoice-localstorage new-db))
       :dispatch [:form/set-value :form.invoice/add-invoice field-key value validator]})))

(reg-event-fx
  :contract.invoice/pay-invoice
  interceptors
  (fn [{:keys [db]} [form-data amount address]]
    {:dispatch [:form/submit
                {:form-data form-data
                 :address address
                 :value (web3/to-wei amount :ether)
                 :fn-key :ethlance-invoice/pay-invoice
                 :form-key :form.invoice/pay-invoice
                 :receipt-dispatch [:contract.db/load-invoices #{:invoice/status :invoice/paid-on
                                                                 :invoice/paid-by}
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
                 :receipt-dispatch [:contract.db/load-invoices #{:invoice/status :invoice/cancelled-on}
                                    [(:invoice/id form-data)]]}]}))

;; ============messages

(reg-event-fx
  :contract.db/load-messages
  interceptors
  (fn [{:keys [db]} [all-fields message-ids]]
    (let [message-ids (u/big-nums->nums message-ids)
          {:keys [fields ids]} (filter-needed-fields (u/filter-by-namespaces
                                                       (u/distinct-namespaces ethlance-db/message-entity-fields)
                                                       all-fields)
                                                     (:app/messages db)
                                                     message-ids
                                                     ethlance-db/message-editable-fields)]
      {:ethlance-db/entities
       {:instance (get-instance db :ethlance-db)
        :ids ids
        :fields fields
        :partitions 2
        :on-success [:contract/messages-loaded all-fields]
        :on-error [:log-error :contract.db/load-messages]}})))

(reg-event-fx
  :contract/messages-loaded
  interceptors
  (fn [{:keys [db]} [fields messages]]
    (let [messages (->> messages
                     (medley/remove-keys (complement pos?))
                     (u/assoc-key-as-value :message/id))]
      {:db (update db :app/messages (partial merge-with merge) messages)})))

(reg-event-fx
  :contract.views/load-contract-messages
  interceptors
  (fn [{:keys [db]} [values]]
    {:web3-fx.contract/constant-fns
     {:fns [(concat
              [(get-instance db :ethlance-views)
               :ethlance-views/get-contract-messages]
              (args-map->vec values ethlance-db/get-contract-messages-args)
              [[:contract/contract-messages-loaded values]
               [:log-error :contract.views/load-contract-messages]])]}}))

(reg-event-fx
  :contract.message/add-job-contract-message
  interceptors
  (fn [{:keys [db]} [form-data address]]
    {:dispatch [:form/submit
                {:form-data form-data
                 :address address
                 :fn-key :ethlance-message/add-job-contract-message
                 :form-key :form.message/add-job-contract-message
                 :receipt-dispatch-n [[:contract.views/load-contract-messages (select-keys form-data [:contract/id])]
                                      [:snackbar/show-message "Your message was successfully sent"]
                                      [:form/set-value :form.message/add-job-contract-message :message/text ""]]}]}))

(reg-event-fx
  :contract/contract-messages-loaded
  interceptors
  (fn [{:keys [db]} [{:keys [:contract/id]} message-ids]]
    {:db (update-in db [:app/contracts id] merge {:contract/messages (u/big-nums->nums message-ids)
                                                  :contract/messages-count (count message-ids)})
     :dispatch [:contract.db/load-messages ethlance-db/message-entity-fields message-ids]}))

;; ============sponsorships

(reg-event-fx
  :contract.db/load-sponsorships
  interceptors
  (fn [{:keys [db]} [all-fields sponsorship-ids]]
    (let [sponsorship-ids (u/big-nums->nums sponsorship-ids)
          sponsorship-fields (u/filter-by-namespaces (u/distinct-namespaces ethlance-db/sponsorship-entity-fields)
                                                     all-fields)
          {:keys [fields ids]} (filter-needed-fields sponsorship-fields
                                                     (:app/sponsorships db)
                                                     sponsorship-ids
                                                     ethlance-db/sponsorship-editable-fields)]
      {:ethlance-db/entities
       {:instance (get-instance db :ethlance-db)
        :ids ids
        :fields fields
        :on-success [:contract/sponsorships-loaded all-fields]
        :on-error [:log-error :contract.db/load-sponsorships]}})))

(reg-event-fx
  :contract/sponsorships-loaded
  interceptors
  (fn [{:keys [db]} [fields sponsorships]]
    (let [sponsorships (->> sponsorships
                         (medley/remove-keys (complement pos?))
                         (u/assoc-key-as-value :sponsorship/id))]
      (merge
        {:db (update db :app/sponsorships (partial merge-with merge) sponsorships)}
        (when (contains? fields :sponsorship/job)
          {:dispatch [:contract.db/load-jobs
                      fields
                      (map :sponsorship/job (vals sponsorships))]})))))

(reg-event-fx
  :contract.views/load-job-sponsorships
  [interceptors]
  (fn [{:keys [db]} [args]]
    {:dispatch [:list/load-ids {:list-key :list/job-sponsorships
                                :fn-key :ethlance-views/get-job-sponsorships
                                :load-dispatch-key :contract.db/load-sponsorships
                                :fields ethlance-db/job-sponsorships-table-entity-fields
                                :args args}]}))

(reg-event-fx
  :contract.sponsor/add-job-sponsorship
  interceptors
  (fn [{:keys [db]} [form-data address]]
    {:dispatch [:form/submit
                {:form-data form-data
                 :address address
                 :value (web3/to-wei (:sponsorship/amount form-data) :ether)
                 :fn-key :ethlance-sponsor/add-job-sponsorship
                 :form-key :form.sponsor/add-job-sponsorship
                 :receipt-dispatch-n [[:snackbar/show-message "Your sponsorship was successfully sent!"]
                                      [:contract.views/load-job-sponsorships (:params (:list/job-sponsorships db))]
                                      [:contract.db/load-jobs (set/union ethlance-db/job-sponsorship-stats-fields
                                                                         #{:job/sponsorships-count})
                                       [(:sponsorship/job form-data)]]]}]}))

(reg-event-fx
  :contract.sponsor/refund-job-sponsorships
  interceptors
  (fn [{:keys [db]} [form-data address]]
    {:dispatch [:form/submit
                {:form-data form-data
                 :address address
                 :fn-key :ethlance-sponsor/refund-job-sponsorships
                 :form-key :form.sponsor/refund-job-sponsorships
                 :receipt-dispatch-n [[:snackbar/show-message "Sponsorships were successfully refunded"]
                                      [:contract.views/load-job-sponsorships (:params (:list/job-sponsorships db))]
                                      [:contract.db/load-jobs
                                       (set/union ethlance-db/job-sponsorship-stats-fields
                                                  #{:job/status})
                                       [(:sponsorship/job form-data)]]]}]}))

;; ============misc

(reg-event-fx
  :list/load-ids
  interceptors
  (fn [{:keys [db]} [{:keys [:list-key :fn-key :load-dispatch-key :load-dispatch-opts fields :args :load-per
                             :keep-items?]}]]
    {:db (cond-> db
           (and (not keep-items?) (not= (get-in db [list-key :params]) args))
           (assoc-in [list-key :items] [])

           true (update list-key merge {:loading? true :params args}))
     :web3-fx.contract/constant-fns
     {:fns [(concat
              [(get-instance db (keyword (namespace fn-key)))
               fn-key]
              (args-map->vec args (ethlance-db/eth-contracts-fns fn-key))
              [[:contract.views/ids-loaded list-key load-dispatch-key fields load-per load-dispatch-opts]
               [:log-error :list/load-ids fn-key]])]}}))

(reg-event-fx
  :contract.views/ids-loaded
  interceptors
  (fn [{:keys [db]} [list-key load-dispatch-key fields load-per load-dispatch-opts ids]]
    (let [ids (u/big-nums->nums ids)
          items-list (get db list-key)]
      {:db (update db list-key merge {:items ids :loading? false})
       :dispatch [load-dispatch-key fields (u/sort-paginate-ids items-list ids) load-per load-dispatch-opts]})))

(reg-event-fx
  :contract.views/load-skill-count
  interceptors
  (fn [{:keys [db]}]
    {:web3-fx.contract/constant-fns
     {:fns [[(get-instance db :ethlance-views)
             :ethlance-views/get-skill-count
             :contract.views/skill-count-loaded
             [:blockchain/on-error :contract.views/load-skill-count]]]}}))

(reg-event-fx
  :contract.views/skill-count-loaded
  interceptors
  (fn [{:keys [db]} [new-skill-count]]
    (let [new-skill-count (u/big-num->num new-skill-count)
          old-skill-count (:app/skill-count db)
          skill-load-limit (:skill-load-limit db)]
      (when (< old-skill-count new-skill-count)
        {:db (assoc db :app/skill-count new-skill-count)
         :dispatch-n
         (into []
               (for [x (range (js/Math.ceil (/ (- new-skill-count old-skill-count) skill-load-limit)))]
                 [:contract.views/load-skill-names {:skill/limit skill-load-limit
                                                    :skill/offset (+ old-skill-count (* x skill-load-limit))}]))}))))

(reg-event-fx
  :contract.views/load-skill-names
  interceptors
  (fn [{:keys [db]} [values]]
    {:web3-fx.contract/constant-fns
     {:fns [(concat
              [(get-instance db :ethlance-views)
               :ethlance-views/get-skill-names]
              (args-map->vec values (ethlance-db/eth-contracts-fns :ethlance-views/get-skill-names))
              [:contract.views/skill-names-loaded
               [:log-error :contract.views/load-skill-names values]])]}}))

(reg-event-db
  :contract.views/skill-names-loaded
  interceptors
  (fn [db [[ids names]]]
    (update db :app/skills merge (zipmap (u/big-nums->nums ids)
                                         (map (comp (partial hash-map :skill/name) u/remove-zero-chars web3/to-ascii) names)))))


(reg-event-fx
  :contracts/listen-active-user-events
  [interceptors]
  (fn [{:keys [db]} [{:keys [:user/id]}]]
    (when id
      (let [invoice-instance (get-instance db :ethlance-invoice)
            contract-instance (get-instance db :ethlance-contract)
            message-instance (get-instance db :ethlance-message)
            sponsor-instance (get-instance db :ethlance-sponsor)
            job-instance (get-instance db :ethlance-job)]
        {:web3-fx.contract/events
         {:db db
          :db-path [:active-user-events]
          :events [[contract-instance :on-job-proposal-added {:employer-id id} "latest"
                    :contract.contract/on-job-proposal-added [:log-error :on-job-proposal-added]]
                   [contract-instance :on-job-contract-added {:freelancer-id id} "latest"
                    :contract.contract/on-job-contract-added [:log-error :on-job-contract-added]]
                   [contract-instance :on-job-contract-cancelled {:employer-id id} "latest"
                    :contract.contract/on-job-contract-cancelled [:log-error :on-job-contract-cancelled]]
                   [contract-instance :on-job-contract-feedback-added {:receiver-id id} "latest"
                    :contract.contract/on-job-contract-feedback-added [:log-error :on-job-contract-feedback-added]]
                   [contract-instance :on-job-invitation-added {:freelancer-id id} "latest"
                    :contract.contract/on-job-invitation-added [:log-error :on-job-invitation-added]]
                   [invoice-instance :on-invoice-added {:employer-id id} "latest"
                    :contract.invoice/on-invoice-added [:log-error :on-invoice-added]]
                   [invoice-instance :on-invoice-paid {:freelancer-id id} "latest"
                    :contract.invoice/on-invoice-paid [:log-error :on-invoice-paid]]
                   [invoice-instance :on-invoice-cancelled {:employer-id id} "latest"
                    :contract.invoice/on-invoice-cancelled [:log-error :on-invoice-cancelled]]
                   [message-instance :on-job-contract-message-added {:receiver-id id} "latest"
                    :contract.message/on-job-contract-message-added [:log-error :on-job-contract-message-added]]
                   [sponsor-instance :on-job-sponsorship-added {:employer-id id} "latest"
                    :contract.sponsor/on-job-sponsorship-added [:log-error :contract.sponsor/on-job-sponsorship-added]]
                   [sponsor-instance :on-job-sponsorship-refunded {:receiver-id id} "latest"
                    :contract.sponsor/on-job-sponsorship-refunded [:log-error :contract.sponsor/on-job-sponsorship-refunded]]
                   [job-instance :on-sponsorable-job-approved {:employer-id id} "latest"
                    :contract.job/on-sponsorable-job-approved [:log-error :contract.job/on-sponsorable-job-approved]]]}}))))

(reg-event-fx
  :contract.contract/on-job-proposal-added
  [interceptors]
  (fn [{:keys [db]} [{:keys [:contract-id :job-id]}]]
    (let [contract-id (u/big-num->num contract-id)
          job-id (u/big-num->num job-id)]
      (merge
        {:dispatch [:snackbar/show-message-redirect-action
                    "Your job just received a proposal!" :contract/detail {:contract/id contract-id}]}
        (when (active-page-this-job-detail? db job-id)
          {:dispatch-n [[:contract.db/load-jobs #{:job/contracts-count} [job-id]]]})
        (when (active-page-this-contract-detail? db contract-id)
          {:dispatch-n [[:contract.db/load-contracts
                         (set/union ethlance-db/job-proposals-list-fields)
                         [contract-id]]]})))))

(reg-event-fx
  :contract.contract/on-job-contract-added
  [interceptors]
  (fn [{:keys [db]} [{:keys [:contract-id]}]]
    (let [contract-id (u/big-num->num contract-id)]
      (merge
        {:dispatch [:snackbar/show-message-redirect-action
                    "Your job proposal was accepted!" :contract/detail {:contract/id contract-id}]}
        (when (active-page-this-contract-detail? db contract-id)
          {:dispatch-n [[:contract.db/load-contracts
                         #{:contract/status :contract/created-on :contract/description}
                         [contract-id]]]})))))

(reg-event-fx
  :contract.contract/on-job-contract-cancelled
  [interceptors]
  (fn [{:keys [db]} [{:keys [:contract-id]}]]
    (let [contract-id (u/big-num->num contract-id)]
      (merge
        {:dispatch [:snackbar/show-message-redirect-action
                    "A freelancer just cancelled your contract" :contract/detail {:contract/id contract-id}]}
        (when (active-page-this-contract-detail? db contract-id)
          {:dispatch-n [[:contract.db/load-contracts
                         #{:contract/status :contract/cancelled-on :contract/cancel-description}
                         [contract-id]]]})))))

(reg-event-fx
  :contract.contract/on-job-contract-feedback-added
  [interceptors]
  (fn [{:keys [db]} [{:keys [:contract-id :is-sender-freelancer]}]]
    (let [contract-id (u/big-num->num contract-id)]
      (merge
        {:dispatch [:snackbar/show-message-redirect-action
                    "You just received feedback!" :contract/detail {:contract/id contract-id}]}
        (when (active-page-this-contract-detail? db contract-id)
          {:dispatch-n [[:contract.db/load-contracts
                         (set/union
                           #{:contract/done-by-freelancer? :contract/done-on}
                           (if is-sender-freelancer
                             ethlance-db/freelancer-feedback-entity-fields
                             ethlance-db/employer-feedback-entity-fields))
                         [contract-id]]]})))))

(reg-event-fx
  :contract.contract/on-job-invitation-added
  [interceptors]
  (fn [{:keys [db]} [{:keys [:contract-id]}]]
    {:dispatch [:snackbar/show-message-redirect-action
                "You just received job invitation!" :contract/detail {:contract/id (u/big-num->num contract-id)}]}))

(reg-event-fx
  :contract.invoice/on-invoice-added
  [interceptors]
  (fn [{:keys [db]} [{:keys [:invoice-id]}]]
    {:dispatch [:snackbar/show-message-redirect-action
                "You just received invoice to pay!" :invoice/detail {:invoice/id (u/big-num->num invoice-id)}]}))

(reg-event-fx
  :contract.invoice/on-invoice-paid
  [interceptors]
  (fn [{:keys [db]} [{:keys [:invoice-id]}]]
    (let [invoice-id (u/big-num->num invoice-id)]
      (merge
        {:dispatch [:snackbar/show-message-redirect-action
                    "Your employer just paid your invoice!" :invoice/detail {:invoice/id invoice-id}]}
        (when (active-page-this-invoice-detail? db invoice-id)
          {:dispatch-n [[:contract.db/load-invoices #{:invoice/status :invoice/paid-on} [invoice-id]]]})))))

(reg-event-fx
  :contract.invoice/on-invoice-cancelled
  [interceptors]
  (fn [{:keys [db]} [{:keys [:invoice-id]}]]
    (let [invoice-id (u/big-num->num invoice-id)]
      (merge
        {:dispatch [:snackbar/show-message-redirect-action
                    "Your received invoice was just cancelled!" :invoice/detail {:invoice/id invoice-id}]}
        (when (active-page-this-invoice-detail? db invoice-id)
          {:dispatch-n [[:contract.db/load-invoices #{:invoice/status :invoice/cancelled-on} [invoice-id]]]})))))

(reg-event-fx
  :contract.message/on-job-contract-message-added
  [interceptors]
  (fn [{:keys [db]} [{:keys [:message-id :contract-id]}]]
    (let [contract-id (u/big-num->num contract-id)]
      (merge
        {:dispatch [:snackbar/show-message-redirect-action
                    "You just received message!" :contract/detail {:contract/id contract-id}]}
        (when (active-page-this-contract-detail? db contract-id)
          {:dispatch-n [[:contract.views/load-contract-messages {:contract/id contract-id}]]})))))

(reg-event-fx
  :contract.sponsor/on-job-sponsorship-added
  [interceptors]
  (fn [{:keys [db]} [{:keys [:job-id :amount]}]]
    (let [job-id (u/big-num->num job-id)
          amount (u/format-currency (web3/from-wei amount :ether) 0)]
      (merge
        {:dispatch [:snackbar/show-message-redirect-action
                    (str "Your job received sponsorship " amount)
                    :job/detail {:job/id job-id}]}
        (when (active-page-this-job-detail? db job-id)
          {:dispatch-n [[:contract.views/load-job-sponsorships {:job/id job-id}]
                        [:contract.db/load-jobs (set/union ethlance-db/job-sponsorship-stats-fields
                                                           #{:job/sponsorships-count})
                         [job-id]]]})))))

(reg-event-fx
  :contract.sponsor/on-job-sponsorship-refunded
  [interceptors]
  (fn [{:keys [db]} [{:keys [:job-id :amount]}]]
    (let [job-id (u/big-num->num job-id)
          amount (u/format-currency (web3/from-wei amount :ether) 0)]
      (merge
        {:dispatch [:snackbar/show-message-redirect-action
                    (str "Your sponsorship was refunded with " amount)
                    :job/detail {:job/id job-id}]}
        (when (active-page-this-job-detail? db job-id)
          {:dispatch-n [[:contract.views/load-job-sponsorships {:job/id job-id}]
                        [:contract.db/load-jobs
                         (set/union ethlance-db/job-sponsorship-stats-fields #{:job/status})
                         [job-id]]]})))))

(reg-event-fx
  :contract.job/on-sponsorable-job-approved
  [interceptors]
  (fn [{:keys [db]} [{:keys [:job-id]}]]
    (let [job-id (u/big-num->num job-id)]
      (merge
        {:dispatch [:snackbar/show-message-redirect-action "Your job just got approval" :job/detail {:job/id job-id}]}
        (when (active-page-this-job-detail? db job-id)
          {:dispatch-n [[:contract.views/load-job-approvals {:job/id job-id}]
                        [:contract.db/load-jobs #{:job/status} [job-id]]]})))))

(reg-event-fx
  :contract/call
  interceptors
  (fn [{:keys [db]} [contract-key & args]]
    {:web3-fx.contract/constant-fns
     {:fns [(concat [(get-instance db contract-key)] args [:log :log-error])]}}))

(reg-event-fx
  :contract/state-call
  interceptors
  (fn [{:keys [db]} [{:keys [:contract-key :contract-method :args :transaction-opts]}]]
    (let [transaction-opts (merge {:gas u/max-gas-limit
                                   :from (:active-address db)}
                                  transaction-opts)]
      {:web3-fx.contract/state-fns
       {:web3 (:web3 db)
        :db-path [:contract/state-fns]
        :fns [(concat [(get-instance db contract-key)
                       contract-method]
                      args
                      [transaction-opts
                       :contract/transaction-sent
                       [:contract/transaction-error :contract/state-call]
                       [:contract/transaction-receipt contract-method (:gas transaction-opts) nil nil]])]}})))



(reg-event-fx
  :toggle-search-skills-input
  interceptors
  (fn [{:keys [db]} [open-key form-key open?]]
    (merge
      {:db (assoc db open-key open?)}
      (when (and (not open?)
                 (seq (get-in db [form-key :search/skills-or])))
        {:dispatch [:form.search/set-value :search/skills-or []]}))))

(reg-event-fx
  :form.search/set-value
  interceptors
  (fn [{:keys [db]} [field-key field-value validator]]
    (when (or (not validator)
              (validator field-value))
      {:location/add-to-query [(merge {field-key field-value}
                                      (when (and (= field-key :search/country)
                                                 (not (u/united-states? field-value)))
                                        {:search/state 0})
                                      (when-not (= field-key :search/offset)
                                        {:search/offset 0}))]
       :ga/event ["form.search/set-value" (name field-key) (str field-value)]})))

(reg-event-fx
  :form/submit
  interceptors
  (fn [{:keys [db]} [{:keys [:form-key :fn-key :form-data :value :address] :as props}]]
    (let [form (get db form-key)
          {:keys [:web3 :active-address]} db
          {:keys [:gas-limit]} form
          gas (min (+ gas-limit (ethlance-db/estimate-form-data-gas form-data))
                   (get-max-gas-limit db))]
      {:web3-fx.contract/state-fns
       {:web3 web3
        :db-path [:contract/state-fns]
        :fns [(concat
                [(get-instance db (keyword (namespace fn-key)))
                 fn-key]
                (args-map->vec form-data (ethlance-db/eth-contracts-fns fn-key))
                [(merge
                   {:gas gas
                    :from (or address active-address)
                    ;:gas-price 5000000000
                    }
                   (when value
                     {:value value}))
                 [:form/start-loading form-key]
                 [:contract/transaction-error :form/submit fn-key form-data value address]
                 [:form/submit-receipt gas props]])]}})))

#_(defn get-method-name [this method-name]
    (aget this (if (string/includes? method-name "-")
                 (cs/->camelCase method-name)
                 method-name)))

#_(reg-event-fx
    :form/estimate-gas                                      ;; This is bs
    interceptors
    (fn [{:keys [db]} [form-key fn-key]]
      (let [form (get db form-key)
            {:keys [:web3]} db]
        (web3-utils/js-apply
          (get-method-name (get-contract-class db (keyword (namespace fn-key)))
                           (name fn-key))
          :get-data
          (args-map->vec (:data form) (ethlance-db/eth-contracts-fns fn-key)))
        nil)))

(reg-event-fx
  :form/submit-receipt
  [interceptors log-used-gas]
  (fn [{:keys [db]} [{:keys [:receipt-dispatch :receipt-dispatch-n :form-data :form-key]} {:keys [success?]}]]
    (merge
      (when form-key
        {:db (assoc-in db [form-key :loading?] false)})
      (when (and success? receipt-dispatch)
        {:dispatch (conj receipt-dispatch form-data)})
      (when (and success? receipt-dispatch-n)
        {:dispatch-n (map #(conj % form-data) receipt-dispatch-n)})
      (when-not success?
        {:dispatch [:snackbar/show-error]}))))

(reg-event-fx
  :form.user/set-email
  interceptors
  (fn [{:keys [db]} [form-key value]]
    {:dispatch-n (remove nil?
                         [[:form/set-value form-key :user/email value u/empty-or-valid-email?]
                          (when (u/empty-or-valid-email? value)
                            [:form/set-value form-key :user/gravatar (when (seq value)
                                                                       (u/md5 (string/trim value)))])])}))

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
  (fn [db [form-key errors]]
    (cond-> db
      true (assoc-in [form-key :data] {})
      errors (assoc-in [form-key :errors] errors))))

(reg-event-fx
  :form/add-value
  interceptors
  (fn [{:keys [:db]} [form-key field-key value & [validator]]]
    (let [existing-values (get-in db [form-key :data field-key])]
      {:dispatch [:form/set-value form-key field-key (into [] (conj existing-values value)) validator]})))

(reg-event-fx
  :form/remove-value
  interceptors
  (fn [{:keys [:db]} [form-key field-key value & [validator]]]
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

(reg-event-fx
  :snackbar/show-error
  interceptors
  (fn [{:keys [db]} [error-text]]
    (when-not generate-mode?
      {:db (update db :snackbar merge
                   {:open? true
                    :message (or error-text "Oops, we got an error while saving to blockchain")
                    :action nil
                    :on-action-touch-tap nil})})))

(reg-event-fx
  :snackbar/show-message
  interceptors
  (fn [{:keys [db]} [message]]
    (when-not generate-mode?
      {:db (update db :snackbar merge
                   {:open? true
                    :message message
                    :action nil
                    :on-action-touch-tap nil})})))

(reg-event-fx
  :snackbar/show-message-redirect-action
  interceptors
  (fn [{:keys [db]} [message route route-params]]
    (when-not generate-mode?
      {:db (update db :snackbar merge
                   {:open? true
                    :message message
                    :action "SHOW ME"
                    :on-action-touch-tap #(dispatch [:location/set-hash route route-params])})})))

(reg-event-db
  :snackbar/close
  interceptors
  (fn [db _]
    (assoc-in db [:snackbar :open?] false)))

(reg-event-fx
  :dialog/open-confirmation
  interceptors
  (fn [{:keys [db]} [{:keys [:on-confirm] :as dialog}]]
    {:db (update db :dialog merge
                 (merge {:open? true
                         :message dialog
                         :actions (confirm-dialog/create-confirm-dialog-action-buttons
                                    {:confirm-button-props {:on-confirm on-confirm}})}
                        (dissoc dialog :on-confirm)))}))

(reg-event-db
  :dialog/close
  interceptors
  (fn [db _]
    (assoc-in db [:dialog :open?] false)))

(reg-event-db
  :search-filter.freelancers/set-open?
  interceptors
  (fn [db [open?]]
    (assoc db :search-freelancers-filter-open? open?)))

(reg-event-db
  :search-filter.jobs/set-open?
  interceptors
  (fn [db [open?]]
    (assoc db :search-jobs-filter-open? open?)))

(reg-event-fx
  :contract/transaction-sent
  interceptors
  (fn [_ [tx-hash]]
    #_(console :log tx-hash)))

(reg-event-fx
  :contract/transaction-error
  interceptors
  (fn [_ errors]
    (apply console :error "transaction-error" errors)
    {:ga/event ["transaction-error" (name (first errors)) (str (rest errors))]}))

(reg-event-fx
  :blockchain/on-error
  interceptors
  (fn [{:keys [:db]} errors]
    (apply console :error "blockchain-error" errors)
    {:db (assoc db :blockchain/connection-error? true)
     :ga/event ["blockchain-error" (name (first errors)) (str (rest errors))]
     :dispatch [:snackbar/show-error "Oops, looks like we have trouble connecting into the Ethereum blockchain"]}))

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
  :reinitialize
  interceptors
  (fn [{:keys [:db]} args]
    (let [{:keys [:contract-keys :address-index]} (s/conform (s/cat :contract-keys (s/? sequential?)
                                                                    :address-index (s/? number?))
                                                             args)]
      (.clear js/console)
      {:db (update db :eth/contracts (partial medley/map-kv
                                              (fn [contract-key contract]
                                                (if (or (not contract-keys)
                                                        (contains? (set contract-keys) contract-key))
                                                  [contract-key (dissoc contract :abi :bin :address :instance)]
                                                  [contract-key contract]))))
       :async-flow {:first-dispatch [:load-eth-contracts]
                    :rules [{:when :seen?
                             :events [:eth-contracts-loaded]
                             :dispatch-n [(if contract-keys
                                            [:contracts/deploy contract-keys address-index]
                                            [:contracts/deploy-all address-index])]
                             :halt? true}]}})))



(reg-event-db
  :print-db
  interceptors
  (fn [db]
    (print.foo/look db)
    db))

(reg-event-fx
  :print-contract-addresses
  interceptors
  (fn [{:keys [db]}]
    (doseq [[key {:keys [:address]}] (:eth/contracts db)]
      (println key address))
    nil))

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
    (apply console :log (u/big-nums->nums (if (and (not (string? result)) (some sequential? result))
                                            (map u/big-nums->nums result)
                                            result)))))

(reg-event-fx
  :do-nothing
  interceptors
  (fn [db result]
    ))

(reg-event-fx
  :log-error
  interceptors
  (fn [{:keys [:db]} errors]
    (apply console :error errors)
    {:db db
     :ga/event ["error" (first errors) (str (rest errors))]}))


(reg-event-fx
  :blockchain/unlock-account
  interceptors
  (fn [{:keys [db]} [address password]]
    {:web3-fx.blockchain/fns
     {:web3 (:web3 db)
      :fns [[web3-personal/unlock-account address password 999999
             :blockchain/account-unlocked
             [:log-error :blockchain/unlock-account]]]}}))

(reg-event-fx
  :blockchain/new-account
  interceptors
  (fn [{:keys [db]} [password]]
    {:web3-fx.blockchain/fns
     {:web3 (:web3 db)
      :fns [[web3-personal/new-account password
             :blockchain/new-account-created
             [:log-error :blockchain/new-account]]]}}))

(reg-event-fx
  :blockchain/new-account-created
  interceptors
  (fn [{:keys [db]} args]
    (console :log "Account was created." args)
    {}))

(reg-event-fx
  :blockchain/account-unlocked
  interceptors
  (fn [{:keys [db]}]
    (console :log "Account was unlocked.")
    {}))

(reg-event-fx
  :print-accounts
  interceptors
  (fn [{:keys [db]}]
    {:web3-fx.blockchain/fns
     {:web3 (:web3 db)
      :fns [[web3-eth/accounts :log [:blockchain/on-error :print-accounts]]]}}))


(comment
  (dispatch [:initialize])
  (dispatch [:print-db])
  (dispatch [:contracts/deploy-all])
  (dispatch [:estimate-contracts])
  (dispatch [:clean-localstorage true])
  (dispatch [:print-localstorage])
  (dispatch [:contract.config/set-configs {:config/keys [:max-freelancer-skills]
                                           :config/values [11]}])
  (dispatch [:contract.config/block-skills {:skill/ids [2 4]}])

  (dispatch [:contract.config/set-skill-name {:skill/id 4 :skill/name "abc"}])
  (dispatch [:contract.db/add-allowed-contracts])
  (dispatch [:contract/call :ethlance-db :get-allowed-contracts])
  (dispatch [:contract/call :ethlance-db :allowed-contracts-keys 5])
  (dispatch [:contract/call :ethlance-user :get-config "max-user-languages"])
  (dispatch [:contract/call :ethlance-db :get-u-int-value (u/sha3 :user/count)])
  (dispatch [:contract/call :ethlance-db :get-u-int-value (u/sha3 :message/count)])
  (dispatch [:contract/call :ethlance-db :get-u-int-value (u/sha3 :contract/count)])
  (dispatch [:contract/call :ethlance-db :get-u-int-value (u/sha3 :category/jobs-count 1)])
  (dispatch [:contract/call :ethlance-db :get-u-int-value (u/sha3 :skill/jobs-count 5)])
  (dispatch [:contract/call :ethlance-db :get-address-value (u/sha3 :user/address 1)])
  (dispatch [:contract/call :ethlance-db :get-bytes32-value (u/sha3 :user/name 1)])
  (dispatch [:contract/call :ethlance-db :get-u-int-value (u/sha3 :freelancer/hourly-rate 1)])
  (dispatch [:contract/call :ethlance-db :get-string-value (u/sha3 :user/name 58)])
  (dispatch [:contract/call :ethlance-db :get-u-int-value (u/sha3 :contract/freelancer+job 1 1)])

  (dispatch [:contract/call :ethlance-sponsor-wallet :ethlance-invoice])
  (dispatch [:contract/call :ethlance-sponsor-wallet :ethlance-sponsor])
  (dispatch [:contract/call :ethlance-sponsor :ethlance-sponsor-wallet])
  (dispatch [:contract/call :ethlance-invoice :ethlance-sponsor-wallet])
  (dispatch [:print-contract-addresses])

  (dispatch [:contract/call :ethlance-user :diff #{10 11 12} #{1 2 3 4 5 6}])
  (dispatch [:contract/call :ethlance-user :diff [10 11 12] [1 2 3 4 5 6]])
  (dispatch [:contract/call :ethlance-user :diff [2] [1]])
  (dispatch [:contract/call :ethlance-user :diff [2 3] [1 3]])
  (dispatch [:contract/call :ethlance-user :sort (repeatedly 30 (partial rand-int 100))])
  (dispatch [:contract/call :ethlance-user :intersect [1 2 3 4] [2 8 1 6 5 4]])

  (dispatch [:contract/call :ethlance-db :get-u-int-value (storage-keys 6)])

  (get-entities [33] [:user/gravatar] (get-ethlance-db) #(dispatch [:log %]) #(dispatch [:log-error %]))
  (get-entities [1] [:user/address] (get-ethlance-db) #(dispatch [:log %]) #(dispatch [:log-error]))

  (get-entities-field-items {10 4}
                            :job/skills
                            (get-in @re-frame.db/app-db [:eth/contracts :ethlance-db :instance])
                            #(dispatch [:log %])
                            #(dispatch [:log-error]))

  (get-entities-field-items {1 3} :contract/messages
                            (get-in @re-frame.db/app-db [:eth/contracts :ethlance-db :instance])
                            #(dispatch [:log %])
                            #(dispatch [:log-error]))

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
                                           :search/skills-or []
                                           :search/payment-types [1 2 3]
                                           :search/experience-levels [1 2 3]
                                           :search/estimated-durations [1 2 3 4]
                                           :search/hours-per-weeks [1 2]
                                           :search/min-budget 0
                                           :search/min-budget-currency 0
                                           :search/min-employer-avg-rating 0
                                           :search/min-employer-ratings-count 0
                                           :search/country 0
                                           :search/state 0
                                           :search/language 0
                                           :search/offset 0
                                           :search/limit 10}])

  (dispatch [:contract.search/search-jobs {:search/category 0
                                           :search/skills [2070]
                                           :search/skills-or [5]
                                           :search/payment-types [1 2 3]
                                           :search/experience-levels [1 2 3]
                                           :search/estimated-durations [1 2 3 4]
                                           :search/hours-per-weeks [1 2]
                                           :search/min-budget 0
                                           :search/min-budget-currency 0
                                           :search/min-employer-avg-rating 0
                                           :search/min-employer-ratings-count 0
                                           :search/country 0
                                           :search/state 0
                                           :search/language 0
                                           :search/offset 0
                                           :search/limit 10}])

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
