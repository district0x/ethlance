(ns ethlance.server.contract.ethlance-registry
  "EthlanceRegistry holds User Account addresses and Job Contract addresses.
  
  It is also used to fire EthlanceEvents.
   
  Notes:

  - EthlanceRegistry is not accessed directly, it has authorized
  access via the EthlanceUserFactory and EthlanceJobFactory."
  (:require
   [cljs-web3.eth :as web3-eth]
   [district.server.smart-contracts :as contracts]))


(defn address []
  (contracts/contract-address :ethlance-registry))


(defn ethlance-event-in-tx
  "Retrieve the first EthlanceEvent emitted since `transaction-hash`.
  
  Returns {:name <:event_name> :data <:event_data>}, or nil otherwise"
  [transaction-hash]
  (when-let [tx-event (contracts/contract-event-in-tx transaction-hash :ethlance-registry :EthlanceEvent)]
    {:name (-> tx-event :args :event_name)
     :data (-> tx-event :args :event_data)}))
