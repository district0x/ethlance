(ns ethlance.server.contract.test-token-test
  "Unit Tests for TestToken wrapper."
  (:require
   [bignumber.core :as bn]
   [clojure.test :refer [deftest is are testing use-fixtures]]
   [cljs-web3.eth :as web3-eth]
   [taoensso.timbre :as log]

   [district.server.web3 :refer [web3]]
   [district.server.smart-contracts :as contracts]
   [ethlance.server.contract.test-token :as test-token]
   [ethlance.server.test-utils :refer-macros [deftest-smart-contract]]))


(deftest-smart-contract main-test-token {}
  (testing "Main Tests"
    (is (not= 0 1))))


