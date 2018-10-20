(ns cljs.tests
  "Place to put specific test cases to run"
  (:require
   [clojure.test :refer [run-tests]]
   [ethlance.shared.type.currency-test]))


(defn run-type-tests []
  (run-tests
   'ethlance.shared.type.currency-test))
