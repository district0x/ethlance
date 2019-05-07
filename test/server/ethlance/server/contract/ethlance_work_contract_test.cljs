(ns ethlance.server.contract.ethlance-work-contract-test
  (:require
   [bignumber.core :as bn]
   [clojure.test :refer [deftest is are testing use-fixtures]]
   [cljs-web3.core :as web3]
   [cljs-web3.eth :as web3-eth]
   [taoensso.timbre :as log]
   [clojure.core.async :as async :refer [go go-loop <! >! chan] :include-macros true]

   [district.server.web3 :refer [web3]]
   [district.server.smart-contracts :as contracts]

   [ethlance.server.contract.ethlance-job-factory :as job-factory]
   [ethlance.server.contract.ethlance-job-store :as job-store]
   [ethlance.server.contract.ethlance-work-contract :as work-contract]
   [ethlance.server.contract.ethlance-user-factory :as user-factory]
   [ethlance.server.contract.ethlance-user :as user]
   [ethlance.server.contract.ethlance-registry :as registry]
   [ethlance.server.contract.ds-guard :as ds-guard]
   [ethlance.server.test-utils :refer-macros [deftest-smart-contract-go]]
   [ethlance.server.contract.test-generators :as test-gen]

   [ethlance.shared.enumeration.bid-option :as enum.bid-option]
   [ethlance.shared.enumeration.currency-type :as enum.currency]
   [ethlance.shared.enumeration.payment-type :as enum.payment]
   [ethlance.shared.enumeration.contract-status :as enum.status]
   [ethlance.shared.async-utils :refer [<!-<log <!-<throw flush! go-try] :include-macros true]))


(def null-address "0x0000000000000000000000000000000000000000")


(deftest-smart-contract-go main-work-contract {}
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

        _ (<!-<throw (test-gen/create-job-store! {} {:from employer-address}))
        job-address (<!-<throw (job-factory/job-store-by-index 0))]

    (testing "Create a invite request as a candidate, and accept the invite as an employer"
      (is (= employer-address (<!-<throw (job-store/employer-address job-address))))

      ;; Request the accepted arbiter
      (<!-<throw (job-store/request-arbiter! job-address arbiter-address {:from arbiter-address}))
      (<!-<throw (job-store/request-arbiter! job-address arbiter-address {:from employer-address}))

      ;; Create the initial work contract as the candidate
      (<!-<throw (job-store/request-work-contract! job-address candidate-address {:from candidate-address}))
      (let [work-address (<!-<throw (job-store/work-contract-by-index job-address 0))]

        (is (= candidate-address (<!-<throw (work-contract/candidate-address work-address))))
        (is (= ::enum.status/request-candidate-invite (<!-<throw (work-contract/contract-status work-address))))
        
        ;; Invite the candidate as the employer
        (<!-<throw (work-contract/request-invite! work-address {:from employer-address}))
        (is (= ::enum.status/accepted) (<!-<throw (work-contract/contract-status work-address)))

        ;; Proceed with the work contract
        (<!-<throw (work-contract/proceed! work-address {:from employer-address}))
        (is (= ::enum.status/in-progress (<!-<throw (work-contract/contract-status work-address))))))

    (testing "Create a invite request as a employer, and accept the invite as a candidate"
      (test-gen/create-job-store! {} {:from employer-address})
      (let [job-address (<!-<throw (job-factory/job-store-by-index 1))]

        ;; Request the accepted arbiter
        (<!-<throw (job-store/request-arbiter! job-address arbiter-address {:from arbiter-address}))
        (<!-<throw (job-store/request-arbiter! job-address arbiter-address {:from employer-address}))

        ;; Create the initial work contract as the employer.
        (<!-<throw (job-store/request-work-contract! job-address candidate-address {:from employer-address}))
        (let [work-address (<!-<throw (job-store/work-contract-by-index job-address 0))]
          (is (= candidate-address (<!-<throw (work-contract/candidate-address work-address))))
          (is (= ::enum.status/request-employer-invite (<!-<throw (work-contract/contract-status work-address))))

          (testing "Accept the invite as the candidate."
            (<!-<throw (work-contract/request-invite! work-address {:from candidate-address}))
            (is (= ::enum.status/accepted) (<!-<throw (work-contract/contract-status work-address))))

          (testing "Proceed with the work contract."
            (<!-<throw (work-contract/proceed! work-address {:from employer-address}))
            (is (= ::enum.status/in-progress (<!-<throw (work-contract/contract-status work-address)))))

          (testing "Request Finished by candidate,"
            (is (<!-<throw (work-contract/request-finished! work-address {:from candidate-address})))
            (is (= ::enum.status/request-candidate-finished
                   (<!-<throw (work-contract/contract-status work-address)))))

          (testing "Accept Finished by employer"
            (is (<!-<throw (work-contract/request-finished! work-address {:from employer-address})))
            (is (= ::enum.status/finished (<!-<throw (work-contract/contract-status work-address))))))))))
