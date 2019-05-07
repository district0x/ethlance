(ns ethlance.shared.enumeration.comment-type
  (:require
   [ethlance.shared.enumeration :as enum]))

(def enum-comment-type
  {::work-contract 0
   ::invoice 1
   ::dispute 2})


(def kw->val #(enum/kw->val enum-comment-type %))
(def val->kw #(enum/val->kw enum-comment-type %))
(def assoc-kw->val #(enum/assoc-kw->val enum-comment-type %1 %2))
(def assoc-val->kw #(enum/assoc-val->kw enum-comment-type %1 %2))
