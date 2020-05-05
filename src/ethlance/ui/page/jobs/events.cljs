(ns ethlance.ui.page.jobs.events
  (:require
   [re-frame.core :as re]
   [district.parsers :refer [parse-int parse-float]]
   [district.ui.router.effects :as router.effects]
   [ethlance.shared.constants :as constants]
   [ethlance.shared.mock :as mock]
   [ethlance.ui.event.utils :as event.utils]
   [ethlance.ui.event.templates :as event.templates]))


;; Page State
(def state-key :page.jobs)
(def state-default
  {:job-listing/max-per-page 10
   :job-listing/state :start
   :job-listing []

   ;; Job Listing Query Parameters
   :skills #{}
   :category constants/category-default
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


(defn add-skill
  "Event FX Handler. Append skill to skill listing."
  [{:keys [db]} [_ new-skill]]
  {:db (update-in db [state-key :skills] conj new-skill)})


;;
;; Registered Events
;;


;; TODO: switch based on dev environment
(re/reg-event-fx :page.jobs/initialize-page initialize-page)
(re/reg-event-fx :page.jobs/query-job-listing mock-query-job-listing)
(re/reg-event-fx :page.jobs/set-skills (event.utils/create-assoc-handler state-key :skills))
(re/reg-event-fx :page.jobs/add-skill add-skill)
(re/reg-event-fx :page.jobs/set-category (event.utils/create-assoc-handler state-key :category))
(re/reg-event-fx :page.jobs/set-feedback-max-rating (event.templates/create-set-feedback-max-rating state-key))
(re/reg-event-fx :page.jobs/set-feedback-min-rating (event.templates/create-set-feedback-min-rating state-key))
(re/reg-event-fx :page.jobs/set-min-hourly-rate (event.templates/create-set-min-hourly-rate state-key))
(re/reg-event-fx :page.jobs/set-max-hourly-rate (event.templates/create-set-max-hourly-rate state-key))
(re/reg-event-fx :page.jobs/set-min-num-feedbacks (event.utils/create-assoc-handler state-key :min-num-feedbacks parse-int))
(re/reg-event-fx :page.jobs/set-payment-type (event.utils/create-assoc-handler state-key :payment-type))
(re/reg-event-fx :page.jobs/set-experience-level (event.utils/create-assoc-handler state-key :experience-level))
(re/reg-event-fx :page.jobs/set-country (event.utils/create-assoc-handler state-key :country))


;; Intermediates
(re/reg-event-fx :page.jobs/-set-job-listing set-job-listing)
