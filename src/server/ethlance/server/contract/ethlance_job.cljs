(ns ethlance.server.contract.ethlance-job
  "EthlanceJob contract methods"
  (:require
   [bignumber.core :as bn]
   [cljs-web3.eth :as web3-eth]
   [district.server.smart-contracts :as contracts]))


(def ^:dynamic *job-key*
  "Job Contract Key"
  nil) ;; [:ethlance-job "0x0"]


(defn- requires-job-key
  "Asserts the correct use of the job functions."
  []
  (assert *job-key* "Given function needs to be wrapped in 'with-ethlance-job"))


(defn metahash-ipfs
  "Retrieve the job's IPFS metahash."
  [& [opts]]
  (requires-job-key)
  (contracts/contract-call *job-key* :metahash_ipfs (merge {:gas 1000000} opts)))


(defn update-metahash!
  "Update the job's IPFS metahash"
  [new-metahash & [opts]]
  (requires-job-key)
  (contracts/contract-call
   *job-key* :update-metahash new-metahash
   (merge {:gas 1000000} opts)))


