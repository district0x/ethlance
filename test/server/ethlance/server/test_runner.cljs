(ns ethlance.server.test-runner
  (:require
   [clojure.test :refer [deftest is are testing run-all-tests run-tests]]

   ;;
   ;; Test Namespaces
   ;;

   [ethlance.server.core-test]
   [ethlance.server.contract.ethlance-user-factory-test]
   [ethlance.server.contract.ethlance-user-test]
   [ethlance.server.contract.ethlance-job-factory-test]
   [ethlance.server.contract.ethlance-job-store-test]
   [ethlance.server.contract.ethlance-work-contract-test]
   [ethlance.server.contract.ethlance-invoice-test]
   [ethlance.server.contract.ethlance-dispute-test]

   [ethlance.server.db-test]
   [ethlance.server.model.user-test]
   [ethlance.server.model.candidate-test]
   [ethlance.server.model.arbiter-test]
   [ethlance.server.model.employer-test]
   [ethlance.server.model.job-test]
   
   [ethlance.server.ipfs-test]

   [ethlance.shared.random-test]
   [ethlance.shared.spec-utils-test]
   [ethlance.shared.enum.currency-type-test]
   [ethlance.shared.enum.payment-type-test]
   [ethlance.shared.enum.bid-option-test]
   [ethlance.shared.enum.contract-status-test]))


(defn run-test
  "Run all tests with the given namespace."
  [p]
  (run-tests p))


(defn run-tests
  "Run tests, can be used within figwheel server instance."
  []
  (run-all-tests #"^ethlance.*-test$"))


(defn -test-main
  "Main Entrypoint."
  [& args]
  (run-all-tests #"^ethlance.*-test$"))


(set! *main-cli-fn* -test-main)
