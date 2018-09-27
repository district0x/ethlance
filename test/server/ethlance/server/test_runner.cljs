(ns ethlance.server.test-runner
  (:require
   [clojure.test :refer [deftest is are testing]]
   [doo.runner :refer-macros [doo-tests]]

   ;; Test Namespaces
   [ethlance.server.core-test]))


(doo-tests
 'ethlance.server.core-test)
