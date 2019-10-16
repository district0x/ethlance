(ns ethlance.shared.enumeration.bid-option-test
  (:require
   [clojure.test :refer [deftest is are testing]]
   [ethlance.shared.enumeration.bid-option :as enum.bid-option]))


(deftest bid-option-tests
  (is (= (enum.bid-option/kw->val ::enum.bid-option/hourly-rate) 0))
  (is (= (enum.bid-option/kw->val ::enum.bid-option/fixed-price) 1))
  (is (= (enum.bid-option/kw->val ::enum.bid-option/annual-salary) 2))
  (is (thrown? js/Error (enum.bid-option/kw->val ::enum.bid-option/digerydoos)))
  (is (= (enum.bid-option/val->kw 0) ::enum.bid-option/hourly-rate))
  (is (= (enum.bid-option/val->kw 1) ::enum.bid-option/fixed-price))
  (is (= (enum.bid-option/val->kw 2) ::enum.bid-option/annual-salary))
  (is (thrown? js/Error (enum.bid-option/val->kw -99))))
