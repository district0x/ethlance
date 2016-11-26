(ns ethlance.events
  (:require
    [ajax.core :as ajax]
    [akiroz.re-frame.storage :as re-frame-storage]
    [camel-snake-kebab.core :as cs :include-macros true]
    [cljs-web3.core :as web3]
    [cljs-web3.eth :as web3-eth]
    [cljs-web3.personal :as web3-personal]
    [cljs.spec :as s]
    [day8.re-frame.http-fx]
    [ethlance.db :refer [default-db]]
    [ethlance.utils :as u]
    [goog.string :as gstring]
    [goog.string.format]
    [madvas.re-frame.google-analytics-fx]
    [madvas.re-frame.web3-fx]
    [re-frame.core :refer [reg-event-db reg-event-fx path trim-v after debug reg-fx console dispatch]]))

(re-frame-storage/reg-co-fx! :ethlance {:fx :localstorage :cofx :localstorage})

(def interceptors [;check-spec-interceptor
                   ;(when ^boolean goog.DEBUG debug)
                   trim-v])

(defn contract-xhrio [contract-name code-type on-success on-failure]
  {:method :get
   :uri (gstring/format "./contracts/build/%s.%s" contract-name (name code-type))
   :timeout 6000
   :response-format (if (= code-type :abi) (ajax/json-response-format) (ajax/text-response-format))
   :on-success on-success
   :on-failure on-failure})

(def all-contracts
  {:ethlance-user "EthlanceUser"
   :ethlance-job "EthlanceJob"
   :ethlance-contract "EthlanceContract"
   :ethlance-invoice "EthlanceInvoice"
   :ethlance-config "EthlanceConfig"
   :ethlance-db "EthlanceDB"
   :ethlance-views "EthlanceViews"
   :ethlance-search "EthlanceSearch"})

(def deploy-contract-gas 4500000)

(comment
  (dispatch [:initialize])
  (dispatch [:print-db])
  (dispatch [:deploy-contracts])
  (dispatch [:estimate-contracts])
  )

(reg-event-fx
  :initialize
  (fn [_]
    (let [{:keys [web3 provides-web3?]} default-db]
      (merge
        {:db default-db
         :http-xhrio
         (for [[key name] all-contracts]
           (for [code-type [:abi :bin]]
             (contract-xhrio name code-type [:contract/loaded key code-type] [:log-error])))}
        (when provides-web3?
          {:web3-fx.blockchain/fns
           {:web3 web3
            :fns [[web3-eth/accounts :blockchain/my-addresses-loaded :blockchain/on-error]]}})))))

(reg-event-fx
  :deploy-contracts
  interceptors
  (fn [{:keys [db]}]
    (let [ethance-db (get-in db [:contracts :ethlance-db])]
      {:web3-fx.blockchain/fns
       {:web3 (:web3 db)
        :fns [[web3-eth/contract-new
               (:abi ethance-db)
               {:gas deploy-contract-gas
                :data (:bin ethance-db)
                :from (first (:my-addresses db))}
               :contract/on-deployed-ethlance-db
               :log-error]]}})))

(reg-event-fx
  :contract/on-deployed-ethlance-db
  interceptors
  (fn [{:keys [db]} [instance]]
    (when-let [db-address (aget instance "address")]
      (console :log :ethlance-db " deployed at " db-address)
      {:db (update-in db [:contracts :ethlance-db] merge {:address db-address
                                                          :instance instance})
       :web3-fx.blockchain/fns
       {:web3 (:web3 db)
        :fns (for [[key {:keys [abi bin]}] (dissoc (:contracts db) :ethlance-db)]
               [web3-eth/contract-new
                abi
                {:gas deploy-contract-gas
                 :data bin
                 :from (first (:my-addresses db))}
                [:contract/on-deployed key]
                [:log-error key]])}})))

(reg-event-fx
  :contract/on-deployed
  interceptors
  (fn [{:keys [db]} [key instance]]
    (when-let [contract-address (aget instance "address")]
      (console :log key " deployed at " contract-address)
      {:db (update-in db [:contracts key] merge {:address contract-address
                                                 :instance instance})})))

(reg-event-fx
  :estimate-contracts
  interceptors
  (fn [{:keys [db]}]
    {:web3-fx.blockchain/fns
     {:web3 (:web3 db)
      :fns (for [[key {:keys [abi bin]}] (:contracts db)]
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
  interceptors
  (fn [{:keys [db]} [address]]
    {:db (assoc db :active-address address)}))

(reg-event-fx
  :blockchain/my-addresses-loaded
  interceptors
  (fn [{:keys [db]} [addresses]]
    (let [addresses-map (reduce #(assoc %1 %2 {:address %2}) {} addresses)]
      (merge
        {:db (-> db
               (assoc :my-addresses addresses)
               (assoc :active-address (first addresses))
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

(reg-event-db
  :contract/loaded
  interceptors
  (fn [db [contract-key code-type code]]
    (assoc-in db [:contracts contract-key code-type] (if (= code-type :abi)
                                                       (clj->js code)
                                                       code))))

(reg-event-db
  :print-db
  interceptors
  (fn [db]
    (print.foo/look db)
    db))

(reg-event-db
  :log-error
  interceptors
  (fn [db errors]
    (apply console :error errors)
    db))

