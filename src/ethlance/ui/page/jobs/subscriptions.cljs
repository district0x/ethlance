(ns ethlance.ui.page.jobs.subscriptions
  (:require
   [re-frame.core :as re]
   
   [ethlance.ui.page.jobs.events :as jobs.events]))


(defn job-listing [db _]
  ;; FIXME: base on query loading data
  (empty? (get-in db [jobs.events/state-key :job-listing])))


;; Registered Subscriptions

(re/reg-sub :page.jobs/job-listing job-listing)
