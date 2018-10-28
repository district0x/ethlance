(ns cljs.tests
  "Place to put specific test cases to run"
  (:require
   [clojure.test :refer [run-tests]]
   [ethlance.server.contract.ethlance-job-factory-test]
   [ethlance.shared.enum.currency-type-test]))


(defn run-enum-tests []
  (run-tests
   'ethlance.shared.enum.currency-type-test))


(defn run-job-factory-tests []
  (.nextTick js/process
             #(run-tests
               'ethlance.server.contract.ethlance-job-factory-test)))
