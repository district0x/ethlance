(ns ethlance.ui.page.jobs.subscriptions
  (:require
   [re-frame.core :as re]
   
   [ethlance.ui.page.jobs.events :as jobs.events]))


(defn job-listing [db _]
  (get-in db [jobs.events/state-key :job-listing]))


(defn job-listing-state [db _]
  (get-in db [jobs.events/state-key :job-listing/state]))


(defn skills [db _]
  (get-in db [jobs.events/state-key :skills]))


(defn category [db _]
  (get-in db [jobs.events/state-key :category]))


(defn feedback-max-rating [db _]
  (get-in db [jobs.events/state-key :feedback-max-rating]))


(defn feedback-min-rating [db _]
  (get-in db [jobs.events/state-key :feedback-min-rating]))


(defn min-hourly-rate [db _]
  (get-in db [jobs.events/state-key :min-hourly-rate]))


(defn max-hourly-rate [db _]
  (get-in db [jobs.events/state-key :max-hourly-rate]))


(defn min-num-feedbacks [db _]
  (get-in db [jobs.events/state-key :min-num-feedbacks]))


(defn payment-type [db _]
  (get-in db [jobs.events/state-key :payment-type]))


(defn experience-level [db _]
  (get-in db [jobs.events/state-key :experience-level]))


(defn country [db _]
  (get-in db [jobs.events/state-key :country]))


;;
;; Registered Subscriptions
;;

(re/reg-sub :page.jobs/job-listing job-listing)
(re/reg-sub :page.jobs/job-listing-state job-listing-state)
(re/reg-sub :page.jobs/skills skills)
(re/reg-sub :page.jobs/category category)
(re/reg-sub :page.jobs/feedback-max-rating feedback-max-rating)
(re/reg-sub :page.jobs/feedback-min-rating feedback-min-rating)
(re/reg-sub :page.jobs/min-hourly-rate min-hourly-rate)
(re/reg-sub :page.jobs/max-hourly-rate max-hourly-rate)
(re/reg-sub :page.jobs/min-num-feedbacks min-num-feedbacks)
(re/reg-sub :page.jobs/payment-type payment-type)
(re/reg-sub :page.jobs/experience-level experience-level)
(re/reg-sub :page.jobs/country country)
