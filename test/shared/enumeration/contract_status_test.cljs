(ns ethlance.shared.enumeration.contract-status-test
  (:require
   [clojure.test :refer [deftest is are testing]]
   [ethlance.shared.enumeration.contract-status :as enum.status]))


(deftest contract-status-tests
  (is (= (enum.status/kw->val ::enum.status/initial) 0))
  (is (thrown? js/Error (enum.status/kw->val ::enum.status/digerydoos)))
  (is (= (enum.status/val->kw 1) ::enum.status/request-candidate-invite))
  (is (thrown? js/Error (enum.status/val->kw -99))))
