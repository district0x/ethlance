(ns ethlance.server.contract.ethlance-work-contract
  "EthlanceWorkContract contract methods"
  (:require
   [bignumber.core :as bn]
   [cljs-web3.eth :as web3-eth]
   [district.server.smart-contracts :as contracts]
   [ethlance.shared.enum.contract-status :as enum.status]))


(def ^:dynamic *work-contract-key*
  "WorkContract Key"
  nil) ;; [:ethlance-work-contract "0x0"]


(defn- requires-work-contract-key
  "Asserts the correct use of the work contract functions."
  []
  (assert *work-contract-key* "Given function needs to be wrapped in 'with-ethlance-work-contract"))


(defn call
  "Call the bound EthlanceWorkContract contract with the given
  `method-name` and `args`."
  [method-name & args]
  (requires-work-contract-key)
  (apply contracts/contract-call *work-contract-key* method-name args))


(defn candidate-address
  "The accepted candidate for the currently bound work contract."
  []
  (call :candidate_address))


(defn contract-status
  "The work contract status"
  []
  (enum.status/val->kw (call :contract_status)))


(defn append-metahash!
  [metahash & [opts]]
  (call :append-metahash metahash (merge {:gas 1000000} opts)))


(defn request-invite!
  [& [opts]]
  (call :request-invite (merge {:gas 2000000} opts)))


(defn proceed!
  [& [opts]]
  (call :proceed (merge {:gas 1000000} opts)))


(defn request-finished!
  [& [opts]]
  (call :request-finished (merge {:gas 1500000} opts)))


(defn create-dispute!
  [{:keys [reason metahash]} & [opts]]
  (call :create-dispute reason metahash (merge {:gas 1000000} opts)))


(defn dispute-count
  []
  (call :get-dispute-count))


(defn dispute-by-index
  [index]
  (call :get-dispute-by-index (bn/number index)))


(defn create-invoice!
  [{:keys [amount metahash]} & [opts]]
  (call :create-invoice amount metahash (merge {:gas 1000000} opts)))


(defn invoice-count
  []
  (call :get-invoice-count))


(defn invoice-by-index
  [index]
  (call :get-invoice-by-index (bn/number index)))
