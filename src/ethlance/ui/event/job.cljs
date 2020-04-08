(ns ethlance.ui.event.job
  "Events for handling jobs and job listings"
  (:require
   [re-frame.core :as re]))

(def default-job
  #:ethlance.job-listing{:skills #{}
                         :min-rating 1
                         :max-rating 5
                         :min-hourly-rate nil
                         :max-hourly-rate nil
                         :min-num-feedbacks nil
                         :payment-type :hourly-rate
                         :experience-level :novice
                         :country nil})

