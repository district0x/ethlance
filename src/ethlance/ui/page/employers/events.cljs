(ns ethlance.ui.page.employers.events
  (:require [district.parsers :refer [parse-int]]
            [district.ui.router.effects :as router.effects]
            [ethlance.shared.constants :as constants]
            [ethlance.ui.event.templates :as event.templates]
            [ethlance.ui.event.utils :as event.utils]
            [re-frame.core :as re]))

(def state-key :page.employers)

(def state-default
  {:offset 0
   :limit 10
   :skills #{}
   :category constants/category-default
   :feedback-min-rating 1
   :feedback-max-rating 5
   :min-num-feedbacks nil
   :country nil})

(def create-assoc-handler (partial event.utils/create-assoc-handler state-key))

(defn initialize-page
  "Event FX Handler. Setup listener to dispatch an event when the page is active/visited."
  [{:keys []} _]
  {::router.effects/watch-active-page
   [{:id :page.employers/initialize-page
     :name :route.user/employers
     :dispatch []}]})

(defn add-skill
  "Event FX Handler. Append skill to skill listing."
  [{:keys [db]} [_ new-skill]]
  {:db (update-in db [state-key :skills] conj new-skill)})

(re/reg-event-fx :page.employers/initialize-page initialize-page)
(re/reg-event-fx :page.employers/set-offset (create-assoc-handler :offset))
(re/reg-event-fx :page.employers/set-limit (create-assoc-handler :limit))
(re/reg-event-fx :page.employers/set-skills (create-assoc-handler :skills))
(re/reg-event-fx :page.employers/add-skill add-skill)
(re/reg-event-fx :page.employers/set-category (create-assoc-handler :category))
(re/reg-event-fx :page.employers/set-feedback-max-rating (event.templates/create-set-feedback-max-rating state-key))
(re/reg-event-fx :page.employers/set-feedback-min-rating (event.templates/create-set-feedback-min-rating state-key))
(re/reg-event-fx :page.employers/set-min-num-feedbacks (create-assoc-handler :min-num-feedbacks parse-int))
(re/reg-event-fx :page.employers/set-country (create-assoc-handler :country))
