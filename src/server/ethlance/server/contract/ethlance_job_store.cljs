(ns ethlance.server.contract.ethlance-job-store
  "EthlanceJobStore contract methods"
  (:require
   [bignumber.core :as bn]
   [cljs-web3.eth :as web3-eth]
   [district.server.smart-contracts :as contracts]
   [ethlance.shared.enum.availability :as enum.availability]
   [ethlance.shared.enum.bid-option :as enum.bid-option]
   [ethlance.shared.enum.boolean :as enum.boolean]
   [ethlance.shared.enum.contract-status :as enum.status]))


(def ^:dynamic *job-store-key*
  "JobStore Key"
  nil) ;; [:ethlance-job-store "0x0"]


(defn- requires-job-store-key
  "Asserts the correct use of the job store functions."
  []
  (assert *job-store-key* "Given function needs to be wrapped in 'with-ethlance-job-store"))


(defn call
  "Call the bound EthlanceJobStore with the given `method-name` and `args`."
  [method-name & args]
  (requires-job-store-key)
  (apply contracts/contract-call *job-store-key* method-name args))


(defn bid-option [] (enum.bid-option/val->kw (call :bid_option)))
(defn date-created [] (call :date_created))
(defn date-updated [] (call :date_updated))
(defn date-finished [] (call :date_finished))

(defn employer-address
  "The employer address assigned to this job store."
  []
  (call :employer_address))

(defn estimated-length-seconds [] (call :estimated_length_seconds))
(defn include-ether-token? [] (call :include_ether_token))
(defn is-invitation-only? [] (call :is_invitation_only))

(defn metahash
  "Retrieve the JobStore's metahash"
  []
  (call :metahash))


(defn update-metahash!
  "Update the JobStore's metahash with the given `metahash`."
  [metahash]
  (call :update-metahash metahash))


(defn reward-value [] (call :reward_value))


(defn token-store
  "Retrieve the address of the JobStore's token storage contract."
  []
  (call :token-store))


(defn request-arbiter!
  "Request an arbiter for the job contract"
  [arbiter-address & [opts]]
  (call
   :request-arbiter arbiter-address
   (merge {:gas 2000000} opts)))


(defn accepted-arbiter
  "The accepted arbiter for all of the work contracts."
  []
  (call :accepted_arbiter))


(defn requested-arbiter-count
  "The number of requested arbiters in the given job contract."
  []
  (call :get-requested-arbiter-count))


(defn requested-arbiter-by-index
  "Returns requested arbiter data for the arbiter at the given
  `index`."
  [index]
  (let [[is-employer-request? date-requested arbiter-address]
        (call :get-requested-arbiter-by-index index)]
    {:is-employer-request? is-employer-request?
     :date-requested date-requested
     :arbiter-address arbiter-address}))


(defn request-work-contract!
  "Request a work contract for the given `candidate-address`"
  [candidate-address & [opts]]
  (call :request-work-contract candidate-address
        (merge {:gas 2000000} opts)))


(defn work-contract-count
  "Get the number of work contracts within the bound Job Store."
  []
  (call :get-work-contract-count))


(defn work-contract-by-index
  "Get the work contract address by the given `index`."
  [index]
  (call :get-work-contract-by-index (bn/number index)))


(defn fund!
  "Fund the job store. This will be used as payment for accepted work contracts."
  [& [opts]]
  (call :fund (merge {:gas 1000000} opts)))
