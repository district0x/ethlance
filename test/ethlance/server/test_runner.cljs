(ns ethlance.server.test-runner
  (:require
   [clojure.test :refer [deftest is are testing run-tests]]
   [orchestra-cljs.spec.test :as st]

   ;; Mount Components
   [district.server.logging]

   ;;
   ;; Test Namespaces
   ;;

   ;; Contract Tests
   [ethlance.server.contract.token-test]
   
   ;; Database Tests
   #_[ethlance.server.db-test]
   
   ;; Misc Tests
   [ethlance.server.core-test]
   [ethlance.server.ipfs-test]
   [ethlance.shared.random-test]
   [ethlance.shared.spec-utils-test]
   [ethlance.shared.enumeration.currency-type-test]
   [ethlance.shared.enumeration.payment-type-test]
   [ethlance.shared.enumeration.bid-option-test]))


(defn run-all-tests
  "Run tests, can be used within figwheel server instance."
  []

  (run-tests

   ;; Basic Tests
   'ethlance.server.core-test
   'ethlance.server.ipfs-test
   'ethlance.shared.random-test
   'ethlance.shared.spec-utils-test
   'ethlance.shared.enumeration.currency-type-test
   'ethlance.shared.enumeration.payment-type-test
   'ethlance.shared.enumeration.bid-option-test

   ;; Database Tests
   #_'ethlance.server.db-test

   ;; Smart Contract Tests
   'ethlance.server.contract.token-test))



(defn -test-main
  "Main Entrypoint."
  [& args]
  ;; Enable Instrumentation
  (st/instrument)
  (run-all-tests))


(set! *main-cli-fn* -test-main)
