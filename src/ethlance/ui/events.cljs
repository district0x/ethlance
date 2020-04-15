(ns ethlance.ui.events
  "Includes the list of all registered events within re-frame."
  (:require 
   [re-frame.core :as re]

   ;; District UI Events
   [district.ui.logging.events]

   ;; Ethlance Component Event Handlers
   [ethlance.ui.component.modal.events]

   ;; Ethlance Page Event Handlers
   [ethlance.ui.page.me.events]       ;; :page.me/*

   ;; Ethlance Main Event Handlers
   [ethlance.ui.event.sign-in]        ;; :user/*
   [ethlance.ui.event.job]            ;; :job/*
   [ethlance.ui.event.job-listing]))  ;; :job-listing/*


(defn initialize
  "Sets initial db state for local components, local pages, and site-wide events."
  [{:keys [db] :as cofx} _]
  (let [new-db
        (assoc db
               ;; Component Events
               ;; ...

               ;; Page Events
               ethlance.ui.page.me.events/state-key
               ethlance.ui.page.me.events/state-default

               ;; Main Events
               ethlance.ui.event.job/state-key
               ethlance.ui.event.job/state-default
               ethlance.ui.event.job-listing/state-key
               ethlance.ui.event.job-listing/state-default)]
    {:db new-db
     :log/info ["Initialized re-frame app state" (clj->js new-db)]}))

;;
;; Registered Events
;;

(re/reg-event-fx :ethlance/initialize initialize)
