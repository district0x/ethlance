(ns ethlance.server.contract.token-test
  "Unit Tests for Token wrapper."
  (:require
   [bignumber.core :as bn]
   [clojure.test :refer [deftest is are testing use-fixtures]]
   [cljs-web3.eth :as web3-eth]
   [taoensso.timbre :as log]

   [district.server.web3 :refer [web3]]
   [district.server.smart-contracts :as contracts]
   [ethlance.server.contract.token :as token]
   [ethlance.server.test-utils :refer-macros [deftest-smart-contract]]))


(deftest-smart-contract main-token {}
  (testing "Main Tests"
    (is (token/address))

    (is (= (token/name) "TestToken"))
    (is (= (token/symbol) "TEST"))
    (is (bn/= (token/decimals) 0))
    (is (bn/= (token/total-supply) 0)))

  (testing "Funding accounts, getting the balance"
    (let [[user1 user2 user3] (web3-eth/accounts @web3)]
      (is (token/mint! user1 100 {:from user1}))
      (is (token/mint! user2 200 {:from user1}))
      (is (token/mint! user3 300 {:from user1}))

      ;; Check Balances
      (is (bn/= (token/balance-of user1) 100))
      (is (bn/= (token/balance-of user2) 200))
      (is (bn/= (token/balance-of user3) 300))))

  (testing "Standard Transfer"
    (let [[user1 user2 user3] (web3-eth/accounts @web3)]
      (token/transfer! user1 50 {:from user2})
      (is (bn/= (token/balance-of user1) 150))
      (is (bn/= (token/balance-of user2) 150))
      (token/transfer! user2 50 {:from user1})
      (is (bn/= (token/balance-of user1) 100))
      (is (bn/= (token/balance-of user2) 200))))

  (testing "Approved Transfer From"
    (let [[user1 user2 user3] (web3-eth/accounts @web3)]
      (is (bn/= (token/allowance user1 user2) 0))
      (token/approve! user2 50 {:from user1})
      (is (bn/= (token/allowance user1 user2) 50))
      (token/transfer-from! user1 user2 25 {:from user2})
      (is (bn/= (token/allowance user1 user2) 25))
      (token/transfer-from! user1 user2 25 {:from user2})
      (is (bn/= (token/allowance user1 user2) 0))
      (is (bn/= (token/balance-of user1) 50))
      (is (bn/= (token/balance-of user2) 250)))))
