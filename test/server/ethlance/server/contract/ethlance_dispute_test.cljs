(ns ethlance.server.contract.ethlance-dispute-test
  (:require
   [bignumber.core :as bn]
   [clojure.test :refer [deftest is are testing use-fixtures]]
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
   [ethlance.server.contract.ethlance-dispute :as dispute :include-macros true]
   [ethlance.server.test-utils :refer-macros [deftest-smart-contract]]
   [ethlance.server.contract.test-generators :as test-gen]

   [ethlance.shared.enum.bid-option :as enum.bid-option]
   [ethlance.shared.enum.currency-type :as enum.currency]
   [ethlance.shared.enum.payment-type :as enum.payment]))

(def null-address "0x0000000000000000000000000000000000000000")


(deftest-smart-contract main-dispute {}
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

    ;; Create a Job Store, and assign an accepted arbiter
    (test-gen/create-job-store! {} {:from employer-address})
    (job-store/with-ethlance-job-store (job-factory/job-store-by-index 0)
      (is (= (job-store/employer-address) employer-address))
      (job-store/request-arbiter! arbiter-address {:from arbiter-address})
      (job-store/request-arbiter! arbiter-address {:from employer-address})
      (is (= (job-store/accepted-arbiter) arbiter-address))

      ;; Create a work contract, and assign a candidate
      (job-store/request-work-contract! candidate-address {:from candidate-address})
      (work-contract/with-ethlance-work-contract (job-store/work-contract-by-index 0)
        (work-contract/request-invite! {:from employer-address})
        (is (= (work-contract/candidate-address) candidate-address))
        (work-contract/proceed! {:from employer-address})

        ;; Create a dispute
        (is (bn/= (work-contract/dispute-count) 0))
        (work-contract/create-dispute!
         {:reason "For being 'testy'" :metahash ""}
         {:from employer-address})
        (is (bn/= (work-contract/dispute-count) 1))))))

        ;; 
