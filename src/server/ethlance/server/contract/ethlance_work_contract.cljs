(ns ethlance.server.contract.ethlance-work-contract
  "EthlanceWorkContract contract methods"
  (:require
   [bignumber.core :as bn]
   [cljs-web3.eth :as web3-eth]
   [district.server.smart-contracts :as contracts]
   [ethlance.shared.enumeration.contract-status :as enum.status]
   [clojure.core.async :as async :refer [go go-loop <! >! chan] :include-macros true]
   [ethlance.shared.async-utils :refer [<!-<log <!-<throw flush! go-try] :include-macros true]
   [ethlance.server.contract]))


(def ^:dynamic *work-contract-key*
  "WorkContract Key"
  nil) ;; [:ethlance-work-contract "0x0"]


(defn call
  "Call the bound EthlanceWorkContract contract with the given
  `method-name` and `args`."
  [address method-name args & [opts]]
  (ethlance.server.contract/call
   :contract-key [:ethlance-work-contract address]
   :method-name method-name
   :contract-arguments args
   :contract-options (or opts {})))


(defn date-updated [address] (call address :date-updated []))
(defn date-created [address] (call address :date-created []))


(defn candidate-address
  "The accepted candidate for the currently bound work contract."
  [address]
  (call address :candidate-address []))


(defn contract-status
  "The work contract status"
  [address]
  (let [result-channel (chan 1)
        [success-channel error-channel] (call address :contract-status [])]
    (go
      (let [result (enum.status/val->kw (<! success-channel))]
        (>! result-channel result)))
    
    [result-channel error-channel]))


(defn request-invite!
  [address sender & [opts]]
  (call address :request-invite [sender] (merge {:gas 2000000} opts)))


(defn proceed!
  [address & [opts]]
  (call address :proceed [] (merge {:gas 1000000} opts)))


(defn request-finished!
  [address & [opts]]
  (call address :request-finished [] (merge {:gas 1500000} opts)))


(defn create-dispute!
  [address {:keys [reason metahash]} & [opts]]
  (call address :create-dispute [reason metahash] (merge {:gas 1000000} opts)))


(defn dispute-count
  [address]
  (call address :get-dispute-count []))


(defn dispute-by-index
  [address index]
  (call address :get-dispute-by-index [(bn/number index)]))


(defn create-invoice!
  [address {:keys [amount metahash]} & [opts]]
  (call address :create-invoice [amount metahash] (merge {:gas 1000000} opts)))


(defn invoice-count
  [address]
  (call address :get-invoice-count []))


(defn invoice-by-index
  [address index]
  (call address :get-invoice-by-index [(bn/number index)]))


(defn add-comment!
  [address metahash & [opts]]
  (call address :add-comment [metahash] (merge {:gas 1500000} opts)))


(defn leave-feedback!
  [address rating metahash & [opts]]
  (call address :leave-feedback [rating metahash] (merge {:gas 1500000} opts)))
