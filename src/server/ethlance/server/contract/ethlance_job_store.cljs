(ns ethlance.server.contract.ethlance-job-store
  "EthlanceJobStore contract methods"
  (:require
   [bignumber.core :as bn]
   [cljs-web3.eth :as web3-eth]
   [district.server.smart-contracts :as contracts]))


(def ^:dynamic *job-store-key*
  "JobStore Key"
  nil) ;; [:ethlance-job-store "0x0"]


(defn- requires-job-store-key
  "Asserts the correct use of the job store functions."
  []
  (assert *job-store-key* "Given function needs to be wrapped in 'with-ethlance-job-store"))


(defn request-arbiter!
  "Request an arbiter for the job contract"
  [arbiter-address & [opts]]
  (requires-job-store-key)
  (contracts/contract-call
   *job-store-key* :request-arbiter arbiter-address
   (merge {:gas 2000000} opts)))


(defn accepted-arbiter
  "The accepted arbiter for a given job contract."
  []
  (requires-job-store-key)
  (contracts/contract-call *job-store-key* :accepted_arbiter))


(defn requested-arbiter-count
  "The number of requested arbiters in the given job contract."
  []
  (requires-job-store-key)
  (contracts/contract-call *job-store-key* :get-requested-arbiter-count))


(defn requested-arbiter-by-index
  "Returns requested arbiter data for the arbiter at the given
  `index`."
  [index]
  (requires-job-store-key)
  (let [[is-employer-request? arbiter-address]
        (contracts/contract-call *job-store-key* :get-requested-arbiter-by-index index)]
    {:is-employer-request? is-employer-request?
     :arbiter-address arbiter-address}))
