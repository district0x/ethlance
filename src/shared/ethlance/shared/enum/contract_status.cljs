(ns ethlance.shared.enum.contract-status
  (:require
   [bignumber.core :as bn]
   [clojure.spec.alpha :as s]
   [ethlance.shared.spec-utils :refer [strict-conform]]))


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


(s/def ::key (set (keys enum-status)))
(s/def ::value (set (vals enum-status)))


(defn kw->val
  "Convert an enumerated keyword status `kw` into the unsigned integer
  representation."
  [kw]
  (let [kw (strict-conform ::key kw)]
    (get enum-status kw)))


(defn val->kw
  "Get the enumerated keyword from the provided unsigned integer `x`."
  [x]
  (let [x (strict-conform ::value (bn/number x))
        zm (zipmap (vals enum-status) (keys enum-status))]
    (get zm x)))
