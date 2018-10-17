(ns ethlance.server.contract.ethlance-job-factory
  "EthlanceJobFactory contract methods"
  (:require
   [bignumber.core :as bn]
   [cljs-web3.eth :as web3-eth]
   [district.server.smart-contracts :as contracts]))


(def ^:dynamic *job-factory-key*
  "Main Job Factory Key"
  [:ethlance-job-factory :ethlance-job-factory-fwd])


(defn address []
  (contracts/contract-address :ethlance-job-factory-fwd))


(defn call
  "Call the EthlanceJobFactory contract with given `method-name` and
  `args`."
  [method-name & args]
  (apply contracts/contract-call *job-factory-key* method-name args))


(defn create-job!
  "Create a job contract"
  [{:keys [bid-option
           estimated-length-seconds
           include-ether-token?
           is-bounty?
           is-invitation-only?
           employer-metahash
           reward-value]}
   & [opts]]
  (call :create-job
        bid-option
        estimated-length-seconds
        include-ether-token?
        is-bounty?
        is-invitation-only?
        employer-metahash
        reward-value
        (merge {:gas 2000000} opts)))


(defn job-count
  "Get the number of job contracts."
  [& [opts]]
  (call :get-job-count (merge {:gas 1000000} opts)))


(defn job-by-index
  "Get a job contract address by the given index."
  [index & [opts]]
  (call :get-job-by-index index (merge {:gas 1000000} opts)))


