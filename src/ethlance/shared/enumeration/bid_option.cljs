(ns ethlance.shared.enumeration.bid-option
  (:require
   [ethlance.shared.enumeration :as enum]))


(def enum-bid
  {::hourly-rate 0
   ::fixed-price 1
   ::annual-salary 2
   ::bounty 3})


(def kw->val #(enum/kw->val enum-bid %))
(def val->kw #(enum/val->kw enum-bid %))
(def assoc-kw->val #(enum/assoc-kw->val enum-bid %1 %2))
(def assoc-val->kw #(enum/assoc-val->kw enum-bid %1 %2))
