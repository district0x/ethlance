(ns ethlance.server.contract.ethlance-user-factory-test
  "Unit Tests for EthlanceUserFactory wrapper."
  (:require
   [bignumber.core :as bn]
   [clojure.test :refer [deftest is are testing use-fixtures]]
   [cljs-web3.eth :as web3-eth]
   [taoensso.timbre :as log]

   [district.server.web3 :refer [web3]]

   [ethlance.server.contract.ethlance-user-factory :as user-factory]
   [ethlance.server.test-utils :refer-macros [deftest-smart-contract]]))


(def sample-meta-hash-1 "QmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJH")
(def sample-meta-hash-2 "JmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJ9")


(defn register-user! [user-address meta-hash]
  (user-factory/register-user!
   {:metahash-ipfs meta-hash}
   {:from user-address}))


(deftest-smart-contract registering-user
  {:deployer-options {} :force-deployment? false}
  
  (testing "Register New Users"
    (let [[user1 user2 user3 user4] (web3-eth/accounts @web3)
          user-count (user-factory/user-count)]
      (is (bn/= user-count 0)))))
