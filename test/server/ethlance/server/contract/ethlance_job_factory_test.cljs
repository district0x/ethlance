(ns ethlance.server.contract.ethlance-job-factory-test
  "Unit Tests for EthlanceJobFactory wrapper."
  (:require
   [bignumber.core :as bn]
   [clojure.test :refer [deftest is are testing use-fixtures]]
   [cljs-web3.eth :as web3-eth]
   [taoensso.timbre :as log]
   [clojure.core.async :refer [go go-loop <! >! chan close!] :include-macros true]

   [district.server.web3 :refer [web3]]
   [district.server.smart-contracts :as contracts]

   [ethlance.server.contract.ethlance-job-factory :as job-factory]
   [ethlance.server.contract.ethlance-user-factory :as user-factory]
   [ethlance.server.contract.ethlance-user :as user]
   [ethlance.server.contract.ethlance-registry :as registry]
   [ethlance.server.contract.ds-guard :as ds-guard]
   [ethlance.server.test-utils :refer-macros [deftest-smart-contract-go]]
   [ethlance.server.contract.test-generators :as test-gen]

   [ethlance.shared.enum.bid-option :as enum.bid-option]
   [ethlance.shared.enum.currency-type :as enum.currency]
   [ethlance.shared.enum.payment-type :as enum.payment]
   [ethlance.shared.async-utils :refer [<!-<throw <!-<log <ignore-<! go-try] :include-macros true]))


(deftest-smart-contract-go creating-job-store {}
  (let [[employer-address candidate-address arbiter-address random-user-address]
        (web3-eth/accounts @web3)

        ;; Employer User
        tx-1 (<!-<throw (test-gen/register-user! employer-address "QmZhash1"))
        employer-contract-address (<!-<throw (user-factory/user-by-address employer-address))
        _ (<!-<throw (user/register-employer! employer-contract-address {:from employer-address}))

        ;; Candidate User
        tx-2 (<!-<throw (test-gen/register-user! candidate-address "QmZhash2"))
        candidate-contract-address (<!-<throw (user-factory/user-by-address candidate-address))
        _ (<!-<throw
           (user/register-candidate!
            candidate-contract-address
            {:hourly-rate 120
             :currency-type ::enum.currency/usd}
            {:from candidate-address}))

        ;; Arbiter User
        tx-3 (<!-<throw (test-gen/register-user! arbiter-address "QmZhash3"))
        arbiter-contract-address (<!-<throw (user-factory/user-by-address arbiter-address))
        _ (<!-<throw
           (user/register-arbiter!
            arbiter-contract-address
            {:payment-value 3
             :currency-type ::enum.currency/eth
             :payment-type ::enum.payment/percentage}
            {:from arbiter-address}))]
    
    (testing "Creation of a job store as an Employer"
      (is (bn/= (<!-<throw (job-factory/job-store-count)) 0))
      (<!-<throw (test-gen/create-job-store! {} {:from employer-address}))
      (is (bn/= (<!-<throw (job-factory/job-store-count)) 1)))

    (testing "Should fail to create a job store as a candidate"
      (is (<ignore-<! (test-gen/create-job-store! {} {:from candidate-address}))))

    (testing "Should fail to create a job store as an arbiter"
      (is (<ignore-<! (test-gen/create-job-store! {} {:from arbiter-address}))))

    (testing "Should fail to create a job store as a random user"
      (is (<ignore-<! (test-gen/create-job-store! {} {:from random-user-address}))))

    (testing "JobStore's should be different at each index"
      (<!-<throw (test-gen/create-job-store! {} {:from employer-address}))
      (is (bn/= (<!-<throw (job-factory/job-store-count)) 2))
      (is (not= (<!-<throw (job-factory/job-store-by-index 0))
                (<!-<throw (job-factory/job-store-by-index 1)))))

    (testing "Should be out of bounds index error"
      (is (<ignore-<! (job-factory/job-store-by-index 2))))))
