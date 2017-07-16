(ns ethlance.debounce-fx
  (:require
    [re-frame.core :refer [reg-fx dispatch console]]
    [cljs.spec.alpha :as s]))

(defn now [] (.getTime (js/Date.)))

(def registered-keys (atom nil))

(s/def ::key keyword?)
(s/def ::event vector?)
(s/def ::delay integer?)

(s/def ::dispatch-debounce (s/keys :req-un [::key ::event ::delay]))

(defn dispatch-if-not-superceded [{:keys [key delay event time-received]}]
  (when (= time-received (get @registered-keys key))
    ;; no new events on this key!
    (dispatch event)))

(defn dispatch-later [{:keys [delay] :as debounce}]
  (js/setTimeout
    (fn [] (dispatch-if-not-superceded debounce))
    delay))

(reg-fx
  :dispatch-debounce
  (fn dispatch-debounce [debounce]
    (when (= :cljs.spec/invalid (s/conform ::dispatch-debounce debounce))
      (console :error (s/explain-str ::dispatch-debounce debounce)))
    (let [ts (now)]
      (swap! registered-keys assoc (:key debounce) ts)
      (dispatch-later (assoc debounce :time-received ts)))))
