(ns ethlance.shared.enumeration.payment-type
  "Represents an enumeration type for different types of payment."
  (:require
   [ethlance.shared.enumeration :as enum]))


(def enum-payment
  {::fixed-price 0
   ::percentage 1})


(def kw->val #(enum/kw->val enum-payment %))
(def val->kw #(enum/val->kw enum-payment %))
(def assoc-kw->val #(enum/assoc-kw->val enum-payment %1 %2))
(def assoc-val->kw #(enum/assoc-val->kw enum-payment %1 %2))
