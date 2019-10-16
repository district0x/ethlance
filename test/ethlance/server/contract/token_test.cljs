(ns ethlance.server.contract.token-test
  "Unit Tests for Token wrapper."
  (:require
   [bignumber.core :as bn]
   [clojure.test :refer [deftest is are testing use-fixtures]]
   [cljs-web3.eth :as web3-eth]
   [taoensso.timbre :as log]
   [clojure.core.async :as async :refer [go go-loop <! >! chan] :include-macros true]

   [district.server.web3 :refer [web3]]
   [district.server.smart-contracts :as contracts]
   [ethlance.server.contract.token :as token]
   [ethlance.server.test-utils :refer-macros [deftest-smart-contract-go]]
   [ethlance.shared.async-utils :refer [<!-<log <!-<throw flush! go-try] :include-macros true]))

(deftest-smart-contract-go main-token {}
  (let [token-address (token/test-token-address)]
    (testing "Main Tests"
      (log/debug "Grabbing Token Attributes...")
      (is (token/test-token-address))

      (is (= (<!-<throw (token/name token-address)) "TestToken"))
      (is (= (<!-<throw (token/symbol token-address)) "TEST"))
      (is (bn/= (<!-<throw (token/decimals token-address)) 0))
      (is (bn/= (<!-<throw (token/total-supply token-address)) 0)))

    (testing "Funding accounts, getting the balance"
      (log/debug "Minting User Accounts...")
      (let [[user1 user2 user3] (web3-eth/accounts @web3)]
        (is (<!-<throw (token/mint! token-address user1 100 {:from user1})))
        (is (<!-<throw (token/mint! token-address user2 200 {:from user1})))
        (is (<!-<throw (token/mint! token-address user3 300 {:from user1})))

        ;; Check Balances
        (log/debug "Checking the token balances...")
        (is (bn/= (<!-<throw (token/balance-of token-address user1)) 100))
        (is (bn/= (<!-<throw (token/balance-of token-address user2)) 200))
        (is (bn/= (<!-<throw (token/balance-of token-address user3)) 300))))

    (testing "Standard Transfer"
      (log/debug "Attemping transfer...")
      (let [[user1 user2 user3] (web3-eth/accounts @web3)]
        (<!-<throw (token/transfer! token-address user1 50 {:from user2}))
        (is (bn/= (<!-<throw (token/balance-of token-address user1)) 150))
        (is (bn/= (<!-<throw (token/balance-of token-address user2)) 150))
        (<!-<throw (token/transfer! token-address user2 50 {:from user1}))
        (is (bn/= (<!-<throw (token/balance-of token-address user1)) 100))
        (is (bn/= (<!-<throw (token/balance-of token-address user2)) 200))))

    (testing "Approved Transfer From"
      (log/debug "Attempt transfer_from with approval...")
      (let [[user1 user2 user3] (web3-eth/accounts @web3)]
        (is (bn/= (<!-<throw (token/allowance token-address user1 user2)) 0))
        (log/debug "- Approving...")
        (<!-<throw (token/approve! token-address user2 50 {:from user1}))
        (is (bn/= (<!-<throw (token/allowance token-address user1 user2)) 50))
        (log/debug "- Transferring...")
        (<!-<throw (token/transfer-from! token-address user1 user2 25 {:from user2}))
        (is (bn/= (<!-<throw (token/allowance token-address user1 user2)) 25))
        (log/debug "- Transferring...")
        (<!-<throw (token/transfer-from! token-address user1 user2 25 {:from user2}))
        (is (bn/= (<!-<throw (token/allowance token-address user1 user2)) 0))
        (is (bn/= (<!-<throw (token/balance-of token-address user1)) 50))
        (is (bn/= (<!-<throw (token/balance-of token-address user2)) 250))))))
