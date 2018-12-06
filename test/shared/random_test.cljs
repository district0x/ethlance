(ns ethlance.shared.random-test
  (:require
   [clojure.test :refer [deftest is are testing]]
   [ethlance.shared.random :as random]))


(deftest random-distrib-main)


(deftest pluck-main
   (let [test-coll #{:a :b :c :d}
         *pluck-collection (atom (vec test-coll))]
     (is (contains? test-coll (random/pluck! *pluck-collection)))
     (is (vector? @*pluck-collection))
     (is (contains? test-coll (random/pluck! *pluck-collection)))
     (is (contains? test-coll (random/pluck! *pluck-collection)))
     (is (contains? test-coll (random/pluck! *pluck-collection)))
     (is (nil? (random/pluck! *pluck-collection)))))
