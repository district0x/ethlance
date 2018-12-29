(ns ethlance.server.contract.multi-linked-list
  "Dynamic multi-list for storing multiple lists within a single
  collection by providing a unique byte32 hash value."
  (:require
   [bignumber.core :as bn]
   [cljs-web3.eth :as web3-eth]
   [district.server.smart-contracts :as contracts]
   [cljs-solidity-sha3.core :refer [solidity-sha3]]))


(def sha3 solidity-sha3)


