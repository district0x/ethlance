(ns ethlance.server.contract.ethlance-invoice-test
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
   [ethlance.server.contract.ethlance-invoice :as invoice :include-macros true]
   [ethlance.server.test-utils :refer-macros [deftest-smart-contract]]
   [ethlance.server.contract.test-generators :as test-gen]

   [ethlance.shared.enum.bid-option :as enum.bid-option]
   [ethlance.shared.enum.currency-type :as enum.currency]
   [ethlance.shared.enum.payment-type :as enum.payment]
   [ethlance.shared.enum.contract-status :as enum.status]))

(def null-address "0x0000000000000000000000000000000000000000")


(deftest-smart-contract main-invoice {}
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

    (log/debug "Creating Job Store...")
    (test-gen/create-job-store! {} {:from employer-address})
    (job-store/with-ethlance-job-store (job-factory/job-store-by-index 0)
      (is (= employer-address (job-store/employer-address)))

      ;; Create the initial work contract as the candidate
      (log/debug "Requesting Work Contract...")
      (job-store/request-work-contract! candidate-address {:from candidate-address})
      (work-contract/with-ethlance-work-contract (job-store/work-contract-by-index 0)
        (is (= candidate-address (work-contract/candidate-address)))
        (is (= ::enum.status/request-candidate-invite (work-contract/contract-status)))
        
        ;; Invite the candidate as the employer
        (log/debug "Accepting Work Contract...")
        (work-contract/request-invite! {:from employer-address})
        (is (= ::enum.status/accepted) (work-contract/contract-status))

        ;; Proceed with the work contract
        (log/debug "Proceed with Work Contract...")
        (work-contract/proceed! {:from employer-address})
        (is (= ::enum.status/in-progress (work-contract/contract-status)))))))


(deftest-smart-contract invoice-payment {}
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

    (log/debug "Creating Job Store...")
    (test-gen/create-job-store! {} {:from employer-address})
    (job-store/with-ethlance-job-store (job-factory/job-store-by-index 0)
      (is (= employer-address (job-store/employer-address))))
    
    (let [job-employer-funding (web3/to-wei 20.0 :ether)
          job-address (job-factory/job-store-by-index 0)]
      (is (bn/= (web3-eth/get-balance @web3 job-address) 0))

      ;; Employer funding the job store.
      (log/debug "Funding Job Store...")
      (job-store/with-ethlance-job-store job-address
        (job-store/fund! {:from employer-address :value job-employer-funding}))
      (is (bn/= (web3-eth/get-balance @web3 job-address) job-employer-funding)))

    (log/debug "Requesting Work Contract...")
    (job-store/with-ethlance-job-store (job-factory/job-store-by-index 0)
      ;; Create the initial work contract as the candidate
      (job-store/request-work-contract! candidate-address {:from candidate-address})
      (work-contract/with-ethlance-work-contract (job-store/work-contract-by-index 0)
        (is (= candidate-address (work-contract/candidate-address)))
        (is (= ::enum.status/request-candidate-invite (work-contract/contract-status)))
        
        (log/debug "Accept the contract as the employer...")
        (work-contract/request-invite! {:from employer-address})
        (is (= ::enum.status/accepted) (work-contract/contract-status))

        (log/debug "Proceed with the work contract...")
        (work-contract/proceed! {:from employer-address})
        (is (= ::enum.status/in-progress (work-contract/contract-status)))))
    
    (testing "Create invoice, pay invoice"
      (job-store/with-ethlance-job-store (job-factory/job-store-by-index 0)
        (work-contract/with-ethlance-work-contract (job-store/work-contract-by-index 0)
          (is (bn/= (work-contract/invoice-count) 0))
          (log/debug "Create an Invoice...")
          (work-contract/create-invoice!
           {:amount (web3/to-wei 1.0 :ether) :metahash ""}
           {:from candidate-address})
          (is (bn/= (work-contract/invoice-count) 1))
          
          (log/debug "Create a Second Invoice...")
          (work-contract/create-invoice!
           {:amount (web3/to-wei 1.0 :ether) :metahash ""}
           {:from candidate-address})
          (is (bn/= (work-contract/invoice-count) 2))

          ;; Get the employer to pay the first invoice
          (invoice/with-ethlance-invoice (work-contract/invoice-by-index 0)
            (let [paid-amount (web3/to-wei 1.0 :ether)
                  candidate-balance (web3-eth/get-balance @web3 candidate-address)]
              (is (not (invoice/paid?)))
              (log/debug "Pay for the first invoice...")
              (invoice/pay! paid-amount {:from employer-address})
              (is (invoice/paid?))

              ;; Candidate should have received the balance
              (is (bn/= (bn/+ candidate-balance paid-amount)
                        (web3-eth/get-balance @web3 candidate-address)))))
          
          ;; Get the employer to pay the second invoice
          (invoice/with-ethlance-invoice (work-contract/invoice-by-index 1)
            (let [paid-amount (web3/to-wei 1.0 :ether)
                  candidate-balance (web3-eth/get-balance @web3 candidate-address)]
              (is (not (invoice/paid?)))
              (log/debug "Pay for the second invoice...")
              (invoice/pay! paid-amount {:from employer-address})
              (is (invoice/paid?))

              ;; Candidate should have received the balance
              (is (bn/= (bn/+ candidate-balance paid-amount)
                        (web3-eth/get-balance @web3 candidate-address))))))))))
