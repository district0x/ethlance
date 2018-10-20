(ns ethlance.shared.type.currency-test
  (:require
   [clojure.test :refer [deftest is are testing]]
   [ethlance.shared.type.currency :as type.currency]))


(deftest currency-tests
  (is (= (type.currency/kw->val ::type.currency/eth) 0))
  (is (= (type.currency/kw->val ::type.currency/usd) 1))
  (is (thrown? js/Error (type.currency/kw->val ::type.currency/digerydoos)))
  (is (= (type.currency/val->kw 0) ::type.currency/eth))
  (is (= (type.currency/val->kw 1) ::type.currency/usd))
  (is (thrown? js/Error (type.currency/val->kw -99))))
