(ns ethlance.ui.event.job
  "Events for handling jobs and job listings"
  (:require
   [re-frame.core :as re]))

(def state-key :ethlance.job)
(def state-default
  {:title nil
   :employer nil
   :arbiter nil
   :skills #{}
   :min-rating 1
   :max-rating 5
   :min-hourly-rate nil
   :max-hourly-rate nil
   :min-num-feedbacks nil
   :payment-type :hourly-rate
   :project-length nil
   :availability :full-time
   :experience-level :novice
   :country nil})
