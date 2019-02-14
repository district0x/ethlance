(ns ethlance.server.contract.ethlance-dispute
  "EthlanceDispute contract methods"
  (:require
   [bignumber.core :as bn]
   [cljs-web3.eth :as web3-eth]
   [district.server.smart-contracts :as contracts]
   [clojure.core.async :as async :refer [go go-loop <! >! chan] :include-macros true]
   [ethlance.shared.async-utils :refer [<!-<log <!-<throw flush! go-try] :include-macros true]
   [ethlance.server.contract]))


(def ^:dynamic *dispute-key*
  "Dispute Key"
  nil) ;; [:ethlance-dispute "0x0"]


(defn call
  "Call the bound EthlanceDispute contract with the given
  `method-name` and `args`."
  [address method-name args & [opts]]
  (ethlance.server.contract/call
   :contract-key [:ethlance-dispute address]
   :method-name method-name
   :contract-arguments args
   :contract-options (or opts {})))


(defn date-created [address] (call address :date_created []))
(defn date-updated [address] (call address :date_updated []))
(defn date-resolved [address] (call address :date_resolved []))
(defn reason [address] (call address :reason []))
(defn employer-resolution-amount [address] (call address :employer_resolution_amount []))
(defn candidate-resolution-amount [address] (call address :candidate_resolution_amount []))
(defn arbiter-resolution-amount [address] (call address :arbiter_resolution_amount []))


(defn append-metahash!
  "Append a `metahash` payload to the given dispute."
  [address metahash & [opts]]
  (call address :append-metahash [metahash] (merge {:gas 1000000} opts)))


(defn resolve!
  "Resolve a dispute between an employer and a candidate(employee).

  Notes:

  - *-token parameters represent the type of ERC20 token that will be
  transferred in resolution. By default, it will use the ETH currency
  defined by '0x0'."
  [address
   {:keys [employer-amount
           employer-token
           candidate-amount
           candidate-token
           arbiter-amount
           arbiter-token]
    :or {employer-token "0x0"
         candidate-token "0x0"
         arbiter-token "0x0"}}
   & [opts]]
  (call
   address
   :resolve
   [employer-amount employer-token
    candidate-amount candidate-token
    arbiter-amount arbiter-token]
   (merge {:gas 1000000} opts)))


(defn resolved?
  "Returns true if the dispute has already been resolved."
  [address]
  (call address :is-resolved []))


(defn add-comment!
  [address metahash & [opts]]
  (call address :add-comment [metahash] (merge {:gas 1500000} opts)))


(defn leave-feedback!
  [address rating metahash & [opts]]
  (call address :leave-feedback [rating metahash] (merge {:gas 1500000} opts)))
