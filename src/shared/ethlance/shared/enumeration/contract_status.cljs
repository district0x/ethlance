(ns ethlance.shared.enumeration.contract-status
  (:require
   [ethlance.shared.enumeration :as enum]))

(def enum-status
  {::initial 0
   ;; --
   ::request-candidate-invite 1
   ::request-employer-invite 2
   ::open-bounty 3
   ;; --
   ::accepted 4
   ;; --
   ::in-progress 5
   ::on-hold 6
   ;; --
   ::request-candidate-finished 7
   ::request-employer-finished 8
   ::finished 9
   ;; --
   ::cancelled 10})


(def kw->val #(enum/kw->val enum-status %))
(def val->kw #(enum/val->kw enum-status %))
(def assoc-kw->val #(enum/assoc-kw->val enum-status %1 %2))
(def assoc-val->kw #(enum/assoc-val->kw enum-status %1 %2))
