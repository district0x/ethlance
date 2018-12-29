(ns ethlance.server.contract.multi-linked-list-test
  (:require
   [bignumber.core :as bn]
   [clojure.test :refer [deftest is are testing use-fixtures]]
   [cljs-web3.eth :as web3-eth]
   [taoensso.timbre :as log]

   [district.server.web3 :refer [web3]]
   [district.server.smart-contracts :as contracts]

   [ethlance.server.test-utils :refer-macros [deftest-smart-contract]]
   [ethlance.server.contract.multi-linked-list :as mll :refer [sha3] :include-macros true]))


(def test-address-1
  "beefbeefbeefbeefbeefbeefbeefbeefbeefbeef")


(def test-address-2
  "dabadabadabadabadabadabadabadabadabadaba")


(def test-address-3
  "abcdabcdabcdabcdabcdabcdabcdabcdabcdabcd")


(def test-address-4
  "dabbdabbdabbdabbdabbdabbdabbdabbdabbdabb")


(deftest-smart-contract main-mll-tests {}
  (mll/with-multi-linked-list :test-multi-linked-list
    (let [test-key (sha3 "test")])))
      ;;(mll/push! test-key test-address-1)
      ;;(mll/push! test-key test-address-2))))


