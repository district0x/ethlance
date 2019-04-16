(ns ethlance.server.contract.ethlance-dispute-test
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
   [ethlance.server.test-utils :refer-macros [deftest-smart-contract-go]]
   [ethlance.server.contract.test-generators :as test-gen]

   [ethlance.shared.enum.bid-option :as enum.bid-option]
   [ethlance.shared.enum.currency-type :as enum.currency]
   [ethlance.shared.enum.payment-type :as enum.payment]
   [ethlance.shared.async-utils :refer [<!-<log <!-<throw flush! go-try] :include-macros true]))


(def null-address "0x0000000000000000000000000000000000000000")


(deftest-smart-contract-go main-dispute {}
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

    ;; Request the accepted arbiter
    (log/debug "Requesting Arbiter...")
    (<!-<throw (job-store/request-arbiter! job-address arbiter-address {:from arbiter-address}))
    (<!-<throw (job-store/request-arbiter! job-address arbiter-address {:from employer-address}))

    ;; Fund the Job Contract
    (log/debug "Funding the Job Contract...")
    (<!-<throw (job-store/fund! job-address {:from employer-address :value (web3/to-wei 10.1 :ether)}))

    ;; Create a work contract, and assign a candidate
    (log/debug "Creating the Work Contract...")
    (<!-<throw (job-store/request-work-contract! job-address candidate-address {:from candidate-address}))
    (let [work-address (<!-<throw (job-store/work-contract-by-index job-address 0))]
      (log/debug "Accepting the work contract...")
      (<!-<throw (work-contract/request-invite! work-address {:from employer-address}))
      (log/debug "Proceeding with work contract...")
      (<!-<throw (work-contract/proceed! work-address {:from employer-address}))

      ;; Create a dispute
      (is (bn/= (<!-<throw (work-contract/dispute-count work-address)) 0))
      (log/debug "Creating the dispute...")
      (work-contract/create-dispute!
       work-address
       {:reason "For being 'testy'" :metahash ""}
       {:from employer-address})
      (is (bn/= (<!-<throw (work-contract/dispute-count work-address)) 1))

      ;; Resolve the dispute
      (log/debug "Resolving Dispute...")
      (let [dispute-address (<!-<throw (work-contract/dispute-by-index work-address 0))
            job-balance (web3-eth/get-balance @web3 job-address)
            employer-balance (web3-eth/get-balance @web3 employer-address)
            candidate-balance (web3-eth/get-balance @web3 candidate-address)
            arbiter-balance (web3-eth/get-balance @web3 arbiter-address)

            ;; Tested resolution amounts.
            resolved-employer-amount (web3/to-wei 5.0 :ether)
            resolved-candidate-amount (web3/to-wei 3.0 :ether)
            resolved-arbiter-amount (web3/to-wei 2.0 :ether)]
        
        (is (not (<!-<throw (dispute/resolved? dispute-address))))
        (log/debug "Resolving!")
        (dispute/resolve!
         dispute-address
         {:employer-amount resolved-employer-amount
          :candidate-amount resolved-candidate-amount
          :arbiter-amount resolved-arbiter-amount}
         {:from arbiter-address})
        (is (<!-<throw (dispute/resolved? dispute-address)))

        ;; Check balances
        (log/debug "Checking Balances...")
        (is (bn/= (web3-eth/get-balance @web3 job-address)
                  (reduce bn/- job-balance
                          [resolved-employer-amount
                           resolved-candidate-amount
                           resolved-arbiter-amount])))
        
        (is (bn/= (web3-eth/get-balance @web3 employer-address)
                  (bn/+ employer-balance resolved-employer-amount)))

        (is (bn/= (web3-eth/get-balance @web3 candidate-address)
                  (bn/+ candidate-balance resolved-candidate-amount)))))))

            ;; TODO: arbiter balance minus transaction cost
