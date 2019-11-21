(ns ethlance.server.contract.token
  "An ERC20 compliant token contract for testing out ERC20 facilities
  within Ethlance.

  # Notes:

  - This ERC20 compliant contract should not be used as an actual
  cryptocurrency, and should only be used strictly in a development
  and testing environment. Public use of this contract as a store of
  value is not recommended."
  (:refer-clojure :exclude [name symbol])
  (:require
   [bignumber.core :as bn]
   [cljs-web3-next.eth :as web3-eth]
   [district.server.smart-contracts :as contracts]
   [clojure.core.async :as async :refer [go go-loop <! >! chan] :include-macros true]
   [ethlance.shared.async-utils :refer [<!-<log <!-<throw flush! go-try] :include-macros true]
   [ethlance.server.contract]))


(def ^:dynamic *token-key*
  "The main contract key"
  :token)


(defn test-token-address []
  (contracts/contract-address *token-key*))


(defn call
  "Call the TestToken contract method with the given `method-name` and `args`."
  [address method-name args & [opts]]
  (ethlance.server.contract/call
   :contract-key [:token address]
   :method-name method-name
   :contract-arguments args
   :contract-options (or opts {})))


(defn name [address] (call address :name []))
(defn symbol [address] (call address :symbol []))
(defn decimals [address] (call address :decimals []))
(defn total-supply [address] (call address :total-supply []))


(defn balance-of
  "Retrieve the balance of TEST tokens for the given address."
  [address owner]
  (call address :balance-of [owner]))


(defn allowance
  "The allowed amount of TEST tokens that `spender` can transfer from `owner`."
  [address owner spender]
  (call address :allowance [owner spender]))


(defn transfer!
  "Transfer `value` to the given `to` address as the given owner."
  [address to value & [opts]]
  (call address :transfer [to value] opts))


(defn approve!
  "Set an allowance for `spender` at the given `value`."
  [address spender value & [opts]]
  (call address :approve [spender value] opts))


(defn transfer-from!
  "Transfer an allowance from the address `from`, to the address `to`
  with the given amount of TEST tokens, `value`."
  [address from to value & [opts]]
  (call address :transfer-from [from to value] opts))


(defn mint!
  "Mint TEST tokens to the given address `to` a given number defined by `value`.

  # Note

  - This is a publicly available function, since this contract is for
  testing purposes."
  [address to value & [opts]]
  (call address :mint [to value] opts))
