(ns ethlance.server.contract.ds-auth
  "Functions for manipulating a DSAuth contract."
  (:require
   [cljs-web3.eth :as web3-eth]
   [district.server.smart-contracts :as contracts]
   [ethlance.server.contract :refer [call]]))


(defn owner
  "Get the owner address from the DSAuth contract defined by
  `contract-key`."
  [contract-key]
  (call contract-key :owner [] {}))


(defn set-owner!
  "Set the owner address for the DSAuth contract defined by
  `contract-key`.

  Keyword Arguments:
  
  new-owner - The address of the new owner

  Optional Arguments (opts)

  web3 contract-call arguments
  "
  [contract-key new-owner & [opts]]
  (call contract-key :set-owner [] (merge {:gas 100000} opts)))


(defn authority
  "Get the authority address from the DSAuth contract defined by
  `contract-key`."
  [contract-key]
  (call contract-key :authority [] {}))


(defn set-authority!
  "Set the DSAuthority implementation defined by the contract address
  `new-authority` to the DSAuth defined by the smart-contract key
  `contract-key`."
  [contract-key new-authority & [opts]]
  (call contract-key :set-authority [new-authority] (merge {:gas 100000} opts)))
