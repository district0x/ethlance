(ns ethlance.ui.page.jobs.subscriptions
  (:require
   [re-frame.core :as re]
   
   [ethlance.ui.page.jobs.events :as jobs.events]))


(defn job-listing [db _]
  (get-in db [jobs.events/state-key :job-listing]))


(defn job-listing-state [db _]
  (get-in db [jobs.events/state-key :job-listing/state]))



;; Registered Subscriptions

(re/reg-sub :page.jobs/job-listing job-listing)
(re/reg-sub :page.jobs/job-listing-state job-listing-state)
