(ns ethlance.ui.page.candidates.events
  (:require
    [district.parsers :refer [parse-int]]
    [district.ui.router.effects :as router.effects]
    [ethlance.shared.constants :as constants]
    [ethlance.ui.event.templates :as event.templates]
    [ethlance.ui.event.utils :as event.utils]
    [re-frame.core :as re]))


(def state-key :page.candidates)


(def state-default
  {:offset 0
   :limit 10
   :skills #{}
   :category ["All Categories" nil]
   :feedback-min-rating nil
   :feedback-max-rating 5
   :country nil})


(defn initialize-page
  "Event FX Handler. Setup listener to dispatch an event when the page is active/visited."
  [{:keys [db]} _]
  {:db (assoc-in db [state-key] state-default)})


(defn add-skill
  "Event FX Handler. Append skill to skill listing."
  [{:keys [db]} [_ new-skill]]
  {:db (update-in db [state-key :skills] conj new-skill)})


;;
;; Registered Events
;;
(def create-assoc-handler (partial event.utils/create-assoc-handler state-key))

(re/reg-event-fx :page.candidates/initialize-page initialize-page)
(re/reg-event-fx :page.candidates/set-offset (create-assoc-handler :offset))
(re/reg-event-fx :page.candidates/set-limit (create-assoc-handler :limit))
(re/reg-event-fx :page.candidates/set-skills (create-assoc-handler :skills))

(re/reg-event-fx :page.candidates/add-skill add-skill)
(re/reg-event-fx :page.candidates/set-category (create-assoc-handler :category))
(re/reg-event-fx :page.candidates/set-feedback-max-rating (event.templates/create-set-feedback-max-rating state-key))
(re/reg-event-fx :page.candidates/set-feedback-min-rating (event.templates/create-set-feedback-min-rating state-key))
(re/reg-event-fx :page.candidates/set-min-hourly-rate (event.templates/create-set-min-hourly-rate state-key))
(re/reg-event-fx :page.candidates/set-max-hourly-rate (event.templates/create-set-max-hourly-rate state-key))
(re/reg-event-fx :page.candidates/set-min-num-feedbacks (create-assoc-handler :min-num-feedbacks parse-int))
(re/reg-event-fx :page.candidates/set-country (create-assoc-handler :country))
