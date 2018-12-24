(ns ethlance.server.contract.test-token
  "An ERC20 compliant token contract for testing out ERC20 facilities
  within Ethlance.

  # Notes:

  - This ERC20 compliant contract should not be used as an actual
  cryptocurrency, and should only be used strictly in a development
  and testing environment. Public use of this contract as a store of
  value is not recommended."
  (:require
   [bignumber.core :as bn]
   [cljs-web3.eth :as web3-eth]
   [district.server.smart-contracts :as contracts]))


(def ^:dynamic *test-token-key*
  "The main contract key"
  :test-token)


(defn address []
  (contracts/contract-address *test-token-key*))


(defn call
  "Call the TestToken contract method with the given `method-name` and `args`."
  [method-name & args]
  (apply contracts/contract-call *test-token-key* method-name args))



(defn name [] (call :name))
(defn symbol [] (call :symbol))
(defn decimals [] (call :decimals))
(defn total-supply [] (call :total-supply))


(defn balance-of
  "Retrieve the balance of TEST tokens for the given address."
  [owner]
  (call :balance-of owner))


(defn allowance
  "The allowed amount of TEST tokens that `spender` can transfer from `owner`."
  [owner spender]
  (call :allowance owner spender))


(defn transfer
  "Transfer `value` to the given `to` address as the given owner."
  [to value]
  (call :transfer to value))


(defn approve
  "Set an allowance for `spender` at the given `value`."
  [spender value]
  (call :approve spender value))


(defn transfer-from
  "Transfer an allowance from the address `from`, to the address `to`
  with the given amount of TEST tokens, `value`."
  [from to value]
  (call :transfer-from from to value))
  

(defn mint
  "Mint TEST tokens to the given address `to` a given number defined by `value`.

  # Note

  - This is a publicly available function, since this contract is for
  testing purposes."
  [to value])
