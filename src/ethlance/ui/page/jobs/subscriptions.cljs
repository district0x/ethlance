(ns ethlance.ui.page.jobs.subscriptions
  (:require
   [re-frame.core :as re]
   
   [ethlance.ui.page.jobs.events :as jobs.events]))


(defn job-listing [db _]
  (get-in db [jobs.events/state-key :job-listing]))


(defn job-listing-state [db _]
  (get-in db [jobs.events/state-key :job-listing/state]))


(defn category [db _]
  (get-in db [jobs.events/state-key :category]))


(defn feedback-max-rating [db _]
  (get-in db [jobs.events/state-key :feedback-max-rating]))


(defn feedback-min-rating [db _]
  (get-in db [jobs.events/state-key :feedback-min-rating]))


;; Registered Subscriptions

(re/reg-sub :page.jobs/job-listing job-listing)
(re/reg-sub :page.jobs/job-listing-state job-listing-state)
(re/reg-sub :page.jobs/category category)
(re/reg-sub :page.jobs/feedback-max-rating feedback-max-rating)
(re/reg-sub :page.jobs/feedback-min-rating feedback-min-rating)
