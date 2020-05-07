(ns ethlance.ui.page.candidates.events
  (:require
   [re-frame.core :as re]
   [district.parsers :refer [parse-int parse-float]]
   [district.ui.router.effects :as router.effects]
   [ethlance.shared.constants :as constants]
   [ethlance.shared.mock :as mock]
   [ethlance.ui.event.utils :as event.utils]
   [ethlance.ui.event.templates :as event.templates]))


(def state-key :page.candidates)
(def state-default
  {:skills #{}
   :category constants/category-default
   :feedback-min-rating 1
   :feedback-max-rating 5
   :payment-type :fixed-price
   :country nil})


(defn initialize-page
  "Event FX Handler. Setup listener to dispatch an event when the page is active/visited."
  [{:keys [db]} _]
  (let [page-state (get db state-key)]
    {::router.effects/watch-active-page
     [{:id :page.candidates/initialize-page
       :name :route.user/candidates
       :dispatch []}]}))


(defn add-skill
  "Event FX Handler. Append skill to skill listing."
  [{:keys [db]} [_ new-skill]]
  {:db (update-in db [state-key :skills] conj new-skill)})


;;
;; Registered Events
;;
(def create-assoc-handler (partial event.utils/create-assoc-handler state-key))


(re/reg-event-fx :page.candidates/initialize-page initialize-page)
(re/reg-event-fx :page.candidates/set-skills (create-assoc-handler :skills))
(re/reg-event-fx :page.candidates/add-skill add-skill)
(re/reg-event-fx :page.candidates/set-category (create-assoc-handler :category))
(re/reg-event-fx :page.candidates/set-feedback-max-rating (event.templates/create-set-feedback-max-rating state-key))
(re/reg-event-fx :page.candidates/set-feedback-min-rating (event.templates/create-set-feedback-min-rating state-key))
(re/reg-event-fx :page.candidates/set-payment-type (create-assoc-handler :payment-type))
(re/reg-event-fx :page.candidates/set-country (create-assoc-handler :country))
