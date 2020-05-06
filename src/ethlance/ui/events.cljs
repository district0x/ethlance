(ns ethlance.ui.events
  "Main entry point for all registered events within re-frame for ethlance."
  (:require 
   [re-frame.core :as re]

   ;; District UI Events
   [district.ui.logging.events]

   ;; Ethlance Component Event Handlers
   [ethlance.ui.component.modal.events]

   ;; Ethlance Page Event Handlers
   [ethlance.ui.page.me.events]       ;; :page.me/*
   [ethlance.ui.page.jobs.events]     ;; :page.jobs/*
   [ethlance.ui.page.sign-up.events]  ;; :page.sign-up/*

   ;; Ethlance Main Event Handlers
   [ethlance.ui.event.sign-in]))      ;; :user/*


(def forwarded-events
  "Forwarded Events.

   Notes:

   - district.ui.router/watch-active-page effect handler uses forwarded events
   - Additional info: https://github.com/day8/re-frame-forward-events-fx"
  (list
   [:page.jobs/initialize-page
    :page.sign-up/initialize-page]))


(defn initialize
  "Sets initial db state for local components, local pages, and site-wide events."
  [{:keys [db] :as cofx} _]
  (let [new-db
        (assoc db
               ;; Component Events
               ;; /Nothing here, yet/

               ;; Page Events
               ethlance.ui.page.me.events/state-key
               ethlance.ui.page.me.events/state-default
               ethlance.ui.page.jobs.events/state-key
               ethlance.ui.page.jobs.events/state-default)]

               ;; Main Events
               ;; /Nothing here, yet/
    {:db new-db
     ;; Initialize Forwarded FX Events
     :dispatch-n forwarded-events
     :log/info ["Initialized re-frame app state" (clj->js new-db)]}))

;;
;; Registered Events
;;

(re/reg-event-fx :ethlance/initialize initialize)
