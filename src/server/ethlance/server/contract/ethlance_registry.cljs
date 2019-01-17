(ns ethlance.server.contract.ethlance-registry
  "EthlanceRegistry holds User Account addresses and Job Contract addresses.
  
  It is also used to fire EthlanceEvents.
   
  Notes:

  - EthlanceRegistry is not accessed directly, it has authorized
  access via the EthlanceUserFactory and EthlanceJobFactory."
  (:require
   [bignumber.core :as bn]
   [cljs-web3.eth :as web3-eth]
   [district.server.smart-contracts :as contracts]))


(def ^:dynamic *registry-key*
  "The contract key for the EthlanceRegistry"
  :ethlance-registry)


(defn call
  "Call the EthlanceRegistry contract method with the given
  `method-name` and `args`."
  [method-name & args]
  (apply contracts/contract-call *registry-key* method-name args))


(defn address []
  (contracts/contract-address *registry-key*))


(defn permit-factory-privilege!
  "Allows the given factory contract to carry out it's own contract
  construction"
  [factory-address & [opts]]
  (call
   :permit-factory-privilege factory-address
   (merge {:gas 2000000} opts)))


(defn check-factory-privilege
  "Check if the factory is allowed to carry out its own construction.

  Notes:

  - the constructor of the constructed contract would check with this
  method to ensure the calling contract is a privileged factory."
  [factory-address & [opts]]
  (call
   :check-factory-privilege factory-address
   (merge {:gas 2000000} opts)))


(defn ethlance-event-in-tx
  "Retrieve the first EthlanceEvent emitted since `transaction-hash`.
  
  Returns {:name <:event_name> :data <:event_data>}, or nil otherwise"
  [transaction-hash]
  (when-let [tx-event (contracts/contract-event-in-tx transaction-hash *registry-key* :EthlanceEvent)]
    {:name (-> tx-event :args :event_name)
     :data (-> tx-event :args :event_data)}))


(defn comment-count
  "Get the number of comments attached to the given `address`.

  Notes:
  
  - Currently implemented by EthlanceInvoice, and EthlanceDispute."
  [address]
  (call :get-comment-count address))


(defn comment-by-index
  "Get the address of the EthlanceComment attached to `address` at `index`"
  [address index]
  (call :get-comment-by-index address index))


(defn feedback-count
  "Get the number of feedbacks
 
  Notes:

  - Currently implemented by EthlanceWorkContract."
  []
  (call :get-feedback-count address))


(defn feedback-by-address
  "Get the address of the EthlanceFeedback attached to `address`."
  [address]
  (call :get-feedback-by-address address))


;;
;; Events
;;

(defn ethlance-event [& args]
  (apply contracts/contract-call *registry-key* :EthlanceEvent args))
