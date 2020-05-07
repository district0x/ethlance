(ns ethlance.ui.page.sign-up.events
  (:require
   [re-frame.core :as re]
   [district.parsers :refer [parse-int parse-float]]
   [district.ui.router.effects :as router.effects]
   [ethlance.shared.constants :as constants]
   [ethlance.shared.mock :as mock]
   [ethlance.ui.event.utils :as event.utils]
   [ethlance.ui.event.templates :as event.templates]))


(def state-key :page.sign-up)
(def state-default
  {:candidate/full-name nil
   :candidate/professional-title nil
   :candidate/email nil
   :candidate/hourly-rate nil
   :candidate/github-key nil
   :candidate/linkedin-key nil
   :candidate/languages []
   :candidate/categories []
   :candidate/skills []
   :candidate/biography nil
   :candidate/country nil
   :candidate/ready-for-hire? false

   :employer/full-name nil
   :employer/professional-title nil
   :employer/email nil
   :employer/github-key nil
   :employer/linkedin-key nil
   :employer/languages []
   :employer/biography nil
   :employer/country nil

   :arbiter/full-name nil
   :arbiter/professional-title nil
   :arbiter/fixed-rate-per-dispute nil
   :arbiter/email nil
   :arbiter/github-key nil
   :arbiter/linkedin-key nil
   :arbiter/languages []
   :arbiter/biography nil
   :arbiter/country nil})


(defn initialize-page
  "Event FX Handler. Setup listener to dispatch an event when the page is active/visited."
  [{:keys [db]} _]
  (let [page-state (get db state-key)]
    {::router.effects/watch-active-page
     [{:id :page.sign-up/initialize-page
       :name :route.me/sign-up
       :dispatch []}]}))


;;
;; Registered Events
;;
(def create-assoc-handler (partial event.utils/create-assoc-handler state-key))


(re/reg-event-fx :page.sign-up/initialize-page initialize-page)
(re/reg-event-fx :page.sign-up/set-candidate-full-name (create-assoc-handler :candidate/full-name))
(re/reg-event-fx :page.sign-up/set-candidate-professional-title (create-assoc-handler :candidate/professional-title))
(re/reg-event-fx :page.sign-up/set-candidate-email (create-assoc-handler :candidate/email))
(re/reg-event-fx :page.sign-up/set-candidate-hourly-rate (create-assoc-handler :candidate/hourly-rate parse-float))
(re/reg-event-fx :page.sign-up/set-candidate-github-key (create-assoc-handler :candidate/github-key))
(re/reg-event-fx :page.sign-up/set-candidate-linkedin-key (create-assoc-handler :candidate/linkedin-key))
(re/reg-event-fx :page.sign-up/set-candidate-languages (create-assoc-handler :candidate/languages))
(re/reg-event-fx :page.sign-up/set-candidate-categories (create-assoc-handler :candidate/categories))
(re/reg-event-fx :page.sign-up/set-candidate-skills (create-assoc-handler :candidate/skills))
(re/reg-event-fx :page.sign-up/set-candidate-biography (create-assoc-handler :candidate/biography))
(re/reg-event-fx :page.sign-up/set-candidate-country (create-assoc-handler :candidate/country))
(re/reg-event-fx :page.sign-up/set-candidate-ready-for-hire? (create-assoc-handler :candidate/ready-for-hire? boolean))

(re/reg-event-fx :page.sign-up/set-employer-full-name (create-assoc-handler :employer/full-name))
(re/reg-event-fx :page.sign-up/set-employer-professional-title (create-assoc-handler :employer/professional-title))
(re/reg-event-fx :page.sign-up/set-employer-email (create-assoc-handler :employer/email))
(re/reg-event-fx :page.sign-up/set-employer-github-key (create-assoc-handler :employer/github-key))
(re/reg-event-fx :page.sign-up/set-employer-linkedin-key (create-assoc-handler :employer/linkedin-key))
(re/reg-event-fx :page.sign-up/set-employer-languages (create-assoc-handler :employer/languages))
(re/reg-event-fx :page.sign-up/set-employer-biography (create-assoc-handler :employer/biography))
(re/reg-event-fx :page.sign-up/set-employer-country (create-assoc-handler :employer/country))

(re/reg-event-fx :page.sign-up/set-arbiter-full-name (create-assoc-handler :arbiter/full-name))
(re/reg-event-fx :page.sign-up/set-arbiter-professional-title (create-assoc-handler :arbiter/professional-title))
(re/reg-event-fx :page.sign-up/set-arbiter-fixed-rate-per-dispute (create-assoc-handler :arbiter/fixed-rate-per-dispute parse-float))
(re/reg-event-fx :page.sign-up/set-arbiter-email (create-assoc-handler :arbiter/email))
(re/reg-event-fx :page.sign-up/set-arbiter-github-key (create-assoc-handler :arbiter/github-key))
(re/reg-event-fx :page.sign-up/set-arbiter-linkedin-key (create-assoc-handler :arbiter/linkedin-key))
(re/reg-event-fx :page.sign-up/set-arbiter-languages (create-assoc-handler :arbiter/languages))
(re/reg-event-fx :page.sign-up/set-arbiter-biography (create-assoc-handler :arbiter/biography))
(re/reg-event-fx :page.sign-up/set-arbiter-country (create-assoc-handler :arbiter/country))
