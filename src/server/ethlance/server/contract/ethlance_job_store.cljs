(ns ethlance.server.contract.ethlance-job-store
  "EthlanceJobStore contract methods"
  (:require
   [bignumber.core :as bn]
   [cljs-web3.eth :as web3-eth]
   [district.server.smart-contracts :as contracts]
   [ethlance.shared.enum.availability :as enum.availability]
   [ethlance.shared.enum.bid-option :as enum.bid-option]
   [ethlance.shared.enum.boolean :as enum.boolean]
   [ethlance.shared.enum.contract-status :as enum.status]
   [ethlance.shared.async-utils :refer [<!-<throw <!-<log <ignore-<! go-try] :include-macros true]
   [ethlance.server.contract]))


(def ^:dynamic *job-store-key*
  "JobStore Key"
  nil) ;; [:ethlance-job-store "0x0"]


(defn call
  "Call the bound EthlanceJobStore with the given `method-name` and `args`."
  [contract-address method-name args & [opts]]
  (ethlance.server.contract/call
   :contract-key [:ethlance-job-store contract-address]
   :method-name method-name
   :contract-arguments args
   :contract-options (or opts {})))


(defn bid-option [address] (enum.bid-option/val->kw (call address :bid_option [])))
(defn date-created [address] (call address :date_created []))
(defn date-updated [address] (call address :date_updated []))
(defn date-finished [address] (call address :date_finished []))

(defn employer-address
  "The employer address assigned to this job store."
  [address]
  (call address :employer_address []))

(defn estimated-length-seconds [address] (call address :estimated_length_seconds []))
(defn include-ether-token? [address] (call address :include_ether_token []))
(defn is-invitation-only? [address] (call address :is_invitation_only []))

(defn metahash
  "Retrieve the JobStore's metahash"
  [address]
  (call address :metahash []))


(defn update-metahash!
  "Update the JobStore's metahash with the given `metahash`."
  [address metahash]
  (call address :update-metahash [metahash]))


(defn reward-value [address] (call address :reward_value []))


(defn token-store
  "Retrieve the address of the JobStore's token storage contract."
  [address]
  (call address :token-store []))


(defn request-arbiter!
  "Request an arbiter for the job contract"
  [address arbiter-address & [opts]]
  (call
   address
   :request-arbiter [arbiter-address]
   (merge {:gas 2000000} opts)))


(defn accepted-arbiter
  "The accepted arbiter for all of the work contracts."
  [address]
  (call address :accepted_arbiter []))


(defn requested-arbiter-count
  "The number of requested arbiters in the given job contract."
  [address]
  (call address :get-requested-arbiter-count []))


(defn requested-arbiter-by-index
  "Returns requested arbiter data for the arbiter at the given
  `index`."
  [address index]
  (let [result-channel (chan 1)
        [success-channel error-channel] (call address :get-requested-arbiter-by-index [index])]
    (go
      (let [[is-employer-request? date-requested arbiter-address] (<! success-channel)]
        (>! result-channel {:is-employer-request? is-employer-request?
                            :date-requested date-requested
                            :arbiter-address arbiter-address})))
    [result-channel error-channel]))


(defn request-work-contract!
  "Request a work contract for the given `candidate-address`"
  [address candidate-address & [opts]]
  (call address :request-work-contract [candidate-address]
        (merge {:gas 2000000} opts)))


(defn work-contract-count
  "Get the number of work contracts within the bound Job Store."
  [address]
  (call address :get-work-contract-count))


(defn work-contract-by-index
  "Get the work contract address by the given `index`."
  [address index]
  (call address :get-work-contract-by-index [(bn/number index)]))


(defn fund!
  "Fund the job store. This will be used as payment for accepted work contracts."
  [contract & [opts]]
  (call contract :fund [] (merge {:gas 1000000} opts)))
