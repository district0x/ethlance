(ns ethlance.server.contract.ethlance-user-test
  "Unit Tests for EthlanceUser wrapper."
  (:require
   [bignumber.core :as bn]
   [clojure.test :refer [deftest is are testing use-fixtures async]]
   [cljs-web3.eth :as web3-eth]
   [taoensso.timbre :as log]
   [clojure.core.async :refer [go go-loop <! >! chan close!] :include-macros true]

   [district.server.web3 :refer [web3]]
   [district.server.smart-contracts :as contracts]

   [ethlance.server.contract.ethlance-user :as user]
   [ethlance.server.contract.ethlance-user-factory :as user-factory]
   [ethlance.server.contract.ethlance-registry :as registry]
   [ethlance.server.contract.ds-guard :as ds-guard]
   [ethlance.server.test-utils :refer-macros [deftest-smart-contract-go]]

   [ethlance.shared.enum.currency-type :as enum.currency]
   [ethlance.shared.enum.payment-type :as enum.payment]
   [ethlance.shared.enum.bid-option :as enum.bid-option]
   [ethlance.shared.async-utils :refer [<!-<throw <!-<log <ignore-<! go-try] :include-macros true]))


(def sample-meta-hash-1 "QmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJH")
(def sample-meta-hash-2 "JmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJ9")


(deftest-smart-contract-go register-and-retrieve-user {}
  (let [[user1] (web3-eth/accounts @web3)
        tx-1 (<!-<throw (user-factory/register-user!
                         {:metahash-ipfs sample-meta-hash-1}
                         {:from user1}))
        uaddress (<!-<throw (user-factory/user-by-address user1))]
    (is (= sample-meta-hash-1 (<!-<throw (user/metahash-ipfs uaddress))))))


(deftest-smart-contract-go attempt-reconstruction {}
  (let [[user1 user2] (web3-eth/accounts @web3)
        tx-1 (user-factory/register-user!
              {:metahash-ipfs sample-meta-hash-1}
              {:from user1})
        uaddress (<!-<throw (user-factory/user-by-address user1))]
    (testing "Attempt to reconstruct user from unprivileged account"
      ;; Only the factory is privileged to construct the user contract
      (is (<ignore-<! (user/call uaddress :construct [user2 sample-meta-hash-2] {:from user1})))

      ;; Other users shouldn't be able to construct it.
      (is (<ignore-<! (user/call uaddress :construct [user2 sample-meta-hash-2] {:from user2}))))))


(deftest-smart-contract-go update-user-metahash {}
  (let [[user1 user2] (web3-eth/accounts @web3)
        tx-1 (<!-<throw (user-factory/register-user!
                         {:metahash-ipfs sample-meta-hash-1}
                         {:from user1}))
        uaddress (<!-<throw (user-factory/user-by-address user1))]

    (testing "Update the user metahash"
      (<!-<throw (user/update-metahash! uaddress sample-meta-hash-2 {:from user1}))
      (is (= (<!-<throw (user/metahash-ipfs uaddress)) sample-meta-hash-2)))

    (testing "User can't update another users contract"
      (is (<ignore-<! (user/update-metahash! uaddress sample-meta-hash-1 {:from user2}))))))


(deftest-smart-contract-go register-candidate {}
  (let [[user1 user2] (web3-eth/accounts @web3)
        tx-1 (<!-<throw (user-factory/register-user!
                         {:metahash-ipfs sample-meta-hash-1}
                         {:from user1}))

        tx-2 (<!-<throw (user-factory/register-user!
                         {:metahash-ipfs sample-meta-hash-2}
                         {:from user2}))
        uaddress (<!-<throw (user-factory/user-by-address user1))]

    (testing "Try and register a candidate for different user"
      (is (<ignore-<!
           (user/register-candidate!
            uaddress
            {:hourly-rate 99 :currency-type ::enum.currency/usd} ;; USD
            {:from user2}))))

    (testing "Register as a candidate"
      (<!-<throw 
       (user/register-candidate! 
        uaddress
        {:hourly-rate 100 :currency-type ::enum.currency/usd} ;; USD
        {:from user1})))

    (testing "Try and register candidate twice"
      (is (<ignore-<!
           (user/register-candidate! 
            uaddress
            {:hourly-rate 100 :currency-type ::enum.currency/usd} ;; USD
            {:from user1}))))

    (testing "Get the candidate data"
      (let [candidate-data (<!-<throw (user/candidate-data uaddress))]
        (is (:is-registered? candidate-data))
        (is (bn/= (:hourly-rate candidate-data) 100))
        (is (bn/= (:currency-type candidate-data) ::enum.currency/usd))))

    (testing "Update registered candidate"
      (<!-<throw
       (user/update-candidate!
        uaddress
        {:hourly-rate 80 :currency-type ::enum.currency/eth}
        {:from user1}))
      (let [candidate-data (<!-<throw (user/candidate-data uaddress))]
        (is (:is-registered? candidate-data))
        (is (bn/= (:hourly-rate candidate-data) 80))
        (is (bn/= (:currency-type candidate-data) ::enum.currency/eth))))

    (testing "Try and update candidate as other user"
      (is (<ignore-<!
           (user/update-candidate! 
            uaddress
            {:hourly-rate 80 :currency-type ::enum.currency/eth}
            {:from user2}))))))


(deftest-smart-contract-go register-arbiter {}
  (let [[user1 user2] (web3-eth/accounts @web3)
        tx-1 (<!-<throw (user-factory/register-user!
                         {:metahash-ipfs sample-meta-hash-1}
                         {:from user1}))

        tx-2 (<!-<throw (user-factory/register-user!
                         {:metahash-ipfs sample-meta-hash-2}
                         {:from user2}))
        uaddress (<!-<throw (user-factory/user-by-address user1))]

    (testing "Try and register an arbiter for different user"
      (is (<ignore-<!
           (user/register-arbiter!
            uaddress
            {:payment-value 99
             :currency-type ::enum.currency/usd
             :payment-type ::enum.payment/fixed-price}
            {:from user2}))))

    (testing "Register as an Arbiter"
      (<!-<throw
       (user/register-arbiter!
        uaddress
        {:payment-value 100
         :currency-type ::enum.currency/usd
         :payment-type ::enum.payment/fixed-price}
        {:from user1})))

    (testing "Try and register arbiter twice"
      (is (<ignore-<!
           (user/register-arbiter!
            uaddress
            {:payment-value 100
             :currency-type ::enum.currency/usd
             :payment-type ::enum.payment/fixed-price}
            {:from user1}))))

    (testing "Get the arbiter data"
      (let [arbiter-data (<!-<throw (user/arbiter-data uaddress))]
        (is (:is-registered? arbiter-data))
        (is (bn/= (:payment-value arbiter-data) 100))
        (is (bn/= (:currency-type arbiter-data) ::enum.currency/usd))
        (is (bn/= (:payment-type arbiter-data) ::enum.payment/fixed-price))))

    (testing "Update registered arbiter"
      (<!-<throw
       (user/update-arbiter!
        uaddress
        {:payment-value 3
         :currency-type ::enum.currency/eth
         :payment-type ::enum.payment/percentage}
        {:from user1}))
      (let [arbiter-data (<!-<throw (user/arbiter-data uaddress))]
        (is (:is-registered? arbiter-data))
        (is (bn/= (:currency-type arbiter-data) ::enum.currency/eth))
        (is (bn/= (:payment-value arbiter-data) 3))
        (is (bn/= (:payment-type arbiter-data) ::enum.payment/percentage))))

    (testing "Try and update arbiter as other user"
      (is (<ignore-<!
           (user/update-arbiter!
            uaddress
            {:payment-value 100
             :currency-type ::enum.currency/eth
             :payment-type ::enum.payment/fixed-price}
            {:from user2}))))))


#_(deftest-smart-contract-go register-employer {}
   (let [[user1 user2] (web3-eth/accounts @web3)
         tx-1 (<!-<throw (user-factory/register-user!
                          {:metahash-ipfs sample-meta-hash-1}
                          {:from user1}))

         tx-2 (<!-<throw (user-factory/register-user!
                          {:metahash-ipfs sample-meta-hash-2}
                          {:from user2}))
         uaddress (<!-<throw (user-factory/user-by-address user1))]

     (testing "Attempt to register employer for other user"
       (is (<ignore-<! (user/register-employer! uaddress {:from user2}))))

     (testing "Register an employer"
       (<!-<throw (user/register-employer! uaddress {:from user1})))

     (testing "Attempt to register twice"
       (is (<ignore-<! (user/register-employer! uaddress {:from user1}))))

     (testing "Get employer data"
       (let [employer-data (<!-<throw (user/employer-data uaddress))]
         (is (:is-registered? employer-data))))))

