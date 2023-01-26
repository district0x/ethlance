(ns ethlance.shared.enumeration
  "General Enumeration functions."
  (:require [bignumber.core :as bn]
            [ethlance.shared.spec-utils :refer [strict-conform]]))

(defn kw->val
  "Strict conversion of a keyword to a value within a map representing a
  set of enumerated keys with values."
  [e kw]
  (let [kw (strict-conform (set (keys e)) kw)]
    (get e kw)))


(defn val->kw
  "Strict conversion of a value to the equivalent keyword within a map
  representing a set of enumerated keys with values."
  [e x]
  (let [x (strict-conform (set (vals e)) (bn/number x))
        ze (zipmap (vals e) (keys e))]
    (get ze x)))


(defn assoc-kw->val
  "Soft conversion of a map value representing an enumerated type from
  their keyword to their enumerated value.

  Keyword Paramaters:

  e - The map representing the enumeration between the key and value.

  m - Map containing the key which contains the enumeration

  k - The specific key to convert to an enumerated value.

  Notes:

  - nil fields will be ignored.
  "
  [e m k]
  (if (get m k)
    (assoc m k (kw->val e (get m k)))
    m))


(defn assoc-val->kw
  "Soft conversion of a map value representing an enumerated type from
  their enumerated value to their keyword.

  Keyword Paramaters:

  e - The map representing the enumeration between the key and value.

  m - Map containing the key which contains the enumeration

  k - The keyword containing the value to convert into a keyword

  Notes:

  - nil fields will be ignored.
  "
  [e m k]
  (if (get m k)
    (assoc m k (val->kw e (get m k)))
    m))
