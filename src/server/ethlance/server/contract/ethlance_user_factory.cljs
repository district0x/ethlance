(ns ethlance.server.contract.ethlance-user-factory
  (:require
   [cljs-web3.eth :as web3-eth]
   [district.server.smart-contracts :as contracts]))


(defn call
  "Call the EthlanceUserFactory contract method with the given
  `method-name` and `args`."
  [method-name & args]
  (apply contracts/contract-call :ethlance-user-factory method-name args))


(defn create-user
  "Create a User with the given address, and with the given ipfs
  metahash."
  [{:keys [address metahash-ipfs]} & [opts]]
  (call :create-user address metahash-ipfs
        (merge {:gas 3000000} opts)))


(defn user-by-id
  "Get a user contract by the given user id."
  [user-id & [opts]]
  (call :get-user-by-id user-id
        (merge {:gas 3000000} opts)))


(defn user-address-by-id
  [user-id & [opts]]
  (call :get-user-address-by-id user-id
        (merge {:gas 3000000} opts)))


(defn user-count
  [& [opts]]
  (call :get-user-count
        (merge {:gas 3000000} opts)))
