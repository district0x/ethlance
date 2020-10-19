(ns ethlance.server.contract.ds-auth
  (:require [ethlance.server.contract :refer [call]]))

(defn owner
  "Get the owner address from the DSAuth contract defined by
  `contract-key`."
  [contract-key]
  (call
   :contract-key contract-key
   :method-name :owner []))

(defn set-owner!
  "Set the owner address for the DSAuth contract defined by
  `contract-key`.

  Keyword Arguments:

  new-owner - The address of the new owner

  Optional Arguments (opts)

  web3 contract-call arguments
  "
  [contract-key _ & [opts]]
  (call
   :contract-key contract-key
   :method-name :set-owner
   :contract-options (merge {:gas 100000} opts)))

(defn authority
  "Get the authority address from the DSAuth contract defined by
  `contract-key`."
  [contract-key]
  (call
   :contract-key contract-key
   :method-name :authority))

(defn set-authority!
  "Set the DSAuthority implementation defined by the contract address
  `new-authority` to the DSAuth defined by the smart-contract key
  `contract-key`."
  [contract-key new-authority & [opts]]
  (call
   :contract-key contract-key
   :method-name :set-authority
   :contract-arguments [new-authority]
   :contract-options (merge {:gas 100000} opts)))
