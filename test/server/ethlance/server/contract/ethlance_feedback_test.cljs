(ns ethlance.server.contract.ethlance-feedback-test
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
   [ethlance.server.contract.ethlance-dispute :as dispute :include-macros true]
   [ethlance.server.contract.ethlance-feedback :as feedback :include-macros true]
   [ethlance.server.test-utils :refer-macros [deftest-smart-contract]]
   [ethlance.server.contract.test-generators :as test-gen]

   [ethlance.shared.enum.bid-option :as enum.bid-option]
   [ethlance.shared.enum.currency-type :as enum.currency]
   [ethlance.shared.enum.payment-type :as enum.payment]
   [ethlance.shared.enum.user-type :as enum.user-type]))

(def null-address "0x0000000000000000000000000000000000000000")


(deftest-smart-contract main-feedback-work-contract {}
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

        comment-hash-1 "test1"
        comment-hash-2 "test2"]

    ;; Create a Job Store, assign an accepted arbiter, and fund it.
    (test-gen/create-job-store! {} {:from employer-address})
    (job-store/with-ethlance-job-store (job-factory/job-store-by-index 0)
      (is (= (job-store/employer-address) employer-address))
      (job-store/request-arbiter! arbiter-address {:from arbiter-address})
      (job-store/request-arbiter! arbiter-address {:from employer-address})
      (is (= (job-store/accepted-arbiter) arbiter-address))

      ;; Fund the Job Contract
      (job-store/fund! {:from employer-address :value (web3/to-wei 50.0 :ether)})

      ;; Create a work contract, and assign a candidate
      (job-store/request-work-contract! candidate-address {:from candidate-address})
      (work-contract/with-ethlance-work-contract (job-store/work-contract-by-index 0)
        (work-contract/request-invite! {:from employer-address})
        (is (= (work-contract/candidate-address) candidate-address))
        (work-contract/proceed! {:from employer-address})

        ;; Leave feedback for the employer as the candidate
        (is (not (registry/has-feedback? (job-store/work-contract-by-index 0))))
        (work-contract/leave-feedback! 4 comment-hash-1 {:from candidate-address}) 
        (is (registry/has-feedback? (job-store/work-contract-by-index 0)))

        (feedback/with-ethlance-feedback (registry/feedback-by-address (job-store/work-contract-by-index 0))
          (is (= (feedback/count) 1))
          (let [{:keys [from-user-address
                        from-user-type
                        to-user-address
                        to-user-type
                        metahash
                        rating
                        date-updated]}
                (feedback/feedback-by-index 0)]
            (is (= from-user-address candidate-address))
            (is (= from-user-type ::enum.user-type/candidate))
            (is (= to-user-address employer-address))
            (is (= to-user-type ::enum.user-type/employer))
            (is (= metahash comment-hash-1))
            (is (= rating 4))
            (is (> date-updated 0))))))))


(deftest-smart-contract main-feedback-dispute {}
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

        comment-hash-1 "test1"
        comment-hash-2 "test2"]

    ;; Create a Job Store, assign an accepted arbiter, and fund it.
    (test-gen/create-job-store! {} {:from employer-address})
    (job-store/with-ethlance-job-store (job-factory/job-store-by-index 0)
      (is (= (job-store/employer-address) employer-address))
      (job-store/request-arbiter! arbiter-address {:from arbiter-address})
      (job-store/request-arbiter! arbiter-address {:from employer-address})
      (is (= (job-store/accepted-arbiter) arbiter-address))

      ;; Fund the Job Contract
      (job-store/fund! {:from employer-address :value (web3/to-wei 50.0 :ether)})

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
        (is (bn/= (work-contract/dispute-count) 1))

        ;; Resolve the dispute
        (dispute/with-ethlance-dispute (work-contract/dispute-by-index 0)

          ;; Leave feedback for the employer as the candidate
          (is (not (registry/has-feedback? (work-contract/dispute-by-index 0))))
          (dispute/leave-feedback! 4 comment-hash-1 {:from candidate-address}) 
          (is (registry/has-feedback? (work-contract/dispute-by-index 0)))

          (feedback/with-ethlance-feedback (registry/feedback-by-address (work-contract/dispute-by-index 0))
            (is (= (feedback/count) 1))
            (let [{:keys [from-user-address
                          from-user-type
                          to-user-address
                          to-user-type
                          metahash
                          rating
                          date-updated]}
                  (feedback/feedback-by-index 0)]
              (is (= from-user-address candidate-address))
              (is (= from-user-type ::enum.user-type/candidate))
              (is (= to-user-address arbiter-address))
              (is (= to-user-type ::enum.user-type/arbiter))
              (is (= metahash comment-hash-1))
              (is (= rating 4))
              (is (> date-updated 0)))))))))
