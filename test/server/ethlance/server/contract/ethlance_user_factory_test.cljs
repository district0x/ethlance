(ns ethlance.server.contract.ethlance-user-factory-test
  "Unit Tests for EthlanceUserFactory wrapper."
  (:require
   [bignumber.core :as bn]
   [clojure.test :refer [deftest is are testing use-fixtures]]
   [cljs-web3.eth :as web3-eth]
   [taoensso.timbre :as log]
   [clojure.core.async :refer [go go-loop <! >! chan close!] :include-macros true]

   [district.server.web3 :refer [web3]]
   [district.server.smart-contracts :as contracts]
   [ethlance.server.test-utils :refer-macros [deftest-smart-contract-go]]

   [ethlance.server.contract.ethlance-user-factory :as user-factory]
   [ethlance.server.contract.ethlance-registry :as registry]
   [ethlance.server.contract.ds-guard :as ds-guard]
   [ethlance.shared.async-utils :refer [<!-<throw <!-<log <ignore-<! go-try] :include-macros true]))


(def sample-meta-hash-1 "QmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJH")
(def sample-meta-hash-2 "JmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJ9")


(defn register-user!
  "Helper function for registering a user."
  [user-address meta-hash]
  (user-factory/register-user!
   {:metahash-ipfs meta-hash}
   {:from user-address}))


(deftest-smart-contract-go check-authorization
  {:deployer-options {} :force-deployment? false}
  (let [[user1] (web3-eth/accounts @web3)]
    ;; small test for revert functionality
    (<!-<throw (register-user! user1 sample-meta-hash-2))
    (testing "Check if user factory has access to registry"
      (is (<!-<throw (ds-guard/can-call?
                      {:src (user-factory/address)
                       :dst (registry/address)
                       :sig ds-guard/ANY}))))))


(deftest-smart-contract-go check-factory-privilege
  {:deployer-options {} :force-deployment? false}
  (testing "EthlanceUserFactory Forward is privileged to carry out construction."
    (is (<!-<throw
         (registry/check-factory-privilege (contracts/contract-address :ethlance-user-factory-fwd))))))


(deftest-smart-contract-go registering-duplicate-user
  {:deployer-options [] :force-deployment? false}
  (testing "Registering twice should fail the second time."
    (let [[user1] (web3-eth/accounts @web3)]
      (<!-<throw (register-user! user1 sample-meta-hash-1))
      (is (<ignore-<! (register-user! user1 sample-meta-hash-1))))))


(deftest-smart-contract-go registering-user
  {:deployer-options {} :force-deployment? false}

  (testing "Register New Users"
    (let [[user1 user2 user3 user4] (web3-eth/accounts @web3)
          user-count-1 (<!-<throw (user-factory/user-count))
          
          ;; Register First User
          tx-1 (<!-<throw (register-user! user1 sample-meta-hash-1))
          ethlance-event (registry/ethlance-event-in-tx tx-1 :ethlance-user-factory-fwd :EthlanceEvent)
          uid-1 (-> ethlance-event :data first)
          user-count-2 (<!-<throw (user-factory/user-count))

          ;; Register Second User
          tx-2 (<!-<throw (register-user! user2 sample-meta-hash-2))
          ethlance-event-2 (registry/ethlance-event-in-tx tx-2 :ethlance-user-factory-fwd :EthlanceEvent)
          uid-2 (-> ethlance-event-2 :data first)
          user-count-3 (<!-<throw (user-factory/user-count))]

      (testing "Check initial user pool"
        (is (bn/= user-count-1 0)))

      (testing "Check against first user"
        (is (= (:name ethlance-event) "UserRegistered"))
        (is (bn/= uid-1 1))
        (is (bn/= user-count-2 1)))

      (testing "Check against second user"
        (is (= (:name ethlance-event-2) "UserRegistered"))
        (is (bn/= uid-2 2))
        (is (bn/= user-count-3 2)))

      (testing "Check user ID versus address 1"
        (is (= (<!-<throw (user-factory/user-by-id uid-1))
               (<!-<throw (user-factory/user-by-address user1)))))

      (testing "Check user ID versus address 1"
        (is (= (<!-<throw (user-factory/user-by-id uid-2))
               (<!-<throw (user-factory/user-by-address user2)))))

      (testing "Getting an invalid user-id should throw out of bounds"
        (is (<ignore-<! (user-factory/user-by-id 99))))

      (testing "Getting an invalid user address should throw."
        (is (<ignore-<! (user-factory/user-by-address user4)))))))
