(ns ethlance.server.contract.ethlance-job-test
  "Unit Tests for EthlanceJob wrapper."
  (:require
   [bignumber.core :as bn]
   [clojure.test :refer [deftest is are testing use-fixtures]]
   [cljs-web3.eth :as web3-eth]
   [taoensso.timbre :as log]

   [district.server.web3 :refer [web3]]
   [district.server.smart-contracts :as contracts]

   [ethlance.server.contract.ethlance-user-factory :as user-factory]
   [ethlance.server.contract.ethlance-user :as user]
   [ethlance.server.contract.ethlance-registry :as registry]
   [ethlance.server.contract.ethlance-job-factory :as job-factory]
   [ethlance.server.contract.ethlance-job :as job]
   [ethlance.server.contract.ds-guard :as ds-guard]
   [ethlance.server.test-utils :refer-macros [deftest-smart-contract]]))


(def sample-meta-hash-1 "QmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJH")
(def sample-meta-hash-2 "JmZJWGiKnqhmuuUNfcryiumVHCKGvVNZWdy7xtd3XCkQJ9")


(defn register-user!
  "Helper function for registering a user."
  [user-address meta-hash]
  (user-factory/register-user!
   {:metahash-ipfs meta-hash}
   {:from user-address}))


(def default-job-options
  "Bunch of helpful defaults for creating test job contracts."
  {:bid-option 0
   :estimated-length-seconds (* 8 60 60) ;; 8 Hours
   :include-ether-token? true
   :is-bounty? false
   :is-invitation-only? false
   :metahash-ipfs sample-meta-hash-1
   :reward-value 0})


(defn create-job!
  "Test Helper function for creating jobs with the
  `default-job-options`."
  [job-options & [opts]]
  (job-factory/create-job! (merge default-job-options job-options) opts))


(deftest-smart-contract job-contract {}
  (let [[employer-address candidate-address arbiter-address random-user-address]
        (web3-eth/accounts @web3)

        ;; Employer User
        tx-1 (register-user! employer-address "QmZhash1")
        _ (user/with-ethlance-user (user-factory/user-by-address employer-address)
            (user/register-employer! {:from employer-address}))

        ;; Candidate User
        tx-2 (register-user! candidate-address "QmZhash2")
        _ (user/with-ethlance-user (user-factory/user-by-address candidate-address)
            (user/register-candidate!
             {:hourly-rate 120
              :currency-type 1} ;; USD
             {:from candidate-address}))

        ;; Arbiter User
        tx-3 (register-user! arbiter-address "QmZhash3")
        _ (user/with-ethlance-user (user-factory/user-by-address arbiter-address)
            (user/register-arbiter!
             {:payment-value 3
              :currency-type 0 ;; ETH
              :type-of-payment 1} ;; Percent
             {:from arbiter-address}))]

    (testing "Create Job, and change metahash"
      (let [test-hash-1 "QmZ123"
            test-hash-2 "QmZ456"]
        (create-job!
         {:metahash-ipfs test-hash-1}
         {:from employer-address})
        
        (job/with-ethlance-job (job-factory/job-by-index 0)
          (is (= test-hash-1 (job/metahash-ipfs)))

          (job/update-metahash! test-hash-2)
          (is (= test-hash-2 (job/metahash-ipfs))))))))
