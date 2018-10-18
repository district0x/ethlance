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


(defn metahash-store
  "Get the store of employer, candidate and arbiter IPFS metahashes"
  [& [opts]]
  (requires-job-key)
  (contracts/contract-call *job-key* :get-metahash-store (merge {:gas 1000000} opts)))


(defn employer-metahash
  "Retrieve the employer's IPFS metahash."
  [& [opts]]
  (requires-job-key)
  (first (metahash-store)))
    

(defn update-employer-metahash!
  "Update the employer's IPFS metahash"
  [new-metahash & [opts]]
  (requires-job-key)
  (contracts/contract-call
   *job-key* :update-employer-metahash new-metahash
   (merge {:gas 1000000} opts)))


(defn candidate-metahash
  "Retrieve the candidate's metahash."
  [& [opts]]
  (requires-job-key)
  (second (metahash-store)))


(defn update-candidate-metahash!
  "Update the candidate's IPFS metahash."
  [new-metahash & [opts]]
  (requires-job-key)
  (contracts/contract-call
   *job-key* :update-candidate-metahash new-metahash
   (merge {:gas 1000000} opts)))


(defn arbiter-metahash
  "Retrieve the arbiter's metahash"
  [& [opts]]
  (requires-job-key)
  (nth (metahash-store) 2))


(defn update-arbiter-metahash!
  "Update the arbiter's IPFS metahash."
  [new-metahash & [opts]]
  (requires-job-key)
  (contracts/contract-call
   *job-key* :update-arbiter-metahash new-metahash
   (merge {:gas 1000000} opts)))


(defn request-candidate!
  "Request a candidate for the job contract"
  [candidate-address & [opts]]
  (requires-job-key)
  (contracts/contract-call
   *job-key* :request-candidate candidate-address
   (merge {:gas 2000000} opts)))


(defn request-arbiter!
  "Request an arbiter for the job contract"
  [arbiter-address & [opts]]
  (requires-job-key)
  (contracts/contract-call
   *job-key* :request-arbiter arbiter-address
   (merge {:gas 2000000} opts)))


(defn accepted-candidate
  "The accepted candidate for a given job contract."
  []
  (requires-job-key)
  (contracts/contract-call *job-key* :accepted_candidate))


(defn accepted-arbiter
  "The accepted arbiter for a given job contract."
  []
  (requires-job-key)
  (contracts/contract-call *job-key* :accepted_arbiter))
