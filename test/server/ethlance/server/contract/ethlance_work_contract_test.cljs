(ns ethlance.server.contract.ethlance-work-contract-test
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
   [ethlance.server.contract.ethlance-work-contract :as work-contract :include-macros true]
   [ethlance.server.contract.ethlance-user-factory :as user-factory]
   [ethlance.server.contract.ethlance-user :as user :include-macros true]
   [ethlance.server.contract.ethlance-registry :as registry]
   [ethlance.server.contract.ds-guard :as ds-guard]
   [ethlance.server.test-utils :refer-macros [deftest-smart-contract]]
   [ethlance.server.contract.test-generators :as test-gen]

   [ethlance.shared.enum.bid-option :as enum.bid-option]
   [ethlance.shared.enum.currency-type :as enum.currency]
   [ethlance.shared.enum.payment-type :as enum.payment]
   [ethlance.shared.enum.contract-status :as enum.status]))


(def null-address "0x0000000000000000000000000000000000000000")


(deftest-smart-contract main-work-contract {}
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
             {:from arbiter-address}))]

    (testing "Create a invite request as a candidate, and accept the invite as an employer"
      (test-gen/create-job-store! {} {:from employer-address})
      (job-store/with-ethlance-job-store (job-factory/job-store-by-index 0)
        (is (= employer-address (job-store/employer-address)))

        ;; Request the accepted arbiter
        (job-store/request-arbiter! arbiter-address {:from arbiter-address})
        (job-store/request-arbiter! arbiter-address {:from employer-address})

        ;; Create the initial work contract as the candidate
        (job-store/request-work-contract! candidate-address {:from candidate-address})
        (work-contract/with-ethlance-work-contract (job-store/work-contract-by-index 0)

          (is (= candidate-address (work-contract/candidate-address)))
          (is (= ::enum.status/request-candidate-invite (work-contract/contract-status)))
          
          ;; Invite the candidate as the employer
          (work-contract/request-invite! {:from employer-address})
          (is (= ::enum.status/accepted) (work-contract/contract-status))

          ;; Proceed with the work contract
          (work-contract/proceed! {:from employer-address})
          (is (= ::enum.status/in-progress (work-contract/contract-status))))))

    (testing "Create a invite request as a employer, and accept the invite as a candidate"
      (test-gen/create-job-store! {} {:from employer-address})
      (job-store/with-ethlance-job-store (job-factory/job-store-by-index 1)

        ;; Request the accepted arbiter
        (job-store/request-arbiter! arbiter-address {:from arbiter-address})
        (job-store/request-arbiter! arbiter-address {:from employer-address})

        ;; Create the initial work contract as the employer.
        (job-store/request-work-contract! candidate-address {:from employer-address})
        (work-contract/with-ethlance-work-contract (job-store/work-contract-by-index 0)
          (is (= candidate-address (work-contract/candidate-address)))
          (is (= ::enum.status/request-employer-invite (work-contract/contract-status)))

          (testing "Accept the invite as the candidate."
            (work-contract/request-invite! {:from candidate-address})
            (is (= ::enum.status/accepted) (work-contract/contract-status)))

          (testing "Proceed with the work contract."
            (work-contract/proceed! {:from employer-address})
            (is (= ::enum.status/in-progress (work-contract/contract-status))))

          (testing "Request Finished by candidate,"
            (is (work-contract/request-finished! {:from candidate-address}))
            (is (= ::enum.status/request-candidate-finished (work-contract/contract-status))))

          (testing "Accept Finished by employer"
            (is (work-contract/request-finished! {:from employer-address}))
            (is (= ::enum.status/finished (work-contract/contract-status)))))))))
