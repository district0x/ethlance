(ns ethlance.shared.type.currency
  "Represents an enumeration type for currency."
  (:require
   [clojure.spec.alpha :as s]
   [ethlance.shared.type :refer [strict-conform]]))


(def enum-currency
  {::eth 0
   ::usd 1})


(s/def ::key (set (keys enum-currency-type)))
(s/def ::value (set (vals enum-currency-type)))


(defn kw->val
  "Convert an enumerated keyword currency type `kw` into the unsigned
  integer representation."
  [kw]
  (let [kw (strict-conform ::key kw)]
    (get enum-currency-type kw)))


(defn val->kw
  "Get the enumerated keyword from the provided unsigned integer `x`."
  [x]
  (let [x (strict-conform ::value x)
        zm (zipmap (vals enum-currency-type) (keys enum-currency-type))]
    (get zm x)))
