(ns ethlance.ui.page.jobs.events
  (:require [district.parsers :refer [parse-int]]
            [district.ui.router.effects :as router.effects]
            [ethlance.shared.constants :as constants]
            [district.ui.graphql.events :as gql-events]
            [ethlance.ui.event.templates :as event.templates]
            [ethlance.ui.event.utils :as event.utils]
            [ethlance.ui.page.jobs.graphql :as j-gql]
            [re-frame.core :as re]))

;; Page State
(def state-key :page.jobs)
(def state-default
  {; Job Listing Query Parameters
   :skills #{}
   :category ["All Categories" nil]; constants/category-default
   :feedback-min-rating nil
   :feedback-max-rating 5
   :min-hourly-rate nil
   :max-hourly-rate nil
   :min-num-feedbacks nil
   :payment-type nil
   :experience-level nil})

(defn initialize-page
  "Event FX Handler. Setup listener to dispatch an event when the page is active/visited."
  [{:keys [db]} _]
  (let [page-state (get db state-key)]
    {:db (assoc-in db [state-key] state-default)
     ::router.effects/watch-active-page
     [{:id :page.jobs/initialize-page
       :name :route.job/jobs
       :dispatch [:page.jobs/query-job-listing]}]}))

(defn query-job-listing
  "Event FX Handler. Perform Job Listing Query."
  [{:keys [db]} [_ router-params]]
  (let [page-state (get-in db [state-key])
        filter-keys [:feedback-max-rating]
        filter-params (reduce (fn [acc filter-key]
                                (let [filter-val (get-in db [state-key filter-key])]
                                  (if (not (nil? filter-val))
                                    (assoc acc filter-key filter-val)
                                    acc)))
                              {}
                              filter-keys)
        args {:search-params filter-params}]
      {:dispatch [::gql-events/query {:query {:queries [(j-gql/jobs-query {:search-params {:feedback-max-rating 5}})]}}]}))

(defn add-skill
  "Event FX Handler. Append skill to skill listing."
  [{:keys [db]} [_ new-skill]]
  {:db (update-in db [state-key :skills] conj new-skill)})

;;
;; Registered Events
;;
(def create-assoc-handler (partial event.utils/create-assoc-handler state-key))

(defn trigger-search
  "Stores (assoc) the changed value to app-db and causes event to be dispatched
  that queries GQL API with search params"
  [handler-fn]
  (fn [cofx & [event value]]
    (println ">>> trigger-search" event "|" value)
    {:db (:db (handler-fn cofx event value))
     :dispatch [:page.jobs/query-job-listing]}))

;; TODO: switch based on dev environment
(re/reg-event-fx :page.jobs/initialize-page initialize-page)
(re/reg-event-fx :page.jobs/query-job-listing query-job-listing)
(re/reg-event-fx :page.jobs/set-skills (create-assoc-handler :skills))
(re/reg-event-fx :page.jobs/add-skill add-skill)
(re/reg-event-fx :page.jobs/set-category (create-assoc-handler :category))
(re/reg-event-fx :page.jobs/set-feedback-max-rating (trigger-search (event.templates/create-set-feedback-max-rating state-key)))
(re/reg-event-fx :page.jobs/set-feedback-min-rating (event.templates/create-set-feedback-min-rating state-key))
(re/reg-event-fx :page.jobs/set-min-hourly-rate (event.templates/create-set-min-hourly-rate state-key))
(re/reg-event-fx :page.jobs/set-max-hourly-rate (event.templates/create-set-max-hourly-rate state-key))
(re/reg-event-fx :page.jobs/set-min-num-feedbacks (create-assoc-handler :min-num-feedbacks parse-int))
(re/reg-event-fx :page.jobs/set-payment-type (create-assoc-handler :payment-type))
(re/reg-event-fx :page.jobs/set-experience-level (create-assoc-handler :experience-level))
