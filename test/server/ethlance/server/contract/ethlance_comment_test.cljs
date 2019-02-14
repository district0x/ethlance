(ns ethlance.server.contract.ethlance-comment-test
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
   [ethlance.server.contract.ethlance-job-store :as job-store :include-macros true]
   [ethlance.server.contract.ethlance-work-contract :as work-contract :include-macros true]
   [ethlance.server.contract.ethlance-user-factory :as user-factory]
   [ethlance.server.contract.ethlance-user :as user :include-macros true]
   [ethlance.server.contract.ethlance-registry :as registry]
   [ethlance.server.contract.ds-guard :as ds-guard]
   [ethlance.server.contract.ethlance-dispute :as dispute :include-macros true]
   [ethlance.server.contract.ethlance-invoice :as invoice :include-macros true]
   [ethlance.server.contract.ethlance-comment :as comment :include-macros true]
   [ethlance.server.test-utils :refer-macros [deftest-smart-contract-go]]
   [ethlance.server.contract.test-generators :as test-gen]

   [ethlance.shared.enum.bid-option :as enum.bid-option]
   [ethlance.shared.enum.currency-type :as enum.currency]
   [ethlance.shared.enum.payment-type :as enum.payment]
   [ethlance.shared.enum.contract-status :as enum.status]
   [ethlance.shared.enum.user-type :as enum.user-type]
   [ethlance.shared.async-utils :refer [<!-<log <!-<throw flush! go-try] :include-macros true]))


(def null-address "0x0000000000000000000000000000000000000000")


(deftest-smart-contract-go main-work-contract-comment {}
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

      (log/debug "Adding a Comment...")
      (<!-<throw (work-contract/add-comment! work-address comment-hash-1 {:from employer-address}))

      (log/debug "Getting comment address...")
      (let [comment-address (<!-<throw (registry/comment-by-index work-address 0))]
        (is (= (<!-<throw (comment/user-type comment-address)) ::enum.user-type/employer))
        (is (= (<!-<throw (comment/user-address comment-address)) employer-address))
        (is (bn/= (<!-<throw (comment/count comment-address)) 1))
        (is (= (<!-<throw (comment/last comment-address)) comment-hash-1))
        (is (= (<!-<throw (comment/revision-by-index comment-address 0)) comment-hash-1))

        (log/debug "Updating Comment...")
        (<!-<throw (comment/update! comment-address comment-hash-2 {:from employer-address}))

        (is (bn/= (<!-<throw (comment/count comment-address)) 2))
        (is (= (<!-<throw (comment/last comment-address)) comment-hash-2))
        (is (= (<!-<throw (comment/revision-by-index comment-address 1)) comment-hash-2))))))


(deftest-smart-contract-go main-dispute-comment {}
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
    (<!-<throw (job-store/fund! job-address {:from employer-address :value (web3/to-wei 3.0 :ether)}))

    ;; Create a work contract, and assign a candidate
    (log/debug "Requesting Work Contract...")
    (<!-<throw (job-store/request-work-contract! job-address candidate-address {:from candidate-address}))
    (let [work-address (<!-<throw (job-store/work-contract-by-index job-address 0))]
      (log/debug "Accepting Work Contract...")
      (<!-<throw (work-contract/request-invite! work-address {:from employer-address}))
      
      (log/debug "Proceeding Work Contract...")
      (<!-<throw (work-contract/proceed! work-address {:from employer-address}))

      ;; Create a dispute
      (is (bn/= (<!-<throw (work-contract/dispute-count work-address)) 0))
      (<!-<throw
       (work-contract/create-dispute!
        work-address
        {:reason "For being 'testy'" :metahash ""}
        {:from employer-address}))
      (is (bn/= (<!-<throw (work-contract/dispute-count work-address)) 1))

      (let [dispute-address (<!-<throw (work-contract/dispute-by-index work-address 0))] 

        ;; Initial Dispute creation appends a comment
        (is (bn/= (<!-<throw (registry/comment-count dispute-address)) 1))
        (<!-<throw (dispute/add-comment! dispute-address comment-hash-1 {:from candidate-address}))
        (is (bn/= (<!-<throw (registry/comment-count dispute-address)) 2))
        
        ;; Tests on second comment
        (let [comment-address (<!-<throw (registry/comment-by-index dispute-address 1))]
          (is (= (<!-<throw (comment/user-type comment-address)) ::enum.user-type/candidate))
          (is (= (<!-<throw (comment/user-address comment-address)) candidate-address))
          (is (bn/= (<!-<throw (comment/count comment-address)) 1))
          (is (= (<!-<throw (comment/last comment-address)) comment-hash-1))
          (is (= (<!-<throw (comment/revision-by-index comment-address 0)) comment-hash-1))

          ;; Update Dispute Comment
          (<!-<throw (comment/update! comment-address comment-hash-2 {:from candidate-address}))
          (is (bn/= (<!-<throw (comment/count comment-address)) 2))
          (is (= (<!-<throw (comment/last comment-address)) comment-hash-2))
          (is (= (<!-<throw (comment/revision-by-index comment-address 1)) comment-hash-2)))))))


(deftest-smart-contract-go main-invoice-comment {}
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
      
      (testing "Create invoice, add comments"
        (log/debug "Create an Invoice...")
        (<!-<throw
         (work-contract/create-invoice!
          work-address
          {:amount (web3/to-wei 1.0 :ether) :metahash ""}
          {:from candidate-address}))

        (let [invoice-address (<!-<throw (work-contract/invoice-by-index work-address 0))]

          ;; Initial Invoice creation appends a comment
          (is (bn/= (<!-<throw (registry/comment-count invoice-address)) 1))
          (log/debug "Adding a Comment...")
          (<!-<throw (invoice/add-comment! invoice-address comment-hash-1 {:from arbiter-address}))
          (is (bn/= (<!-<throw (registry/comment-count invoice-address)) 2))

          ;; Tests on second comment
          (let [comment-address (<!-<throw (registry/comment-by-index invoice-address 1))]

            (log/debug "Testing Comment...")
            (is (= (<!-<throw (comment/user-type comment-address)) ::enum.user-type/arbiter))
            (is (= (<!-<throw (comment/user-address comment-address)) arbiter-address))
            (is (bn/= (<!-<throw (comment/count comment-address)) 1))
            (is (= (<!-<throw (comment/last comment-address)) comment-hash-1))
            (is (= (<!-<throw (comment/revision-by-index comment-address 0)) comment-hash-1))

            ;; Update Invoice Comment
            (<!-<throw (comment/update! comment-address comment-hash-2 {:from arbiter-address}))
            (is (bn/= (<!-<throw (comment/count comment-address)) 2))
            (is (= (<!-<throw (comment/last comment-address)) comment-hash-2))
            (is (= (<!-<throw (comment/revision-by-index comment-address 1)) comment-hash-2))))))))
