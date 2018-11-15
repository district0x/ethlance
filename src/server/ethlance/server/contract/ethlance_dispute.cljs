(ns ethlance.server.contract.ethlance-dispute
  "EthlanceDispute contract methods"
  (:require
   [bignumber.core :as bn]
   [cljs-web3.eth :as web3-eth]
   [district.server.smart-contracts :as contracts]))


(def ^:dynamic *dispute-key*
  "Dispute Key"
  nil) ;; [:ethlance-dispute "0x0"]


(defn- requires-dispute-key
  "Asserts the correct use of the dispute functions."
  []
  (assert *dispute-key* "Given function needs to be wrapped in 'with-ethlance-dispute"))


(defn call
  "Call the bound EthlanceDispute contract with the given
  `method-name` and `args`."
  [method-name & args]
  (requires-dispute-key)
  (apply contracts/contract-call *dispute-key* method-name args))


(defn append-metahash!
  "Append a `metahash` payload to the given dispute."
  [metahash & [opts]]
  (call :append-metahash metahash (merge {:gas 1000000} opts)))


(defn resolve!
  "Resolve a dispute between an employer and a candidate(employee).

  Notes:

  - *-token parameters represent the type of ERC20 token that will be
  transferred in resolution. By default, it will use the ETH currency
  defined by '0x0'."
  [{:keys [employer-amount
           employer-token
           candidate-amount
           candidate-token
           arbiter-amount
           arbiter-token]
    :or {employer-token "0x0"
         candidate-token "0x0"
         arbiter-token "0x0"}}
   & [opts]]
  (call :resolve
        employer-amount employer-token
        candidate-amount candidate-token
        arbiter-amount arbiter-token
        (merge {:gas 1000000} opts)))


(defn resolved?
  "Returns true if the dispute has already been resolved."
  []
  (call :is-resolved))
