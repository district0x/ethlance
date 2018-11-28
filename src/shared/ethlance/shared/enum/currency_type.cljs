(ns ethlance.shared.enum.currency-type
  "Represents an enumeration type for currency."
  (:require
   [ethlance.shared.enumeration :as enum]))
   

(def enum-currency
  {::eth 0
   ::usd 1})


(def kw->val #(enum/kw->val enum-currency %))
(def val->kw #(enum/val->kw enum-currency %))
(def assoc-kw->val #(enum/assoc-kw->val enum-currency %1 %2))
(def assoc-val->kw #(enum/assoc-val->kw enum-currency %1 %2))
