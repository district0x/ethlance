(ns ethlance.ui.page.arbiters.events
  (:require [district.parsers :refer [parse-int]]
            [district.ui.router.effects :as router.effects]
            [ethlance.shared.constants :as constants]
            [ethlance.ui.event.templates :as event.templates]
            [ethlance.ui.event.utils :as event.utils]
            [re-frame.core :as re]))

(def state-key :page.arbiters)
(def state-default
  {:offset 0
   :limit 10
   :skills #{}
   :category constants/category-default
   :feedback-min-rating 1
   :feedback-max-rating 5
   :min-hourly-rate nil
   :max-hourly-rate nil
   :min-num-feedbacks nil
   :country nil})

(defn initialize-page
  "Event FX Handler. Setup listener to dispatch an event when the page is active/visited."
  [{:keys []} _]
  {::router.effects/watch-active-page
   [{:id :page.arbiters/initialize-page
     :name :route.user/candidates
     :dispatch []}]})

(defn add-skill
  "Event FX Handler. Append skill to skill listing."
  [{:keys [db]} [_ new-skill]]
  {:db (update-in db [state-key :skills] conj new-skill)})

;;
;; Registered Events
;;
(def create-assoc-handler (partial event.utils/create-assoc-handler state-key))

(re/reg-event-fx :page.arbiters/initialize-page initialize-page)
(re/reg-event-fx :page.arbiters/set-offset (create-assoc-handler :offset))
(re/reg-event-fx :page.arbiters/set-limit (create-assoc-handler :limit))
(re/reg-event-fx :page.arbiters/set-skills (create-assoc-handler :skills))
(re/reg-event-fx :page.arbiters/add-skill add-skill)
(re/reg-event-fx :page.arbiters/set-category (create-assoc-handler :category))
(re/reg-event-fx :page.arbiters/set-feedback-max-rating (event.templates/create-set-feedback-max-rating state-key))
(re/reg-event-fx :page.arbiters/set-feedback-min-rating (event.templates/create-set-feedback-min-rating state-key))
(re/reg-event-fx :page.arbiters/set-min-hourly-rate (event.templates/create-set-min-hourly-rate state-key))
(re/reg-event-fx :page.arbiters/set-max-hourly-rate (event.templates/create-set-max-hourly-rate state-key))
(re/reg-event-fx :page.arbiters/set-min-num-feedbacks (create-assoc-handler :min-num-feedbacks parse-int))
(re/reg-event-fx :page.arbiters/set-country (create-assoc-handler :country))
