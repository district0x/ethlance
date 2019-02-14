(ns ethlance.server.contract.ethlance-job-factory
  "EthlanceJobFactory contract methods"
  (:require
   [bignumber.core :as bn]
   [cljs-web3.eth :as web3-eth]
   [district.server.smart-contracts :as contracts]
   [ethlance.shared.enum.bid-option :as enum.bid-option]
   [ethlance.shared.async-utils :refer [<!-<throw <!-<log <ignore-<! go-try] :include-macros true]
   [ethlance.server.contract]))


(def ^:dynamic *job-factory-key*
  "Main Job Factory Key"
  [:ethlance-job-factory :ethlance-job-factory-fwd])


(defn address []
  (contracts/contract-address :ethlance-job-factory-fwd))


(defn call
  "Call the EthlanceJobFactory contract with given `method-name` and
  `args`."
  [method-name args & [opts]]
  (ethlance.server.contract/call 
   :contract-key *job-factory-key*
   :method-name method-name
   :contract-arguments args
   :contract-options (or opts {})))


(defn create-job-store!
  "Create a job store for the given employer"
  [{:keys [bid-option
           estimated-length-seconds
           include-ether-token?
           is-invitation-only?
           metahash
           reward-value]}
   & [opts]]
  (call :create-job-store
        [(enum.bid-option/kw->val bid-option)
         estimated-length-seconds
         include-ether-token?
         is-invitation-only?
         metahash
         reward-value]
        (merge {:gas 2000000} opts)))


(defn job-store-count
  "Get the number of job contracts."
  []
  (call :get-job-store-count []))


(defn job-store-by-index
  "Get a job contract address by the given index."
  [index]
  (call :get-job-store-by-index [(bn/number index)]))
