(ns ethlance.server.contract.multi-linked-list-test
  (:require
   [bignumber.core :as bn]
   [clojure.test :refer [deftest is are testing use-fixtures]]
   [cljs-web3.core :refer [from-ascii]]
   [cljs-web3.eth :as web3-eth]
   [taoensso.timbre :as log]
   [cuerdas.core :as str]

   [district.server.web3 :refer [web3]]
   [district.server.smart-contracts :as contracts]

   [ethlance.server.test-utils :refer-macros [deftest-smart-contract]]
   [ethlance.server.contract.multi-linked-list :as mll :refer [sha3] :include-macros true]))


(def test-address-1
  "0xbeefbeefbeefbeefbeefbeefbeefbeefbeefbeef")


(def test-address-2
  "0xdabadabadabadabadabadabadabadabadabadaba")


(def test-address-3
  "0xabcdabcdabcdabcdabcdabcdabcdabcdabcdabcd")


(def test-address-4
  "0xdabbdabbdabbdabbdabbdabbdabbdabbdabbdabb")


(deftest-smart-contract main-mll-tests {}
  (mll/with-multi-linked-list :test-multi-linked-list
    (let [[user] (web3-eth/accounts @web3)
          test-key (sha3 "test")]
      (is (bn/= (mll/count test-key) 0))
      (mll/push! test-key test-address-1 {:from user})
      (is (bn/= (mll/count test-key) 1))
      (mll/push! test-key test-address-2 {:from user})
      (is (bn/= (mll/count test-key) 2))
      (mll/push! test-key test-address-3 {:from user})
      (is (bn/= (mll/count test-key) 3))

      (is (= test-address-1 (-> (mll/first test-key) mll/value)))
      (is (= test-address-1 (mll/first-value test-key)))
      (is (= test-address-1 (mll/nth test-key 0)))

      (is (= test-address-2 (-> (mll/second test-key) mll/value)))
      (is (= test-address-2 (mll/second-value test-key)))
      (is (= test-address-2 (mll/nth test-key 1)))

      (is (= test-address-3 (-> (mll/last test-key) mll/value)))
      (is (= test-address-3 (mll/last-value test-key)))
      (is (= test-address-3 (mll/nth test-key 2))))))


(deftest-smart-contract multi-mll-tests {}
  (mll/with-multi-linked-list :test-multi-linked-list
    (let [[user] (web3-eth/accounts @web3)
          test-key-1 (sha3 "test")
          test-key-2 (sha3 "test2")]

      (is (bn/= (mll/count test-key-1) 0))
      (is (bn/= (mll/count test-key-2) 0))

      (mll/push! test-key-1 test-address-1 {:from user})
      (mll/push! test-key-2 test-address-3 {:from user})

      (is (bn/= (mll/count test-key-1) 1))
      (is (bn/= (mll/count test-key-2) 1))

      (mll/push! test-key-1 test-address-2 {:from user})
      (mll/push! test-key-2 test-address-1 {:from user})

      (is (bn/= (mll/count test-key-1) 2))
      (is (bn/= (mll/count test-key-2) 2))

      (mll/push! test-key-1 test-address-3 {:from user})
      (mll/push! test-key-2 test-address-1 {:from user})

      (is (bn/= (mll/count test-key-1) 3))
      (is (bn/= (mll/count test-key-2) 3))

      (is (= test-address-1 (-> (mll/first test-key-1) mll/value)))
      (is (= test-address-1 (mll/first-value test-key-1)))
      (is (= test-address-1 (mll/nth test-key-1 0)))

      (is (= test-address-2 (-> (mll/second test-key-1) mll/value)))
      (is (= test-address-2 (mll/second-value test-key-1)))
      (is (= test-address-2 (mll/nth test-key-1 1)))

      (is (= test-address-3 (-> (mll/last test-key-1) mll/value)))
      (is (= test-address-3 (mll/last-value test-key-1)))
      (is (= test-address-3 (mll/nth test-key-1 2))))))


(deftest-smart-contract multi-mll-tests-2 {}
  (mll/with-multi-linked-list :test-multi-linked-list
    (let [[user] (web3-eth/accounts @web3)
          test-key (sha3 "test")]

      (is (bn/= (mll/count test-key) 0))
      (mll/insert! test-key 0 test-address-1 {:from user})
      (is (bn/= (mll/count test-key) 1))
      (mll/remove! test-key 0 {:from user})
      (is (bn/= (mll/count test-key) 0)))))
