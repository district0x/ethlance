(ns ethlance.server.contract.ethlance-user-factory
  (:require
   [cljs-web3.eth :as web3-eth]
   [district.server.smart-contracts :as contracts]))


(def ^:dynamic *user-factory-key*
  "Main User Factory Key"
  [:ethlance-user-factory :ethlance-user-factory-fwd])


(defn address []
  (contracts/contract-address :ethlance-user-factory-fwd))


(defn call
  "Call the EthlanceUserFactory contract method with the given
  `method-name` and `args`."
  [method-name & args]
  (apply contracts/contract-call *user-factory-key* method-name args))


(defn register-user!
  "Create a User with the given address, and with the given ipfs
  metahash."
  [{:keys [metahash-ipfs]} & [opts]]
  (call :register-user metahash-ipfs
        (merge {:gas 3000000} opts)))


(defn user-by-id
  "Get a user contract by the given user id."
  [user-id & [opts]]
  (call :get-user-by-id user-id
        (merge {:gas 3000000} opts)))


(defn user-by-address
  [user-address & [opts]]
  (call :get-user-by-address user-address
        (merge {:gas 3000000} opts)))


(defn user-count
  [& [opts]]
  (call :get-user-count
        (merge {:gas 3000000} opts)))


(defn is-registered-user?
  [user-address & [opts]]
  (call :is-registered-user
        (merge {:gas 1000000} opts)))
