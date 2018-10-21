(ns ethlance.shared.enum.payment-type-test
  (:require
   [clojure.test :refer [deftest is are testing]]
   [ethlance.shared.enum.payment-type :as enum.payment]))


(deftest payment-type-tests
  (is (= (enum.payment/kw->val ::enum.payment/fixed-price) 0))
  (is (= (enum.payment/kw->val ::enum.payment/percentage) 1))
  (is (thrown? js/Error (enum.payment/kw->val ::enum.payment/digerydoos)))
  (is (= (enum.payment/val->kw 0) ::enum.payment/fixed-price))
  (is (= (enum.payment/val->kw 1) ::enum.payment/percentage))
  (is (thrown? js/Error (enum.payment/val->kw -99))))
