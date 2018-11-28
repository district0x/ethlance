(ns ethlance.shared.enum.boolean
  "Represents an enumeration type for a boolean value"
  (:require
   [ethlance.shared.enumeration :as enum]))
   

(def enum-boolean
  {false 0
   true 1})


(def kw->val #(enum/kw->val enum-boolean %))
(def val->kw #(enum/val->kw enum-boolean %))
(def assoc-kw->val #(enum/assoc-kw->val enum-boolean %1 %2))
(def assoc-val->kw #(enum/assoc-val->kw enum-boolean %1 %2))
