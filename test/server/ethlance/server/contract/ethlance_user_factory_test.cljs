(ns ethlance.server.contract.ethlance-user-factory-test
  (:require
   [clojure.test :refer [deftest is are testing use-fixtures]]
   
   [ethlance.server.contract.ethlance-user-factory :as user-factory]
   [ethlance.server.test-utils :refer-macros [deftest-smart-contract]]))


;;(use-fixtures :each (with-smart-contract))


(deftest-smart-contract registering-user
  (println "Hello World!"))
