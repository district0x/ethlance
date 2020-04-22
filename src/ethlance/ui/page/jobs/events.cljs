(ns ethlance.ui.page.jobs.events
  (:require
   [re-frame.core :as re]
   [district.ui.router.effects :as router.effects]
   [ethlance.shared.mock :as mock]))


;; Page State
(def state-key :page.jobs)
(def state-default
  {:job-listing/max-per-page 10
   :job-listing/state :start
   :job-listing []

   ;; Job Listing Query Parameters
   :skills #{}
   :feedback-min-rating 1
   :feedback-max-rating 5
   :min-hourly-rate nil
   :max-hourly-rate nil
   :min-num-feedbacks nil
   :payment-type :hourly-rate
   :experience-level :novice
   :country nil})


(defn mock-job-listing [& [n]]
  (mapv mock/generate-mock-job (range 1 (or n 10))))


(defn initialize-page
  "Event FX Handler. Setup listener to dispatch an event when the page is active/visited."
  [{:keys [db]} _]
  (let [page-state (get db state-key)]
    {::router.effects/watch-active-page
     [{:id :page.jobs/initialize-page
       :name :route.job/jobs
       :dispatch [:page.jobs/query-job-listing page-state]}]}))


(defn mock-query-job-listing
  "Event FX Handler. Perform Job Listing Query."
  [{:keys [db] :as cofxs} [_ page-state]]
  ;;TODO: mock up + production graphql
  (let [job-listing (mock-job-listing)]
    {:db (assoc-in db [state-key :job-listing/state] :loading)
     :dispatch [:page.jobs/-set-job-listing job-listing]}))


(defn set-job-listing
  "Event FX Handler. Set the Current Job Listing."
  [{:keys [db]} [_ job-listing]]
  {:db (-> db
           (assoc-in [state-key :job-listing/state] :done)
           (assoc-in [state-key :job-listing] job-listing))})


;;
;; Registered Events
;;

;; TODO: switch based on dev environment
(re/reg-event-fx :page.jobs/initialize-page initialize-page)
(re/reg-event-fx :page.jobs/query-job-listing mock-query-job-listing)


;; Intermediates
(re/reg-event-fx :page.jobs/-set-job-listing set-job-listing)


