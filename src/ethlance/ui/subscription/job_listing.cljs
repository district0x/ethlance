(ns ethlance.ui.subscription.job-listing
  (:require
   [re-frame.core :as re]
   
   [ethlance.ui.event.job-listing :as event.job-listing]))


(defn loading? [db _]
  ;; FIXME: base on query loading data
  (empty? (get-in db [event.job-listing/state-key :data])))


(defn data [db _]
  (get-in db [event.job-listing/state-key :data]))



;; Registered Subscriptions

(re/reg-sub :job-listing/loading? loading?)
(re/reg-sub :job-listing/data data)

