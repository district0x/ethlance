(ns cljs.tests
  "Place to put specific test cases to run"
  (:require
   [clojure.test :refer [run-tests]]
   [ethlance.server.contract.token-test]
   [ethlance.server.db-test]
   [ethlance.server.ipfs-test]
   [ethlance.shared.random-test]
   [ethlance.shared.enumeration.currency-type-test]
   [ethlance.shared.enumeration.payment-type-test]
   [ethlance.shared.enumeration.bid-option-test]))


(defn run-enum-tests []
  (run-tests
   'ethlance.shared.enumeration.currency-type-test
   'ethlance.shared.enumeration.payment-type-test
   'ethlance.shared.enumeration.bid-option-test))


(defn run-db-tests []
  (.nextTick js/process
             #(run-tests
               'ethlance.server.db-test)))


(defn run-ipfs-tests []
  (.nextTick js/process
             #(run-tests
               'ethlance.server.ipfs-test)))


(defn run-random-tests []
  (.nextTick js/process
             #(run-tests
               'ethlance.shared.random-test)))


(defn run-token-tests []
  (.nextTick js/process
             #(run-tests
               'ethlance.server.contract.token-test)))

