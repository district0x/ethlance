(ns ethlance.server.contract.ethlance-user-factory
  "EthlanceUserFactory contract methods"
  (:require
   [bignumber.core :as bn]
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
  [user-id]
  (call :get-user-by-i-d (bn/number user-id)))


(defn user-by-address
  "Get user contract linked to the given address"
  [user-address]
  (call :get-user-by-address user-address))


(defn user-count
  "Returns the number of user contracts in the registry."
  []
  (call :get-user-count))


(defn is-registered-user?
  [user-address]
  (call :is-registered-user))
