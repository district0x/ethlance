(ns ethlance.shared.type-test
  (:require
   [clojure.test :refer [deftest is are testing]]
   [clojure.spec.alpha :as s]
   
   [ethlance.shared.type :refer [strict-conform]]))


(s/def ::number number?)
(s/def ::string string?)


(deftest test-strict-conform
  (is (= (strict-conform ::number 12) 12))
  (is (thrown? js/Error (strict-conform ::number "NaN")))

  (is (= (strict-conform ::string "test") "test"))
  (is (thrown? js/Error (strict-conform ::string 12))))
