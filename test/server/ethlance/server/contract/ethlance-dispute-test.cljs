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
   [ethlance.server.contract.ethlance-dispute :as dispute]
   [ethlance.server.test-utils :refer-macros [deftest-smart-contract]]
   [ethlance.server.contract.test-generators :as test-gen]

   [ethlance.shared.enum.bid-option :as enum.bid-option]
   [ethlance.shared.enum.currency-type :as enum.currency]
   [ethlance.shared.enum.payment-type :as enum.payment]))

(def null-address "0x0000000000000000000000000000000000000000")


(deftest-smart-contract main-dispute {})
