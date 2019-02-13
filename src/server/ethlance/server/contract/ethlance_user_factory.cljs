(ns ethlance.server.contract.ethlance-user-factory
  "EthlanceUserFactory contract methods"
  (:require
   [bignumber.core :as bn]
   [cljs-web3.eth :as web3-eth]
   [district.server.smart-contracts :as contracts]
   [clojure.core.async :as async :refer [go go-loop <! >! chan close!] :include-macros true]
   [ethlance.shared.async-utils :refer [<!-<throw go-try] :include-macros true]
   [ethlance.server.contract :refer [call]]))

(def ^:dynamic *user-factory-key*
  "Main User Factory Key"
  [:ethlance-user-factory :ethlance-user-factory-fwd])


(defn address []
  (contracts/contract-address :ethlance-user-factory-fwd))


(defn register-user!
  "Create a User with the given address, and with the given ipfs
  metahash."
  [{:keys [metahash-ipfs]} & [opts]]
  (call 
   :contract-key *user-factory-key*
   :method-name :register-user
   :contract-arguments [metahash-ipfs]
   :contract-options (merge {:gas 3000000} opts)))


(defn user-by-id
  "Get a user contract by the given user id."
  [user-id]
  (call
   :contract-key *user-factory-key*
   :method-name :get-user-by-i-d 
   :contract-arguments [(bn/number user-id)]))


(defn user-by-address
  "Get user contract linked to the given address"
  [user-address]
  (call
   :contract-key *user-factory-key*
   :method-name :get-user-by-address
   :contract-arguments [user-address]))


(defn user-count
  "Returns the number of user contracts in the registry."
  []
  (call
   :contract-key *user-factory-key*
   :method-name :get-user-count))


(defn is-registered-user?
  [user-address]
  (call
   :contract-key *user-factory-key*
   :method-name :is-registered-user
   :contract-arguments [user-address]))
