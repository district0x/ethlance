(ns ethlance.server.test-runner
  (:require
   [clojure.test :refer [deftest is are testing run-all-tests]]
   ;;[doo.runner :refer-macros [doo-tests]]

   ;; Test Namespaces
   [ethlance.server.core-test]
   [ethlance.server.contract.ethlance-user-factory-test]))


(defn run-tests
  "Run tests, can be used within figwheel server instance."
  []
  (run-all-tests #"^ethlance.*-test$"))


(defn -test-main
  "Main Entrypoint."
  [& args]
  (run-all-tests #"^ethlance.*-test$"))


(set! *main-cli-fn* -test-main)
