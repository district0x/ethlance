(ns ethlance.server.test-runner
  (:require
   [clojure.test :refer [deftest is are testing run-all-tests run-tests]]
   ;;[doo.runner :refer-macros [doo-tests]]

   ;; Test Namespaces
   [ethlance.server.core-test]
   [ethlance.server.contract.ethlance-user-factory-test]
   [ethlance.server.contract.ethlance-job-factory-test]
   [ethlance.server.contract.ethlance-user-test]))


(defn run-test
  "Run a single test in the given namespace `ns`"
  [ns]
  (run-tests ns))


(defn run-tests
  "Run tests, can be used within figwheel server instance."
  []
  (run-all-tests #"^ethlance.*-test$"))


(defn -test-main
  "Main Entrypoint."
  [& args]
  (run-all-tests #"^ethlance.*-test$"))


(set! *main-cli-fn* -test-main)
