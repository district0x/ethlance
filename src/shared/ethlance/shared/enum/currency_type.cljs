(ns ethlance.shared.enum.currency-type
  "Represents an enumeration type for currency."
  (:require
   [bignumber.core :as bn]
   [clojure.spec.alpha :as s]
   [ethlance.shared.spec-utils :refer [strict-conform]]
   [ethlance.shared.enumeration :as enum]))
   

(def enum-currency
  {::eth 0
   ::usd 1})


(def kw->val #(enum/kw->val enum-currency %))
(def val->kw #(enum/val->kw enum-currency %))
