(ns ethlance.shared.enumeration.availability
  (:require
   [ethlance.shared.enumeration :as enum]))


(def enum-availability
  {::full-time 0
   ::part-time 1
   ::all 2})


(def kw->val #(enum/kw->val enum-availability %))
(def val->kw #(enum/val->kw enum-availability %))
(def assoc-kw->val #(enum/assoc-kw->val enum-availability %1 %2))
(def assoc-val->kw #(enum/assoc-val->kw enum-availability %1 %2))
