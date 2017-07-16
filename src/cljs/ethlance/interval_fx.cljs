(ns ethlance.interval-fx
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx inject-cofx path trim-v after debug reg-fx console]]
            [medley.core :as medley]
            [ethlance.utils :as u]
            [cljs.spec.alpha :as s]))

(s/def ::dispatch (s/coll-of keyword))
(s/def ::ms int?)
(s/def ::dispatch-interval-args (s/keys :req-un [::dispatch ::db-path ::ms]))

(reg-event-db
  ::set-interval
  (fn [db [_ db-path interval-id]]
    (assoc-in db db-path interval-id)))

(reg-event-db
  ::cancel-interval
  (fn [db [_ db-path interval-id]]
    (medley/dissoc-in db (conj (u/ensure-vec db-path) interval-id))))

(reg-fx
  :dispatch-interval
  (fn [{:keys [:dispatch :db-path :ms] :as config}]
    (s/assert ::dispatch-interval-args config)
    (let [interval-id (js/setInterval #(re-frame/dispatch dispatch) ms)]
      (re-frame/dispatch [::set-interval db-path interval-id]))))

(reg-fx
  :clear-interval
  (fn [{:keys [:interval-id :db-path]}]
    (js/clearInterval interval-id)
    (re-frame/dispatch [::cancel-interval db-path interval-id])))
