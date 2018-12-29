(ns ethlance.server.contract.multi-linked-list
  "Dynamic multilinked-list (mll) for storing multiple lists within a
  single collection by providing a unique byte32 hash value."
  (:refer-clojure :exclude [nth next first second last])
  (:require
   [bignumber.core :as bn]
   [cljs-web3.eth :as web3-eth]
   [district.server.smart-contracts :as contracts]
   [cljs-solidity-sha3.core :refer [solidity-sha3]]))


(def sha3 solidity-sha3)


(def ^:dynamic *multi-linked-list-key*
  "The contract key for a contract that has inherited MultiLinkedList"
  :ethlance-registry)


(defn call
  "Call the multi linked list contract metho with the given
  `method-name` and `args`."
  [method-name & args]
  (apply contracts/contract-call *multi-linked-list-key* method-name args))


(defn push!
  "Push the provided `value` into the multi linked list defined by the
  hash value `bkey`. The value is appended to the end of the list.

  Notes:

  - Requires the contract inheriting MultiLinkedList to implement a
  `push` function calling the underlying MultiLinkedList._push
  function.
  "
  [bkey value]
  (call :push bkey value))


(defn insert! "Not Implemented" [])
(defn remove! "Not Implemented" [])


(defn nth
  "Grab the nth value at `index` from the given linked list `bkey`."
  [bkey index]
  (call :nth bkey index))


(defn iter-start
  "Start of iteration loop index.

  Notes:

  - Returns 0 if list does not exist or is empty.
  "
  [bkey]
  (call :iter-start bkey))


(defn iter-end
  "End of iteration loop index.

  Notes:

  - Returns 0 if list does not exist or is empty.
  "
  [bkey]
  (call :iter-end bkey))


(defn value
  "Get the value of the Node at the given index."
  [index]
  (call :value index))


(defn next
  "Get the next index for the given linked list."
  [index]
  (call :next index))


(defn first
  "Get the first index for a given linked list."
  [bkey]
  (call :first  bkey))


(defn second
  "Get the second index for a given linked list."
  [bkey]
  (call :second bkey))


(defn last
  "Get the last index for a given linked list."
  [bkey]
  (call :last bkey))


(defn first-value
  "Get the first value for a given linked list."
  [bkey]
  (call :first-value bkey))


(defn second-value
  "Get the second value for a given linked list."
  [bkey]
  (call :second-value bkey))


(defn last-value
  "Get the last value for a given linked list."
  [bkey]
  (call :last-value bkey))
