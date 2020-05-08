(ns ethlance.ui.page.employers.events
  (:require
   [re-frame.core :as re]
   [district.parsers :refer [parse-int parse-float]]
   [district.ui.router.effects :as router.effects]
   [ethlance.shared.constants :as constants]
   [ethlance.shared.mock :as mock]
   [ethlance.ui.event.utils :as event.utils]
   [ethlance.ui.event.templates :as event.templates]))

;; Page State
(def state-key :page.employers)
(def state-default
  {:skills #{}
   :category constants/category-default
   :feedback-min-rating 1
   :feedback-max-rating 5
   :min-num-feedbacks nil
   :country nil})


(defn initialize-page
  "Event FX Handler. Setup listener to dispatch an event when the page is active/visited."
  [{:keys [db]} _]
  (let [page-state (get db state-key)]
    {::router.effects/watch-active-page
     [{:id :page.employers/initialize-page
       :name :route.user/employers
       :dispatch []}]}))


(defn add-skill
  "Event FX Handler. Append skill to skill listing."
  [{:keys [db]} [_ new-skill]]
  {:db (update-in db [state-key :skills] conj new-skill)})


;;
;; Registered Events
;;
(def create-assoc-handler (partial event.utils/create-assoc-handler state-key))


;; TODO: switch based on dev environment
(re/reg-event-fx :page.employers/initialize-page initialize-page)
(re/reg-event-fx :page.employers/set-skills (create-assoc-handler :skills))
(re/reg-event-fx :page.employers/add-skill add-skill)
(re/reg-event-fx :page.employers/set-category (create-assoc-handler :category))
(re/reg-event-fx :page.employers/set-feedback-max-rating (event.templates/create-set-feedback-max-rating state-key))
(re/reg-event-fx :page.employers/set-feedback-min-rating (event.templates/create-set-feedback-min-rating state-key))
(re/reg-event-fx :page.employers/set-min-num-feedbacks (create-assoc-handler :min-num-feedbacks parse-int))
(re/reg-event-fx :page.employers/set-country (create-assoc-handler :country))
