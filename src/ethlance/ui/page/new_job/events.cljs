(ns ethlance.ui.page.new-job.events
  (:require [district.ui.router.effects :as router.effects]
            [ethlance.ui.event.utils :as event.utils]
            [re-frame.core :as re]))

(def state-key :page.new-job)

(def state-default
  {:type :job
   :name nil
   :category nil
   :bid-option :hourly-rate
   :required-experience-level :intermediate
   :estimated-project-length :day
   :required-availability :full-time
   :required-skills #{}
   :description nil
   :form-of-payment :ethereum
   :token-address nil
   :with-arbiter? true})

(defn initialize-page
  "Event FX Handler. Setup listener to dispatch an event when the page is active/visited."
  []
  {::router.effects/watch-active-page
   [{:id :page.new-job/initialize-page
     :name :route.job/new
     :dispatch []}]})

(def create-assoc-handler (partial event.utils/create-assoc-handler state-key))

(re/reg-event-fx :page.new-job/initialize-page initialize-page)
(re/reg-event-fx :page.new-job/set-type (create-assoc-handler :type))
(re/reg-event-fx :page.new-job/set-name (create-assoc-handler :name))
(re/reg-event-fx :page.new-job/set-category (create-assoc-handler :category))
(re/reg-event-fx :page.new-job/set-bid-option (create-assoc-handler :bid-option))
(re/reg-event-fx :page.new-job/set-required-experience-level (create-assoc-handler :required-experience-level))
(re/reg-event-fx :page.new-job/set-estimated-project-length (create-assoc-handler :estimated-project-length))
(re/reg-event-fx :page.new-job/set-required-availability (create-assoc-handler :required-availability))
(re/reg-event-fx :page.new-job/set-required-skills (create-assoc-handler :required-skills))
(re/reg-event-fx :page.new-job/set-description (create-assoc-handler :description))
(re/reg-event-fx :page.new-job/set-form-of-payment (create-assoc-handler :form-of-payment))
(re/reg-event-fx :page.new-job/set-token-address (create-assoc-handler :token-address))
(re/reg-event-fx :page.new-job/set-with-arbiter? (create-assoc-handler :with-arbiter?))
