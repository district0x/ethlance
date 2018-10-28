(ns ethlance.server.contract.test-generators
  "Includes functions for generating different smart contract entities
  for test case purposes."
  (:require
   [ethlance.server.contract.ethlance-user-factory :as user-factory]
   [ethlance.server.contract.ethlance-user :as user]
   [ethlance.server.contract.ethlance-job-factory :as job-factory]
   [ethlance.server.contract.ethlance-job-store :as job-store]
   [ethlance.shared.enum.bid-option :as enum.bid-option]))

  
(defn register-user!
  "Helper function for registering a user."
  [user-address metahash]
  (user-factory/register-user!
   {:metahash-ipfs metahash}
   {:from user-address}))


(def default-job-options
  "Bunch of helpful defaults for creating test job contracts."
  {:bid-option ::enum.bid-option/fixed-price
   :estimated-length-seconds (* 8 60 60) ;; 8 Hours
   :include-ether-token? true
   :is-invitation-only? false
   :metahash "QmZTESTHASH"
   :reward-value 0})


(defn create-job-store!
  "Test Helper function for creating jobs with `default-job-options`."
  [job-options & [opts]]
  (job-factory/create-job-store! (merge default-job-options job-options) opts))
