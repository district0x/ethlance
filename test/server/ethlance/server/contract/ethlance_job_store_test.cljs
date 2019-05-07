(ns ethlance.server.contract.ethlance-job-store-test
  (:require
   [bignumber.core :as bn]
   [clojure.test :refer [deftest is are testing use-fixtures]]
   [cljs-web3.core :as web3]
   [cljs-web3.eth :as web3-eth]
   [taoensso.timbre :as log]
   [clojure.core.async :refer [go go-loop <! >! chan close!] :include-macros true]

   [district.server.web3 :refer [web3]]
   [district.server.smart-contracts :as contracts]

   [ethlance.server.contract.ethlance-job-factory :as job-factory]
   [ethlance.server.contract.ethlance-job-store :as job-store]
   [ethlance.server.contract.ethlance-user-factory :as user-factory]
   [ethlance.server.contract.ethlance-user :as user]
   [ethlance.server.contract.ethlance-registry :as registry]
   [ethlance.server.contract.ds-guard :as ds-guard]
   [ethlance.server.test-utils :refer-macros [deftest-smart-contract-go]]
   [ethlance.server.contract.test-generators :as test-gen]

   [ethlance.shared.enumeration.bid-option :as enum.bid-option]
   [ethlance.shared.enumeration.currency-type :as enum.currency]
   [ethlance.shared.enumeration.payment-type :as enum.payment]
   [ethlance.shared.async-utils :refer [<!-<throw <!-<log <ignore-<! go-try] :include-macros true]))


(def null-address "0x0000000000000000000000000000000000000000")


(deftest-smart-contract-go main-job-store {}
  (let [[employer-address candidate-address arbiter-address arbiter-address-2 random-user-address]
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
            {:from arbiter-address}))

        ;; Second Arbiter User
        tx-3 (<!-<throw (test-gen/register-user! arbiter-address-2 "QmZhash3"))
        arbiter-contract-address-2 (<!-<throw (user-factory/user-by-address arbiter-address-2))
        _ (<!-<throw
           (user/register-arbiter!
            arbiter-contract-address-2
            {:payment-value 3
             :currency-type ::enum.currency/eth
             :payment-type ::enum.payment/percentage}
            {:from arbiter-address-2}))
        
        _ (<!-<throw (test-gen/create-job-store! {} {:from employer-address}))
        job-address (<!-<throw (job-factory/job-store-by-index 0))]

    (testing "Try and request a candidate as an arbiter"
      (is (<ignore-<! (job-store/request-arbiter! job-address candidate-address {:from arbiter-address})))
      (is (<ignore-<! (job-store/request-arbiter! job-address candidate-address {:from employer-address}))))

    (testing "Try and request as something other than the employer or arbiter"
      (is (<ignore-<! (job-store/request-arbiter! job-address arbiter-address {:from random-user-address})))
      (is (<ignore-<! (job-store/request-arbiter! job-address arbiter-address {:from candidate-address}))))

    (testing "Request an arbiter as an arbiter, and accept it as an employer"
      (is (bn/= (<!-<throw (job-store/requested-arbiter-count job-address)) 0))
      (<!-<throw (job-store/request-arbiter! job-address arbiter-address {:from arbiter-address}))
      (is (bn/= (<!-<throw (job-store/requested-arbiter-count job-address)) 1))
      (<!-<throw (job-store/request-arbiter! job-address arbiter-address {:from employer-address}))
      (is (= (<!-<throw (job-store/accepted-arbiter job-address)) arbiter-address)))

    (testing "Arbiter requests should fail when an accepted arbiter has already been chosen"
      (is (<ignore-<! (job-store/request-arbiter! job-address arbiter-address-2 {:from arbiter-address-2})))
      (is (<ignore-<! (job-store/request-arbiter! job-address arbiter-address-2 {:from employer-address}))))))


#_(deftest-smart-contract request-arbiter-edge-case-1 {}
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


#_(deftest-smart-contract request-work-contract-main {}
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


#_(deftest-smart-contract job-store-funding {}
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
            (job-store/fund! {:from employer-address :value job-employer-funding}))
          (is (bn/= (web3-eth/get-balance @web3 job-address) job-employer-funding))))))
