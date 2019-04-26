(ns ethlance.server.contract.ethlance-registry
  "EthlanceRegistry holds User Account addresses and Job Contract addresses.
  
  It is also used to fire EthlanceEvents.
   
  Notes:

  - EthlanceRegistry is not accessed directly, it has authorized
  access via the EthlanceUserFactory and EthlanceJobFactory."
  (:require
   [bignumber.core :as bn]
   [cljs-web3.eth :as web3-eth]
   [district.server.smart-contracts :as contracts]
   [ethlance.server.contract]))


(def ^:dynamic *registry-key*
  "The contract key for the EthlanceRegistry"
  :ethlance-registry)


(defn call
  "Call the EthlanceRegistry contract method with the given
  `method-name` and `args`."
  [method-name args & [opts]]
  (ethlance.server.contract/call
   :contract-key *registry-key*
   :method-name method-name
   :contract-arguments args
   :contract-options (or opts {})))


(defn address []
  (contracts/contract-address *registry-key*))


(defn user-id
  "Get the user id linked to the given ethereum address."
  [address]
  (call :get-user-id [address]))


(defn permit-factory-privilege!
  "Allows the given factory contract to carry out it's own contract
  construction"
  [factory-address & [opts]]
  (call
   :permit-factory-privilege [factory-address]
   (merge {:gas 2000000} opts)))


(defn check-factory-privilege
  "Check if the factory is allowed to carry out its own construction.

  Notes:

  - the constructor of the constructed contract would check with this
  method to ensure the calling contract is a privileged factory."
  [factory-address & [opts]]
  (call
   :check-factory-privilege [factory-address]
   (merge {:gas 2000000} opts)))


(defn ethlance-event-in-tx
  "Retrieve the first EthlanceEvent emitted since `transaction-hash`.
  
  Returns {:name <:event_name> :data <:event_data>}, or nil otherwise"
  [transaction-hash]
  (when-let [tx-event (contracts/contract-event-in-tx transaction-hash *registry-key* :EthlanceEvent)]
    {:name (-> tx-event :args :event-name)
     :data (-> tx-event :args :event-data)}))


(defn comment-count
  "Get the number of comments attached to the given `address`.

  Notes:
  
  - Currently implemented by EthlanceInvoice, and EthlanceDispute."
  [address]
  (when-let [result (call :get-comment-count [address])]
    result))


(defn comment-by-index
  "Get the address of the EthlanceComment attached to `address` at `index`"
  [address index]
  (call :get-comment-by-index [address index]))


(defn has-feedback?
  [address]
  (call :has-feedback [address]))


(defn feedback-by-address
  "Get the address of the EthlanceFeedback attached to `address`."
  [address]
  (call :get-feedback-by-address [address]))


;;
;; Events
;;

(defn ethlance-event [args & [opts]]
  (contracts/contract-call *registry-key* :EthlanceEvent args (or opts {})))
