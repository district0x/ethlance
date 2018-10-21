(ns ethlance.server.contract.ethlance-job-factory-test
  "Unit Tests for EthlanceJobFactory wrapper."
  (:require
   [bignumber.core :as bn]
   [clojure.test :refer [deftest is are testing use-fixtures]]
   [cljs-web3.eth :as web3-eth]
   [taoensso.timbre :as log]

   [district.server.web3 :refer [web3]]
   [district.server.smart-contracts :as contracts]

   [ethlance.server.contract.ethlance-job-factory :as job-factory]
   [ethlance.server.contract.ethlance-user-factory :as user-factory]
   [ethlance.server.contract.ethlance-user :as user :include-macros true]
   [ethlance.server.contract.ethlance-registry :as registry]
   [ethlance.server.contract.ds-guard :as ds-guard]
   [ethlance.server.test-utils :refer-macros [deftest-smart-contract]]

   [ethlance.shared.enum.bid-option :as enum.bid-option]
   [ethlance.shared.enum.currency-type :as enum.currency]
   [ethlance.shared.enum.payment-type :as enum.payment]))


(def sample-meta-hash-1 "QmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJH")
(def sample-meta-hash-2 "JmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJ9")


(defn register-user!
  "Helper function for registering a user."
  [user-address metahash]
  (user-factory/register-user!
   {:metahash-ipfs metahash}
   {:from user-address}))


(def default-job-options
  "Bunch of helpful defaults for creating test job contracts."
  {:bid-option ::enum.bid-option/fixed-price
   :estimated-length-seconds (* 8 60 60) ;; 8 Hours
   :include-ether-token? true
   :is-bounty? false
   :is-invitation-only? false
   :employer-metahash sample-meta-hash-1
   :reward-value 0})


(defn create-job!
  "Test Helper function for creating jobs with the
  `default-job-options`."
  [job-options & [opts]]
  (job-factory/create-job! (merge default-job-options job-options) opts))


(deftest-smart-contract creating-job {}
  (let [[employer-address candidate-address arbiter-address random-user-address]
        (web3-eth/accounts @web3)

        ;; Employer User
        tx-1 (register-user! employer-address "QmZhash1")
        _ (user/with-ethlance-user (user-factory/user-by-address employer-address)
            (user/register-employer! {:from employer-address}))

        ;; Candidate User
        tx-2 (register-user! candidate-address "QmZhash2")
        _ (user/with-ethlance-user (user-factory/user-by-address candidate-address)
            (user/register-candidate!
             {:hourly-rate 120
              :currency-type ::enum.currency/usd}
             {:from candidate-address}))

        ;; Arbiter User
        tx-3 (register-user! arbiter-address "QmZhash3")
        _ (user/with-ethlance-user (user-factory/user-by-address arbiter-address)
            (user/register-arbiter!
             {:payment-value 3
              :currency-type ::enum.currency/eth
              :payment-type ::enum.payment/percentage}
             {:from arbiter-address}))]
    
    (testing "Creation of a job as an Employer"
      (is (bn/= (job-factory/job-count) 0))
      (create-job! {} {:from employer-address})
      (is (bn/= (job-factory/job-count) 1)))

    (testing "Should fail to create a job as a candidate"
      (is (thrown? js/Error (create-job! {} {:from candidate-address}))))

    (testing "Should fail to create a job as an arbiter"
      (is (thrown? js/Error (create-job! {} {:from arbiter-address}))))

    (testing "Should fail to create a job as a random user"
      (is (thrown? js/Error (create-job! {} {:from random-user-address}))))

    (testing "Jobs should be different at each index"
      (create-job! {} {:from employer-address})
      (is (bn/= (job-factory/job-count) 2))
      (is (not= (job-factory/job-by-index 0)
                (job-factory/job-by-index 1))))

    (testing "Should be out of bounds index error"
      (is (thrown? js/Error (job-factory/job-by-index 2))))))
