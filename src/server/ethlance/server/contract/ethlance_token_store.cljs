(ns ethlance.server.contract.ethlance-token-store
  (:require
   [bignumber.core :as bn]
   [cljs-web3.eth :as web3-eth]
   [district.server.smart-contracts :as contracts]))


(def ^:dynamic *token-store-key*
  "The main contract key"
  :token-store)


(defn address []
  (contracts/contract-address *token-store-key*))


(defn call
  "Call the TestToken contract method with the given `method-name` and `args`."
  [method-name & args]
  (apply contracts/contract-call *token-store-key* method-name args))


(defn add-token!
  "Add an ERC20 compliant token to the given token store."
  [token-address & [opts]]
  (call :add-token token-address (merge {:gas 1000000} opts)))


(defn has-token?
  [token-address]
  (call :has-token token-address))


(defn token-by-index
  [index]
  (call :get-token-by-index))


(defn token-count
  []
  (call :get-token-count))
