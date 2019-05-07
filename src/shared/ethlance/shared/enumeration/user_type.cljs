(ns ethlance.shared.enumeration.user-type
  (:require
   [ethlance.shared.enumeration :as enum]))

(def enum-user-type
  {::guest 0
   ::employer 1
   ::candidate 2
   ::arbiter 3})


(def kw->val #(enum/kw->val enum-user-type %))
(def val->kw #(enum/val->kw enum-user-type %))
(def assoc-kw->val #(enum/assoc-kw->val enum-user-type %1 %2))
(def assoc-val->kw #(enum/assoc-val->kw enum-user-type %1 %2))
