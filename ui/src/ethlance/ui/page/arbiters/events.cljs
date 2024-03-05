(ns ethlance.ui.page.arbiters.events
  (:require
    [district.parsers :refer [parse-int]]
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
   :category ["All Categories" nil]
   :feedback-min-rating nil
   :feedback-max-rating 5
   :min-fee nil
   :max-fee nil
   :min-num-feedbacks nil
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

(re/reg-event-fx :page.arbiters/initialize-page initialize-page)
(re/reg-event-fx :page.arbiters/set-offset (create-assoc-handler :offset))
(re/reg-event-fx :page.arbiters/set-limit (create-assoc-handler :limit))
(re/reg-event-fx :page.arbiters/set-skills (create-assoc-handler :skills))
(re/reg-event-fx :page.arbiters/add-skill add-skill)
(re/reg-event-fx :page.arbiters/set-category (create-assoc-handler :category))
(re/reg-event-fx :page.arbiters/set-feedback-max-rating (event.templates/create-set-feedback-max-rating state-key))
(re/reg-event-fx :page.arbiters/set-feedback-min-rating (event.templates/create-set-feedback-min-rating state-key))
(re/reg-event-fx :page.arbiters/set-min-fee (create-assoc-handler :min-fee parse-int))
(re/reg-event-fx :page.arbiters/set-max-fee (create-assoc-handler :max-fee parse-int))
(re/reg-event-fx :page.arbiters/set-min-num-feedbacks (create-assoc-handler :min-num-feedbacks parse-int))
(re/reg-event-fx :page.arbiters/set-country (create-assoc-handler :country))
