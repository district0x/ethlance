(ns ethlance.shared.enum.bid-option
  (:require
   [bignumber.core :as bn]
   [clojure.spec.alpha :as s]
   [ethlance.shared.spec-utils :refer [strict-conform]]
   [ethlance.shared.enumeration :as enum]))


(def enum-bid
  {::hourly-rate 0
   ::fixed-price 1
   ::annual-salary 2
   ::bounty 3})


(def kw->val #(enum/kw->val enum-bid %))
(def val->kw #(enum/val->kw enum-bid %))
(def assoc-kw->val #(enum/assoc-kw->val enum-bid %))
