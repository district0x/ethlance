(ns ethlance.server.contract.ethlance-user-factory-test
  "Unit Tests for EthlanceUserFactory wrapper."
  (:require
   [bignumber.core :as bn]
   [clojure.test :refer [deftest is are testing use-fixtures]]
   [cljs-web3.eth :as web3-eth]
   [taoensso.timbre :as log]

   [district.server.web3 :refer [web3]]
   [district.server.smart-contracts :as contracts]

   [ethlance.server.contract.ethlance-user-factory :as user-factory]
   [ethlance.server.contract.ethlance-registry :as registry]
   [ethlance.server.contract.ds-guard :as ds-guard]
   [ethlance.server.test-utils :refer-macros [deftest-smart-contract]]))


(def sample-meta-hash-1 "QmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJH")
(def sample-meta-hash-2 "JmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJ9")


(defn register-user!
  "Helper function for registering a user."
  [user-address meta-hash]
  (user-factory/register-user!
   {:metahash-ipfs meta-hash}
   {:from user-address}))


(deftest-smart-contract check-authorization
  {:deployer-options {} :force-deployment? false}
  (let [[user1] (web3-eth/accounts @web3)]
    ;; small test for revert functionality
    (register-user! user1 sample-meta-hash-2)
    (testing "Check if user factory has access to registry"
      (is (ds-guard/can-call? {:src (user-factory/address)
                               :dst (registry/address)
                               :sig ds-guard/ANY})))))


(deftest-smart-contract check-factory-privilege
  {:deployer-options {} :force-deployment? false}
  (testing "EthlanceUserFactory Forward is privileged to carry out construction."
    (is (true? (registry/check-factory-privilege (contracts/contract-address :ethlance-user-factory-fwd))))))


(deftest-smart-contract registering-user
  {:deployer-options {} :force-deployment? false}

  (testing "Register New Users"
    (let [[user1 user2 user3 user4] (web3-eth/accounts @web3)
          user-count-1 (user-factory/user-count)
          
          ;; Register First User
          tx-1 (register-user! user1 sample-meta-hash-1)
          ethlance-event (registry/ethlance-event-in-tx tx-1)
          uid-1 (-> ethlance-event :data first)
          user-count-2 (user-factory/user-count)

          ;; Register Second User
          tx-2 (register-user! user2 sample-meta-hash-2)
          ethlance-event-2 (registry/ethlance-event-in-tx tx-2)
          uid-2 (-> ethlance-event-2 :data first)
          user-count-3 (user-factory/user-count)]

      #_(testing "Try and register an already registered user"
          (is (throws? js/Error (register-user! user1 sample-meta-hash-1))))

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
        (is (= (user-factory/user-by-id uid-1)
               (user-factory/user-by-address user1)))))))
