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
