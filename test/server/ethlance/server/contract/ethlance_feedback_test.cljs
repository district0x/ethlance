(ns ethlance.server.contract.ethlance-feedback-test
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
   [ethlance.server.contract.ethlance-dispute :as dispute]
   [ethlance.server.contract.ethlance-feedback :as feedback]
   [ethlance.server.test-utils :refer-macros [deftest-smart-contract-go]]
   [ethlance.server.contract.test-generators :as test-gen]

   [ethlance.shared.enum.bid-option :as enum.bid-option]
   [ethlance.shared.enum.currency-type :as enum.currency]
   [ethlance.shared.enum.payment-type :as enum.payment]
   [ethlance.shared.enum.user-type :as enum.user-type]
   [ethlance.shared.async-utils :refer [<!-<log <!-<throw flush! go-try] :include-macros true]))


(deftest-smart-contract-go main-feedback-test {}
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
        job-address (<!-<throw (job-factory/job-store-by-index 0))

        comment-hash-1 "test1"
        comment-hash-2 "test2"]

    ;; Request the accepted arbiter
    (log/debug "Requesting Arbiter...")
    (<!-<throw (job-store/request-arbiter! job-address arbiter-address {:from arbiter-address}))
    (<!-<throw (job-store/request-arbiter! job-address arbiter-address {:from employer-address}))

    ;; Fund the Job Contract
    (log/debug "Funding Job Contract...")
    (<!-<throw (job-store/fund! job-address {:from employer-address :value (web3/to-wei 1.0 :ether)}))

    ;; Create a work contract, and assign a candidate
    (log/debug "Requesting Work Contract...")
    (<!-<throw (job-store/request-work-contract! job-address candidate-address {:from candidate-address}))
    (let [work-address (<!-<throw (job-store/work-contract-by-index job-address 0))]
      (log/debug "Accepting Work Contract...")
      (<!-<throw (work-contract/request-invite! work-address {:from employer-address}))
      
      (log/debug "Proceeding Work Contract...")
      (<!-<throw (work-contract/proceed! work-address {:from employer-address}))

      ;; Leave feedback for the employer as the candidate
      (is (not (<!-<throw (registry/has-feedback? work-address))))
      (log/debug "Leaving Feedback on work contract as candidate (Feedback #1)...")
      (<!-<throw (work-contract/leave-feedback! work-address 4 comment-hash-1 {:from candidate-address}))
      (is (<!-<throw (registry/has-feedback? work-address)))

      (log/debug "Inspecting Feedback #1...")
      (let [feedback-address (<!-<throw (registry/feedback-by-address work-address))
            {:keys [from-user-address
                    from-user-type
                    to-user-address
                    to-user-type
                    metahash
                    rating
                    date-updated]}
            (<!-<throw (feedback/feedback-by-index feedback-address 0))]
        (is (bn/= (<!-<throw (feedback/count feedback-address)) 1))
        (is (= from-user-address candidate-address))
        (is (= from-user-type ::enum.user-type/candidate))
        (is (= to-user-address employer-address))
        (is (= to-user-type ::enum.user-type/employer))
        (is (= metahash comment-hash-1))
        (is (= rating 4))
        (is (> date-updated 0)))

      (log/debug "Leaving Feedback on work contract as employer (Feedback #2)...")
      (<!-<throw (work-contract/leave-feedback! work-address 1 comment-hash-2 {:from employer-address}))

      (log/debug "Inspecting Feedback #2...")
      (let [feedback-address (<!-<throw (registry/feedback-by-address work-address))
            {:keys [from-user-address
                    from-user-type
                    to-user-address
                    to-user-type
                    metahash
                    rating
                    date-updated]}
            (<!-<throw (feedback/feedback-by-index feedback-address 1))]
        (is (bn/= (<!-<throw (feedback/count feedback-address)) 2))
        (is (= from-user-address employer-address))
        (is (= from-user-type ::enum.user-type/employer))
        (is (= to-user-address candidate-address))
        (is (= to-user-type ::enum.user-type/candidate))
        (is (= metahash comment-hash-2))
        (is (= rating 1))
        (is (> date-updated 0)))

      ;; Change the rating the employer gave to the candidate
      (log/debug "Updating Feedback on work contract as employer (Feedback #2)")
      (<!-<throw (work-contract/leave-feedback! work-address 2 comment-hash-1 {:from employer-address}))

      ;; For some reason causes stack overflow?
      ;; FIXME: determine whether this is still broken
      #_(log/debug "Inspecting Updated Feedback #3...")
      #_(let [feedback-address (<!-<throw (registry/feedback-by-address work-address))
              {:keys [from-user-address
                      from-user-type
                      to-user-address
                      to-user-type
                      metahash
                      rating
                      date-updated]}
              (<!-<throw (feedback/feedback-by-index feedback-address 1))]
          (is (bn/= (<!-<throw (feedback/count feedback-address)) 2))
          (is (= from-user-address employer-address))
          (is (= from-user-type ::enum.user-type/employer))
          (is (= to-user-address candidate-address))
          (is (= to-user-type ::enum.user-type/candidate))
          (is (= metahash comment-hash-1))
          (is (= rating 2))
          (is (> date-updated 0))))))
