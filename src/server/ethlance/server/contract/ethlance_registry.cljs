(ns ethlance.server.contract.ethlance-registry
  "EthlanceRegistry holds User Account addresses and Job Contract addresses.
  
  It is also used to fire EthlanceEvents.
   
  Notes:

  - EthlanceRegistry is not accessed directly, it has authorized
  access via the EthlanceUserFactory and EthlanceJobFactory."
  (:require
   [cljs-web3.eth :as web3-eth]
   [district.server.smart-contracts :as contracts]))


(def ^:dynamic *registry-key*
  "The contract key for the EthlanceRegistry"
  :ethlance-registry)


(defn address []
  (contracts/contract-address *registry-key*))


(defn permit-factory-privilege!
  "Allows the given factory contract to carry out it's own contract
  construction"
  [factory-address & [opts]]
  (contracts/contract-call
   *registry-key* :permit-factory-privilege factory-address
   (merge {:gas 2000000} opts)))


(defn check-factory-privilege
  "Check if the factory is allowed to carry out its own construction.

  Notes:

  - the constructor of the constructed contract would check with this
  method to ensure the calling contract is a privileged factory."
  [factory-address & [opts]]
  (contracts/contract-call
   *registry-key* :check-factory-privilege factory-address
   (merge {:gas 2000000} opts)))


(defn ethlance-event-in-tx
  "Retrieve the first EthlanceEvent emitted since `transaction-hash`.
  
  Returns {:name <:event_name> :data <:event_data>}, or nil otherwise"
  [transaction-hash]
  (when-let [tx-event (contracts/contract-event-in-tx transaction-hash *registry-key* :EthlanceEvent)]
    {:name (-> tx-event :args :event_name)
     :data (-> tx-event :args :event_data)}))
