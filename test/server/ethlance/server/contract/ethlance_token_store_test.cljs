(ns ethlance.server.contract.ethlance-token-store-test
  (:require
   [bignumber.core :as bn]
   [clojure.test :refer [deftest is are testing use-fixtures]]
   [cljs-web3.core :as web3]
   [cljs-web3.eth :as web3-eth]
   [taoensso.timbre :as log]

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
   [ethlance.server.contract.ethlance-invoice :as invoice]
   [ethlance.server.contract.ethlance-comment :as comment]
   [ethlance.server.contract.ethlance-token-store :as token-store]
   [ethlance.server.test-utils :refer-macros [deftest-smart-contract-go]]
   [ethlance.server.contract.test-generators :as test-gen]

   [ethlance.shared.enumeration.bid-option :as enum.bid-option]
   [ethlance.shared.enumeration.currency-type :as enum.currency]
   [ethlance.shared.enumeration.payment-type :as enum.payment]
   [ethlance.shared.enumeration.contract-status :as enum.status]
   [ethlance.shared.enumeration.user-type :as enum.user-type]))


(def null-address "0x0000000000000000000000000000000000000000")


;;(deftest-smart-contract-go job-store-token-store {})
