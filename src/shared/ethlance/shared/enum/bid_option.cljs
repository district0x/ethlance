(ns ethlance.shared.enum.bid-option
  (:require
   [bignumber.core :as bn]
   [clojure.spec.alpha :as s]
   [ethlance.shared.spec-utils :refer [strict-conform]]))


(def enum-bid
  {::hourly-rate 0
   ::fixed-price 1
   ::annual-salary 2
   ::bounty 3})


(s/def ::key (set (keys enum-bid)))
(s/def ::value (set (vals enum-bid)))


(defn kw->val
  "Convert an enumerated keyword currency type `kw` into the unsigned
  integer representation."
  [kw]
  (let [kw (strict-conform ::key kw)]
    (get enum-bid kw)))


(defn val->kw
  "Get the enumerated keyword from the provided unsigned integer `x`."
  [x]
  (let [x (strict-conform ::value (bn/number x))
        zm (zipmap (vals enum-bid) (keys enum-bid))]
    (get zm x)))
