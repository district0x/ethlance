(ns ethlance.shared.enum.payment-type
  "Represents an enumeration type for different types of payment."
  (:require
   [clojure.spec.alpha :as s]
   [ethlance.shared.spec-utils :refer [strict-conform]]))


(def enum-payment
  {::fixed-price 0
   ::percentage 1})


(s/def ::key (set (keys enum-payment)))
(s/def ::value (set (vals enum-payment)))


(defn kw->val
  "Convert an enumerated keyword `kw` into the unsigned integer
  representation."
  [kw]
  (let [kw (strict-conform ::key kw)]
    (get enum-payment kw)))


(defn val->kw
  "Get the enumerated keyword from the provided unsigned integer `x`."
  [x]
  (let [x (strict-conform ::value x)
        zm (zipmap (vals enum-payment) (keys enum-payment))]
    (get zm x)))
