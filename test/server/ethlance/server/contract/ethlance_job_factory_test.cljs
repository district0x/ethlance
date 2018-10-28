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
   [ethlance.server.contract.test-generators :as test-gen]

   [ethlance.shared.enum.bid-option :as enum.bid-option]
   [ethlance.shared.enum.currency-type :as enum.currency]
   [ethlance.shared.enum.payment-type :as enum.payment]))


(deftest-smart-contract creating-job-store {}
  (let [[employer-address candidate-address arbiter-address random-user-address]
        (web3-eth/accounts @web3)

        ;; Employer User
        tx-1 (test-gen/register-user! employer-address "QmZhash1")
        _ (user/with-ethlance-user (user-factory/user-by-address employer-address)
            (user/register-employer! {:from employer-address}))

        ;; Candidate User
        tx-2 (test-gen/register-user! candidate-address "QmZhash2")
        _ (user/with-ethlance-user (user-factory/user-by-address candidate-address)
            (user/register-candidate!
             {:hourly-rate 120
              :currency-type ::enum.currency/usd}
             {:from candidate-address}))

        ;; Arbiter User
        tx-3 (test-gen/register-user! arbiter-address "QmZhash3")
        _ (user/with-ethlance-user (user-factory/user-by-address arbiter-address)
            (user/register-arbiter!
             {:payment-value 3
              :currency-type ::enum.currency/eth
              :payment-type ::enum.payment/percentage}
             {:from arbiter-address}))]
    
     (testing "Creation of a job store as an Employer"
       (is (bn/= (job-factory/job-store-count) 0))
       (test-gen/create-job-store! {} {:from employer-address})
       (is (bn/= (job-factory/job-store-count) 1)))

    (testing "Should fail to create a job store as a candidate"
      (is (thrown? js/Error (test-gen/create-job-store! {} {:from candidate-address}))))

    (testing "Should fail to create a job store as an arbiter"
      (is (thrown? js/Error (test-gen/create-job-store! {} {:from arbiter-address}))))

    (testing "Should fail to create a job store as a random user"
      (is (thrown? js/Error (test-gen/create-job-store! {} {:from random-user-address}))))

    (testing "JobStore's should be different at each index"
      (test-gen/create-job-store! {} {:from employer-address})
      (is (bn/= (job-factory/job-store-count) 2))
      (is (not= (job-factory/job-store-by-index 0)
                (job-factory/job-store-by-index 1))))

    (testing "Should be out of bounds index error"
      (is (thrown? js/Error (job-factory/job-store-by-index 2))))))
