(ns ethlance.shared.enumeration
  "General Enumeration functions."
  (:require
   [bignumber.core :as bn]
   [clojure.spec.alpha :as s]
   [ethlance.shared.spec-utils :refer [strict-conform]]))


(defn kw->val
  "Strict conversion of a keyword to a value within a map representing a
  set of enumerated keys with values."
  [m kw]
  (let [kw (strict-conform (set (keys m)) kw)]
    (get m kw)))


(defn val->kw
  "Strict conversion of a value to the equivalent keyword within a map
  representing a set of enumerated keys with values."
  [m x]
  (let [x (strict-conform (set (vals m)) x)
        zm (zipmap (vals m) (keys m))]
    (get zm x)))
