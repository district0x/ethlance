(ns tests.contract.token-test
  "Unit Tests for Token wrapper."
  #_(:require
   [bignumber.core :as bn]
   [clojure.test :refer [deftest is are testing use-fixtures]]
   [cljs-web3-next.eth :as web3-eth]
   [taoensso.timbre :as log]
   [clojure.core.async :as async :refer [go go-loop <! >! chan] :include-macros true]
   [district.shared.async-helpers :refer [<?]]
   [district.server.web3 :refer [web3]]
   [district.server.smart-contracts :as contracts]
   [ethlance.server.contract.token :as token]
   [ethlance.server.test-utils :refer-macros [deftest-smart-contract-go]]))

#_(defn bignum [n]
  (str n))

#_(deftest-smart-contract-go main-token-1 {}
  (let [token-address (token/test-token-address)]
    (println "Test token address " token-address)

    (testing "Main Tests"
      (log/debug "Grabbing Token Attributes...")
      (is (token/test-token-address))

      (is (= (<? (token/name token-address)) "TestToken"))
      (is (= (<? (token/symbol token-address)) "TEST"))
      (is (bn/= (<? (token/decimals token-address)) (bignum 0)))
      (is (bn/= (<? (token/total-supply token-address)) (bignum 0))))

    (testing "Funding accounts, getting the balance"
      (log/debug "Minting User Accounts...")
      (let [[user1 user2 user3] (<! (web3-eth/accounts @web3))]
        (is (<? (token/mint! token-address user1 100 {:from user1})))
        (is (<? (token/mint! token-address user2 200 {:from user1})))
        (is (<? (token/mint! token-address user3 300 {:from user1})))

        ;; Check Balances
        (log/debug "Checking the token balances...")
        (is (bn/= (<? (token/balance-of token-address user1)) (bignum 100)))
        (is (bn/= (<? (token/balance-of token-address user2)) (bignum 200)))
        (is (bn/= (<? (token/balance-of token-address user3)) (bignum 300)))))))

#_(deftest-smart-contract-go main-token-2 {}
  (let [token-address (token/test-token-address)]
    (println "Test token address " token-address)

    (testing "Standard Transfer"
      (log/debug "Attemping transfer...")
      (let [[user1 user2 user3] (<! (web3-eth/accounts @web3))]
        (<? (token/transfer! token-address user1 50 {:from user2}))
        (is (bn/= (<? (token/balance-of token-address user1)) (bignum 150)))
        (is (bn/= (<? (token/balance-of token-address user2)) (bignum 150)))
        (<? (token/transfer! token-address user2 50 {:from user1}))
        (is (bn/= (<? (token/balance-of token-address user1)) (bignum 100)))
        (is (bn/= (<? (token/balance-of token-address user2)) (bignum 200)))))

    (testing "Approved Transfer From"
      (log/debug "Attempt transfer_from with approval...")
      (let [[user1 user2 user3] (<! (web3-eth/accounts @web3))]
        (is (bn/= (<? (token/allowance token-address user1 user2)) (bignum 0)))
        (log/debug "- Approving...")
        (<? (token/approve! token-address user2 50 {:from user1}))
        (is (bn/= (<? (token/allowance token-address user1 user2)) (bignum 50)))
        (log/debug "- Transferring...")
        (<? (token/transfer-from! token-address user1 user2 25 {:from user2}))
        (is (bn/= (<? (token/allowance token-address user1 user2)) (bignum 25)))
        (log/debug "- Transferring...")
        (<? (token/transfer-from! token-address user1 user2 25 {:from user2}))
        (is (bn/= (<? (token/allowance token-address user1 user2)) (bignum 0)))
        (is (bn/= (<? (token/balance-of token-address user1)) (bignum 50)))
        (is (bn/= (<? (token/balance-of token-address user2)) (bignum 250)))))))
