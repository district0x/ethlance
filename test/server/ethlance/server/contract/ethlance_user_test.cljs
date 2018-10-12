(ns ethlance.server.contract.ethlance-user-test
  "Unit Tests for EthlanceUser wrapper."
  (:require
   [bignumber.core :as bn]
   [clojure.test :refer [deftest is are testing use-fixtures]]
   [cljs-web3.eth :as web3-eth]
   [taoensso.timbre :as log]

   [district.server.web3 :refer [web3]]
   [district.server.smart-contracts :as contracts]

   [ethlance.server.contract.ethlance-user :as user :include-macros true]
   [ethlance.server.contract.ethlance-user-factory :as user-factory]
   [ethlance.server.contract.ethlance-registry :as registry]
   [ethlance.server.contract.ds-guard :as ds-guard]
   [ethlance.server.test-utils :refer-macros [deftest-smart-contract]]))


(def sample-meta-hash-1 "QmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJH")
(def sample-meta-hash-2 "JmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJ9")


(deftest-smart-contract register-and-retrieve-user {}
  (let [[user1] (web3-eth/accounts @web3)
        tx-1 (user-factory/register-user!
              {:metahash-ipfs sample-meta-hash-1}
              {:from user1})]
    
    (testing "Check if metahash is correct"
      (user/with-ethlance-user (user-factory/user-by-address user1)
        (is (= sample-meta-hash-1 (user/metahash-ipfs)))))))


(deftest-smart-contract attempt-reconstruction {}
  (let [[user1 user2] (web3-eth/accounts @web3)
        tx-1 (user-factory/register-user!
              {:metahash-ipfs sample-meta-hash-1}
              {:from user1})]
    (testing "Attempt to reconstruct user from unprivileged account"
      ;; Only the factory is privileged to construct the user contract
      (is (thrown?
           js/Error
           (user/with-ethlance-user (user-factory/user-by-address user1)
             (contracts/contract-call
              user/*user-key* :construct user2 sample-meta-hash-2
              {:from user1}))))

      ;; Other users shouldn't be able to construct it.
      (is (thrown?
           js/Error
           (user/with-ethlance-user (user-factory/user-by-address user1)
             (contracts/contract-call
              user/*user-key* :construct user2 sample-meta-hash-2
              {:from user2})))))))


(deftest-smart-contract update-user-metahash {}
  (let [[user1 user2] (web3-eth/accounts @web3)
        tx-1 (user-factory/register-user!
              {:metahash-ipfs sample-meta-hash-1}
              {:from user1})]

   (testing "Update the user metahash"
     (user/with-ethlance-user (user-factory/user-by-address user1)
       (is (= (user/metahash-ipfs) sample-meta-hash-1))
       (user/update-metahash! sample-meta-hash-2 {:from user1})
       (is (= (user/metahash-ipfs) sample-meta-hash-2))))

   (testing "User can't update another users contract"
     (user/with-ethlance-user (user-factory/user-by-address user1)
       (is (thrown? js/Error (user/update-metahash! sample-meta-hash-1 {:from user2})))))))


(deftest-smart-contract register-candidate {}
  (let [[user1] (web3-eth/accounts @web3)
        tx-1 (user-factory/register-user!
              {:metahash-ipfs sample-meta-hash-1}
              {:from user1})]

    (testing "Register as a candidate"
      (user/with-ethlance-user (user-factory/user-by-address user1)
        (user/register-candidate!
         {:hourly-rate 100
          :currency-type 1} ;; USD
         {:from user1})))))
