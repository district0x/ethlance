(ns ethlance.server.contract.ethlance-invoice-test
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
   [ethlance.server.contract.ethlance-invoice :as invoice]
   [ethlance.server.test-utils :refer-macros [deftest-smart-contract-go]]
   [ethlance.server.contract.test-generators :as test-gen]

   [ethlance.shared.enum.bid-option :as enum.bid-option]
   [ethlance.shared.enum.currency-type :as enum.currency]
   [ethlance.shared.enum.payment-type :as enum.payment]
   [ethlance.shared.enum.contract-status :as enum.status]
   [ethlance.shared.async-utils :refer [<!-<log <!-<throw flush! go-try] :include-macros true]))


(deftest-smart-contract-go main-invoice {}
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

    (is (= (<!-<throw (job-store/employer-address job-address)) employer-address))
    (<!-<throw (job-store/request-arbiter! job-address arbiter-address {:from arbiter-address}))
    (<!-<throw (job-store/request-arbiter! job-address arbiter-address {:from employer-address}))
    (is (= (<!-<throw (job-store/accepted-arbiter job-address)) arbiter-address))

    ;; Create the initial work contract as the candidate
    (log/debug "Requesting Work Contract...")
    (<!-<throw (job-store/request-work-contract! job-address candidate-address {:from candidate-address}))
    (let [work-address (<!-<throw (job-store/work-contract-by-index job-address 0))]
      (is (= candidate-address (<!-<throw (work-contract/candidate-address work-address))))
      (is (= ::enum.status/request-candidate-invite (<!-<throw (work-contract/contract-status work-address))))
      
      ;; Invite the candidate as the employer
      (log/debug "Accepting Work Contract...")
      (<!-<throw (work-contract/request-invite! work-address {:from employer-address}))
      (is (= ::enum.status/accepted) (<!-<throw (work-contract/contract-status work-address)))

      ;; Proceed with the work contract
      (log/debug "Proceed with Work Contract...")
      (<!-<throw (work-contract/proceed! work-address {:from employer-address}))
      (is (= ::enum.status/in-progress (<!-<throw (work-contract/contract-status work-address)))))))


(deftest-smart-contract-go invoice-payment {}
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


    (is (= (<!-<throw (job-store/employer-address job-address)) employer-address))
    (<!-<throw (job-store/request-arbiter! job-address arbiter-address {:from arbiter-address}))
    (<!-<throw (job-store/request-arbiter! job-address arbiter-address {:from employer-address}))
    (is (= (<!-<throw (job-store/accepted-arbiter job-address)) arbiter-address))
    
    (let [job-employer-funding (web3/to-wei 20.0 :ether)]
      (is (bn/= (web3-eth/get-balance @web3 job-address) 0))

      ;; Employer funding the job store.
      (log/debug "Funding Job Store...")
      (<!-<throw (job-store/fund! job-address {:from employer-address :value job-employer-funding}))
      (is (bn/= (web3-eth/get-balance @web3 job-address) job-employer-funding)))

    (log/debug "Requesting Work Contract...")

    ;; Create the initial work contract as the candidate
    (<!-<throw (job-store/request-work-contract! job-address candidate-address {:from candidate-address}))
    (let [work-address (<!-<throw (job-store/work-contract-by-index job-address 0))]
      (is (= candidate-address (<!-<throw (work-contract/candidate-address work-address))))
      (is (= ::enum.status/request-candidate-invite (<!-<throw (work-contract/contract-status work-address))))
      
      (log/debug "Accept the contract as the employer...")
      (<!-<throw (work-contract/request-invite! work-address {:from employer-address}))
      (is (= ::enum.status/accepted) (<!-<throw (work-contract/contract-status work-address)))

      (log/debug "Proceed with the work contract...")
      (<!-<throw (work-contract/proceed! work-address {:from employer-address}))
      (is (= ::enum.status/in-progress (<!-<throw (work-contract/contract-status work-address)))))
    
    (testing "Create invoice, pay invoice"
      (let [work-address (<!-<throw (job-store/work-contract-by-index job-address 0))]
        (is (bn/= (<!-<throw (work-contract/invoice-count work-address)) 0))
        (log/debug "Create an Invoice...")
        (<!-<throw
         (work-contract/create-invoice!
          work-address
          {:amount (web3/to-wei 1.0 :ether) :metahash ""}
          {:from candidate-address}))
        (is (bn/= (<!-<throw (work-contract/invoice-count work-address)) 1))
        
        (log/debug "Create a Second Invoice...")
        (<!-<throw
         (work-contract/create-invoice!
          work-address
          {:amount (web3/to-wei 1.0 :ether) :metahash ""}
          {:from candidate-address}))
        (is (bn/= (<!-<throw (work-contract/invoice-count work-address)) 2))

        ;; Get the employer to pay the first invoice
        (let [invoice-address (<!-<throw (work-contract/invoice-by-index work-address 0))
              paid-amount (web3/to-wei 1.0 :ether)
              candidate-balance (web3-eth/get-balance @web3 candidate-address)]
          (is (not (<!-<throw (invoice/paid? invoice-address))))
          (log/debug "Pay for the first invoice...")
          (<!-<throw (invoice/pay! invoice-address paid-amount {:from employer-address}))
          (is (<!-<throw (invoice/paid? invoice-address)))

          ;; Candidate should have received the balance
          (is (bn/= (bn/+ candidate-balance paid-amount)
                    (web3-eth/get-balance @web3 candidate-address))))
        
        ;; Get the employer to pay the second invoice
        (let [invoice-address (<!-<throw (work-contract/invoice-by-index work-address 1))
              paid-amount (web3/to-wei 1.0 :ether)
              candidate-balance (web3-eth/get-balance @web3 candidate-address)]
          (is (not (<!-<throw (invoice/paid? invoice-address))))
          (log/debug "Pay for the second invoice...")
          (<!-<throw (invoice/pay! invoice-address paid-amount {:from employer-address}))
          (is (<!-<throw (invoice/paid? invoice-address)))

          ;; Candidate should have received the balance
          (is (bn/= (bn/+ candidate-balance paid-amount)
                    (web3-eth/get-balance @web3 candidate-address))))))))
