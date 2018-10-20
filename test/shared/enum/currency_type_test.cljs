(ns ethlance.shared.enum.currency-type-test
  (:require
   [clojure.test :refer [deftest is are testing]]
   [ethlance.shared.enum.currency-type :as enum.currency]))


(deftest currency-type-tests
  (is (= (enum.currency/kw->val ::enum.currency/eth) 0))
  (is (= (enum.currency/kw->val ::enum.currency/usd) 1))
  (is (thrown? js/Error (enum.currency/kw->val ::enum.currency/digerydoos)))
  (is (= (enum.currency/val->kw 0) ::enum.currency/eth))
  (is (= (enum.currency/val->kw 1) ::enum.currency/usd))
  (is (thrown? js/Error (enum.currency/val->kw -99))))
