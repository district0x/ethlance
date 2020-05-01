(ns ethlance.ui.page.jobs.events
  (:require
   [re-frame.core :as re]
   [district.parsers]
   [district.ui.router.effects :as router.effects]
   [ethlance.shared.constants :as constants]
   [ethlance.shared.mock :as mock]))


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


(defn set-skills
  "Event FX Handler. Set the skills in the skill listing."
  [{:keys [db]} [_ new-skill-listing]]
  {:db (assoc-in db [state-key :skills] new-skill-listing)})


(defn add-skill
  "Event FX Handler. Append skill to skill listing."
  [{:keys [db]} [_ new-skill]]
  {:db (update-in db [state-key :skills] conj new-skill)})


(defn set-category
  "Event FX Handler. Set the current feedback min rating."
  [{:keys [db]} [_ new-category]]
  {:db (assoc-in db [state-key :category] new-category)})


(defn set-feedback-min-rating
  "Event FX Handler. Set the current feedback min rating.

   # Notes

   - If the min rating is higher than the max rating, the max rating will also be adjusted appropriately."
  [{:keys [db]} [_ new-min-rating]]
  (let [new-min-rating (district.parsers/parse-int new-min-rating)
        current-max-rating (-> db (get-in [state-key :feedback-max-rating]) district.parsers/parse-int)
        new-max-rating (max new-min-rating current-max-rating)]
    {:db (-> db
             (assoc-in [state-key :feedback-min-rating] new-min-rating)
             (assoc-in [state-key :feedback-max-rating] new-max-rating))}))


(defn set-feedback-max-rating
  "Event FX Handler. Set the current feedback max rating.

   # Notes

   - If the max rating is lower than the min rating, the min rating will also be adjusted appropriately."
  [{:keys [db]} [_ new-max-rating]]
  (let [new-max-rating (district.parsers/parse-int new-max-rating)
        current-min-rating (-> db (get-in [state-key :feedback-min-rating]) district.parsers/parse-int)
        min-rating (min new-max-rating current-min-rating)]
    {:db (-> db
             (assoc-in [state-key :feedback-max-rating] new-max-rating)
             (assoc-in [state-key :feedback-min-rating] min-rating))}))


(defn set-min-hourly-rate
  "Event FX Handler. Set the current mininum hourly rate

   # Notes

   - If the min hourly rate is higher than the max hourly rate, the max hourly rate will also be adjusted appropriately."
  [{:keys [db]} [_ new-min-hourly-rate]]
  (let [new-min-hourly-rate (district.parsers/parse-float new-min-hourly-rate)
        current-max-hourly-rate (-> db (get-in [state-key :max-hourly-rate]) district.parsers/parse-float)
        new-max-hourly-rate (when current-max-hourly-rate (max new-min-hourly-rate current-max-hourly-rate))]
    {:db (-> db
             (assoc-in [state-key :min-hourly-rate] new-min-hourly-rate)
             (assoc-in [state-key :max-hourly-rate] new-max-hourly-rate))}))


(defn set-max-hourly-rate
  "Event FX Handler. Set the current maximum hourly rate

   # Notes

   - If the max hourly rate is lower than the min hourly rate, the min hourly rate will also be adjusted appropriately."
  [{:keys [db]} [_ new-max-hourly-rate]]
  (let [new-max-hourly-rate (district.parsers/parse-float new-max-hourly-rate)
        current-min-hourly-rate (-> db (get-in [state-key :min-hourly-rate]) district.parsers/parse-float)
        new-min-hourly-rate (when current-min-hourly-rate (min new-max-hourly-rate current-min-hourly-rate))]
    {:db (-> db
             (assoc-in [state-key :min-hourly-rate] new-min-hourly-rate)
             (assoc-in [state-key :max-hourly-rate] new-max-hourly-rate))}))


(defn set-min-num-feedbacks
  "Event FX Handler. Set the minimum number of feedbacks"
  [{:keys [db]} [_ new-min-num-feedbacks]]
  {:db (assoc-in db [state-key :min-num-feedbacks] (district.parsers/parse-int new-min-num-feedbacks))})


(defn set-payment-type
  "Event FX Handler. Set the payment type"
  [{:keys [db]} [_ new-payment-type]]
  {:db (assoc-in db [state-key :payment-type] new-payment-type)})


(defn set-experience-level
  "Event FX Handler. Set the experience level"
  [{:keys [db]} [_ new-experience-level]]
  {:db (assoc-in db [state-key :experience-level] new-experience-level)})


(defn set-country
  "Event FX Handler. Set the country"
  [{:keys [db]} [_ new-country]]
  {:db (assoc-in db [state-key :country] new-country)})


;;
;; Registered Events
;;


;; TODO: switch based on dev environment
(re/reg-event-fx :page.jobs/initialize-page initialize-page)
(re/reg-event-fx :page.jobs/query-job-listing mock-query-job-listing)
(re/reg-event-fx :page.jobs/set-skills set-skills)
(re/reg-event-fx :page.jobs/add-skill add-skill)
(re/reg-event-fx :page.jobs/set-category set-category)
(re/reg-event-fx :page.jobs/set-feedback-max-rating set-feedback-max-rating)
(re/reg-event-fx :page.jobs/set-feedback-min-rating set-feedback-min-rating)
(re/reg-event-fx :page.jobs/set-min-hourly-rate set-min-hourly-rate)
(re/reg-event-fx :page.jobs/set-max-hourly-rate set-max-hourly-rate)
(re/reg-event-fx :page.jobs/set-min-num-feedbacks set-min-num-feedbacks)
(re/reg-event-fx :page.jobs/set-payment-type set-payment-type)
(re/reg-event-fx :page.jobs/set-experience-level set-experience-level)
(re/reg-event-fx :page.jobs/set-country set-country)


;; Intermediates
(re/reg-event-fx :page.jobs/-set-job-listing set-job-listing)
