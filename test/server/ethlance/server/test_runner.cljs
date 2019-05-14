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
   [ethlance.server.contract.ethlance-user-factory-test]
   [ethlance.server.contract.ethlance-user-test]
   [ethlance.server.contract.ethlance-job-factory-test]
   [ethlance.server.contract.ethlance-job-store-test]
   [ethlance.server.contract.ethlance-work-contract-test]
   [ethlance.server.contract.ethlance-invoice-test]
   [ethlance.server.contract.ethlance-dispute-test]
   [ethlance.server.contract.ethlance-comment-test]
   [ethlance.server.contract.ethlance-feedback-test]
   [ethlance.server.contract.ethlance-token-store-test]

   
   ;; Database Tests
   [ethlance.server.db-test]
   [ethlance.server.model.user-test]
   [ethlance.server.model.candidate-test]
   [ethlance.server.model.arbiter-test]
   [ethlance.server.model.employer-test]
   [ethlance.server.model.job-test]
   [ethlance.server.model.comment-test]
   [ethlance.server.model.feedback-test]
   
   ;; Misc Tests
   [ethlance.server.core-test]
   [ethlance.server.ipfs-test]
   [ethlance.shared.random-test]
   [ethlance.shared.spec-utils-test]
   [ethlance.shared.enumeration.currency-type-test]
   [ethlance.shared.enumeration.payment-type-test]
   [ethlance.shared.enumeration.bid-option-test]
   [ethlance.shared.enumeration.contract-status-test]))


(defn run-all-tests
  "Run tests, can be used within figwheel server instance."
  []
  (let [all-tests (concat smart-contract-tests database-tests misc-tests)]
    ;;(apply run-tests all-tests)))
    (run-tests

     ;; Basic Tests
     'ethlance.server.core-test
     'ethlance.server.ipfs-test
     'ethlance.shared.random-test
     'ethlance.shared.spec-utils-test
     'ethlance.shared.enumeration.currency-type-test
     'ethlance.shared.enumeration.payment-type-test
     'ethlance.shared.enumeration.bid-option-test
     'ethlance.shared.enumeration.contract-status-test

     ;; Database Tests
     'ethlance.server.db-test
     'ethlance.server.model.user-test
     'ethlance.server.model.candidate-test
     'ethlance.server.model.arbiter-test
     'ethlance.server.model.employer-test
     'ethlance.server.model.job-test
     'ethlance.server.model.comment-test
     'ethlance.server.model.feedback-test

     ;; Smart Contract Tests
     'ethlance.server.contract.token-test
     'ethlance.server.contract.ethlance-user-factory-test
     'ethlance.server.contract.ethlance-user-test
     'ethlance.server.contract.ethlance-job-factory-test
     'ethlance.server.contract.ethlance-job-store-test
     'ethlance.server.contract.ethlance-work-contract-test
     'ethlance.server.contract.ethlance-invoice-test
     'ethlance.server.contract.ethlance-dispute-test
     'ethlance.server.contract.ethlance-comment-test
     'ethlance.server.contract.ethlance-feedback-test
     'ethlance.server.contract.ethlance-token-store-test)))


(defn -test-main
  "Main Entrypoint."
  [& args]
  ;; Enable Instrumentation
  (st/instrument)
  (run-all-tests))


(set! *main-cli-fn* -test-main)
