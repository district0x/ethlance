(ns ethlance.server.contract.test-token-test
  "Unit Tests for TestToken wrapper."
  (:require
   [bignumber.core :as bn]
   [clojure.test :refer [deftest is are testing use-fixtures]]
   [cljs-web3.eth :as web3-eth]
   [taoensso.timbre :as log]

   [district.server.web3 :refer [web3]]
   [district.server.smart-contracts :as contracts]
   [ethlance.server.contract.test-token :as test-token]
   [ethlance.server.test-utils :refer-macros [deftest-smart-contract]]))


(deftest-smart-contract main-test-token {}
  (testing "Main Tests"
    (is (test-token/address))

    (is (= (test-token/name) "TestToken"))
    (is (= (test-token/symbol) "TEST"))
    (is (bn/= (test-token/decimals) 0))
    (is (bn/= (test-token/total-supply) 0)))

  (testing "Funding accounts, getting the balance"
    (let [[user1 user2 user3] (web3-eth/accounts @web3)]
      (is (test-token/mint! user1 100 {:from user1}))
      (is (test-token/mint! user2 200 {:from user1}))
      (is (test-token/mint! user3 300 {:from user1}))

      ;; Check Balances
      (is (bn/= (test-token/balance-of user1) 100))
      (is (bn/= (test-token/balance-of user2) 200))
      (is (bn/= (test-token/balance-of user3) 300))))

  (testing "Standard Transfer"
    (let [[user1 user2 user3] (web3-eth/accounts @web3)]
      (test-token/transfer! user1 50 {:from user2})
      (is (bn/= (test-token/balance-of user1) 150))
      (is (bn/= (test-token/balance-of user2) 150))
      (test-token/transfer! user2 50 {:from user1})
      (is (bn/= (test-token/balance-of user1) 100))
      (is (bn/= (test-token/balance-of user2) 200))))

  (testing "Approved Transfer From"
    (let [[user1 user2 user3] (web3-eth/accounts @web3)]
      (is (bn/= (test-token/allowance user1 user2) 0))
      (test-token/approve! user2 50 {:from user1})
      (is (bn/= (test-token/allowance user1 user2) 50))
      (test-token/transfer-from! user1 user2 25 {:from user2})
      (is (bn/= (test-token/allowance user1 user2) 25))
      (test-token/transfer-from! user1 user2 25 {:from user2})
      (is (bn/= (test-token/allowance user1 user2) 0))
      (is (bn/= (test-token/balance-of user1) 50))
      (is (bn/= (test-token/balance-of user2) 250)))))
