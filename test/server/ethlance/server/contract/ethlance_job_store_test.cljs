(ns ethlance.server.contract.ethlance-job-store-test
  (:require
   [bignumber.core :as bn]
   [clojure.test :refer [deftest is are testing use-fixtures]]
   [cljs-web3.core :as web3]
   [cljs-web3.eth :as web3-eth]
   [taoensso.timbre :as log]

   [district.server.web3 :refer [web3]]
   [district.server.smart-contracts :as contracts]

   [ethlance.server.contract.ethlance-job-factory :as job-factory]
   [ethlance.server.contract.ethlance-job-store :as job-store :include-macros true]
   [ethlance.server.contract.ethlance-user-factory :as user-factory]
   [ethlance.server.contract.ethlance-user :as user :include-macros true]
   [ethlance.server.contract.ethlance-registry :as registry]
   [ethlance.server.contract.ds-guard :as ds-guard]
   [ethlance.server.test-utils :refer-macros [deftest-smart-contract]]
   [ethlance.server.contract.test-generators :as test-gen]

   [ethlance.shared.enum.bid-option :as enum.bid-option]
   [ethlance.shared.enum.currency-type :as enum.currency]
   [ethlance.shared.enum.payment-type :as enum.payment]))


(def null-address "0x0000000000000000000000000000000000000000")


(deftest-smart-contract main-job-store {}
  (let [[employer-address candidate-address arbiter-address arbiter-address-2 random-user-address]
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
             {:from arbiter-address}))

        ;; Second Arbiter User
        tx-4 (test-gen/register-user! arbiter-address-2 "QmZhash5")
        _ (user/with-ethlance-user (user-factory/user-by-address arbiter-address-2)
            (user/register-arbiter!
             {:payment-value 3
              :currency-type ::enum.currency/eth
              :payment-type ::enum.payment/percentage}
             {:from arbiter-address-2}))]

    (testing "Creation of a job store, with all of the additional options"
      (is (bn/= (job-factory/job-store-count) 0))
      (test-gen/create-job-store! {} {:from employer-address})
      (is (bn/= (job-factory/job-store-count) 1))
      (job-store/with-ethlance-job-store (job-factory/job-store-by-index 0)
        (is (= (job-store/accepted-arbiter) null-address))))

    (testing "Try and request a candidate as an arbiter"
      (is (thrown? js/Error (job-store/request-arbiter! candidate-address {:from arbiter-address})))
      (is (thrown? js/Error (job-store/request-arbiter! candidate-address {:from employer-address}))))

    (testing "Try and request as something other than the employer or arbiter"
      (is (thrown? js/Error (job-store/request-arbiter! arbiter-address {:from random-user-address})))
      (is (thrown? js/Error (job-store/request-arbiter! arbiter-address {:from candidate-address}))))

    (testing "Request an arbiter as an arbiter, and accept it as an employer"
      (job-store/with-ethlance-job-store (job-factory/job-store-by-index 0)
        (is (bn/= (job-store/requested-arbiter-count) 0))
        (job-store/request-arbiter! arbiter-address {:from arbiter-address})
        (is (bn/= (job-store/requested-arbiter-count) 1))
        (job-store/request-arbiter! arbiter-address {:from employer-address})
        (is (= (job-store/accepted-arbiter) arbiter-address))))

    (testing "Arbiter requests should fail when an accepted arbiter has already been chosen"
        (is (thrown? js/Error (job-store/request-arbiter! arbiter-address-2 {:from arbiter-address-2})))
        (is (thrown? js/Error (job-store/request-arbiter! arbiter-address-2 {:from employer-address}))))))


(deftest-smart-contract request-arbiter-edge-case-1 {}
  (let [[employer-address candidate-address arbiter-address random-user-address]
        (web3-eth/accounts @web3)

        ;; Employer User
        tx-1 (test-gen/register-user! employer-address "QmZhash1")
        _ (user/with-ethlance-user (user-factory/user-by-address employer-address)
            (user/register-employer! {:from employer-address})
            (user/register-arbiter!
             {:payment-value 3
              :currency-type ::enum.currency/eth
              :payment-type ::enum.payment/percentage}
             {:from employer-address}))

        ;; Arbiter User
        tx-3 (test-gen/register-user! arbiter-address "QmZhash3")
        _ (user/with-ethlance-user (user-factory/user-by-address arbiter-address)
            (user/register-arbiter!
             {:payment-value 3
              :currency-type ::enum.currency/eth
              :payment-type ::enum.payment/percentage}
             {:from arbiter-address}))]
    (test-gen/create-job-store! {} {:from employer-address})

    (testing "Try and have the employer become the arbiter of the contract"
      (job-store/with-ethlance-job-store (job-factory/job-store-by-index 0)
        (is (thrown? js/Error (job-store/request-arbiter! employer-address {:from employer-address})))))))


(deftest-smart-contract request-work-contract-main {}
  (let [[employer-address candidate-address candidate-address-2 arbiter-address random-user-address]
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

        ;; Second Candidate User
        tx-3 (test-gen/register-user! candidate-address-2 "QmZhash2")
        _ (user/with-ethlance-user (user-factory/user-by-address candidate-address-2)
            (user/register-candidate!
             {:hourly-rate 120
              :currency-type ::enum.currency/usd}
             {:from candidate-address-2}))

        ;; Arbiter User
        tx-4 (test-gen/register-user! arbiter-address "QmZhash3")
        _ (user/with-ethlance-user (user-factory/user-by-address arbiter-address)
            (user/register-arbiter!
             {:payment-value 3
              :currency-type ::enum.currency/eth
              :payment-type ::enum.payment/percentage}
             {:from arbiter-address}))]

     (test-gen/create-job-store! {} {:from employer-address})
     (testing "Requesting a work contract as a Candidate"
       (job-store/with-ethlance-job-store (job-factory/job-store-by-index 0)
         (is (bn/= (job-store/work-contract-count) 0))
         (job-store/request-work-contract! candidate-address {:from candidate-address})
         (is (bn/= (job-store/work-contract-count) 1))))

     (test-gen/create-job-store! {} {:from employer-address})
     (testing "Requesting a work contract as an Employer"
       (job-store/with-ethlance-job-store (job-factory/job-store-by-index 1)
         (is (bn/= (job-store/work-contract-count) 0))
         (job-store/request-work-contract! candidate-address {:from employer-address})
         (is (bn/= (job-store/work-contract-count) 1))))))


(deftest-smart-contract job-store-funding {}
  (let [[employer-address candidate-address arbiter-address arbiter-address-2 random-user-address]
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
             {:from candidate-address}))]

    (test-gen/create-job-store! {} {:from employer-address})
    (job-store/with-ethlance-job-store (job-factory/job-store-by-index 0)
      (is (= employer-address (job-store/employer-address))))
    
    (testing "Adding funds to the work contract"
      (let [job-employer-funding (web3/to-wei 20.0 :ether)
            job-address (job-factory/job-store-by-index 0)]
        (is (bn/= (web3-eth/get-balance @web3 job-address) 0))

        ;; Employer funding the job store.
        (job-store/with-ethlance-job-store job-address
          (job-store/fund {:from employer-address :value job-employer-funding}))
        (is (bn/= (web3-eth/get-balance @web3 job-address) job-employer-funding))))))
