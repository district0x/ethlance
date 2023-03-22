(ns ethlance.ui.page.job-contract.events
  (:require [district.ui.router.effects :as router.effects]
            [ethlance.ui.event.utils :as event.utils]
            [district.ui.graphql.events :as gql-events]
            [re-frame.core :as re]))

;; Page State
(def state-key :page.job-contract)
(def state-default
  {})

(defn initialize-page
  "Event FX Handler. Setup listener to dispatch an event when the page is active/visited."
  []
  {::router.effects/watch-active-page
   [{:id :page.job-contract/initialize-page
     :name :route.job/contract
     :dispatch []}]})

;;
;; Registered Events
;;
(def create-assoc-handler (partial event.utils/create-assoc-handler state-key))

;; TODO: switch based on dev environment
(re/reg-event-fx :page.job-contract/initialize-page initialize-page)
(re/reg-event-fx :page.job-contract/set-feedback-rating (create-assoc-handler :feedback-rating))
(re/reg-event-fx :page.job-contract/set-feedback-text (create-assoc-handler :feedback-text))
(re/reg-event-fx :page.job-contract/set-feedback-recipient (create-assoc-handler :feedback-recipient))

(defn send-feedback
  [{:keys [db]} [_event-name params]]
  (let [job-story-id (:job-story/id params)
        text (:text params)
        rating (:rating params)
        to (:to params)
        mutation-params {:job-story/id job-story-id
                        :text text
                        :rating rating
                        :to to}]
    (println ">>> ethlance.ui.page.job-contract.events/send-feedback mutation-params" mutation-params)
    {:dispatch [::gql-events/mutation
                {:queries [[:leave-feedback mutation-params]]
                 :id :SendEmployerFeedbackMutation}]}))

(re/reg-event-fx :page.job-contract/send-feedback send-feedback)
