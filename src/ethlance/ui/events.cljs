(ns ethlance.ui.events
  "Includes the list of all registered events within re-frame."
  (:require 
   [re-frame.core :as re]

   ;; District UI Events
   [district.ui.logging.events]

   ;; Ethlance Component Event Handlers
   [ethlance.ui.component.modal.events]

   ;; Ethlance Main Event Handlers
   [ethlance.ui.event.sign-in]        ;; :user/*
   [ethlance.ui.event.job]            ;; :job/*
   [ethlance.ui.event.job-listing]))  ;; :job-listing/*


(defn initialize
  "Sets initial db state."
  [{:keys [db] :as cofx} _]
  {:db (assoc db
              ethlance.ui.event.job/state-key
              ethlance.ui.event.job/state-default
              ethlance.ui.event.job-listing/state-key
              ethlance.ui.event.job-listing/state-default)
   :log/info ["Initialized re-frame app state"]})

;;
;; Registered Events
;;

(re/reg-event-fx :ethlance/initialize initialize)
