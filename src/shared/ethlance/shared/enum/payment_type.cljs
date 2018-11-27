(ns ethlance.shared.enum.payment-type
  "Represents an enumeration type for different types of payment."
  (:require
   [bignumber.core :as bn]
   [clojure.spec.alpha :as s]
   [ethlance.shared.spec-utils :refer [strict-conform]]
   [ethlance.shared.enumeration :as enum]))


(def enum-payment
  {::fixed-price 0
   ::percentage 1})


(def kw->val #(enum/kw->val enum-payment %))
(def val->kw #(enum/val->kw enum-payment %))
(def assoc-kw->val #(enum/assoc-kw->val enum-payment %))
