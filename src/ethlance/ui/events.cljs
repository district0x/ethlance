(ns ethlance.ui.events
  "Main entry point for all registered events within re-frame for ethlance."
  (:require 
   [re-frame.core :as re]

   ;; District UI Events
   [district.ui.logging.events]

   ;; Ethlance Component Event Handlers
   [ethlance.ui.component.modal.events]    ;; :modal/*

   ;; Ethlance Page Event Handlers
   [ethlance.ui.page.me.events]            ;; :page.me/*
   [ethlance.ui.page.jobs.events]          ;; :page.jobs/*
   [ethlance.ui.page.sign-up.events]       ;; :page.sign-up/*
   [ethlance.ui.page.candidates.events]    ;; :page.candidates/*
   [ethlance.ui.page.arbiters.events]      ;; :page.arbiters/*
   [ethlance.ui.page.employers.events]     ;; :page.employers/*
   [ethlance.ui.page.profile.events]       ;; :page.profile/*
   [ethlance.ui.page.job-contract.events]  ;; :page.job-contract/*
   [ethlance.ui.page.job-detail.events]    ;; :page.job-detail/*
   [ethlance.ui.page.new-job.events]       ;; :page.new-job/*
   [ethlance.ui.page.invoices.events]      ;; :page.invoices/*
   [ethlance.ui.page.new-invoice.events]   ;; :page.new-invoice/*

   ;; Ethlance Main Event Handlers
   [ethlance.ui.event.sign-in]))           ;; :user/*


(def forwarded-events
  "Forwarded Events.

   Notes:

   - district.ui.router/watch-active-page effect handler uses forwarded events
   - Additional info: https://github.com/day8/re-frame-forward-events-fx"
  (list
   [:page.jobs/initialize-page
    :page.sign-up/initialize-page
    :page.candidates/initialize-page
    :page.arbiters/initialize-page
    :page.employers/initialize-page
    :page.profile/initialize-page
    :page.job-contract/initialize-page
    :page.job-detail/initialize-page
    :page.new-job/initialize-page
    :page.invoices/initialize-page
    :page.new-invoice/initialize-page]))


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
               ethlance.ui.page.jobs.events/state-default
               ethlance.ui.page.candidates.events/state-key
               ethlance.ui.page.candidates.events/state-default
               ethlance.ui.page.arbiters.events/state-key
               ethlance.ui.page.arbiters.events/state-default
               ethlance.ui.page.employers.events/state-key
               ethlance.ui.page.employers.events/state-default
               ethlance.ui.page.profile.events/state-key
               ethlance.ui.page.profile.events/state-default
               ethlance.ui.page.job-contract.events/state-key
               ethlance.ui.page.job-contract.events/state-default
               ethlance.ui.page.job-detail.events/state-key
               ethlance.ui.page.job-detail.events/state-default
               ethlance.ui.page.new-job.events/state-key
               ethlance.ui.page.new-job.events/state-default
               ethlance.ui.page.invoices.events/state-key
               ethlance.ui.page.invoices.events/state-default
               ethlance.ui.page.new-invoice.events/state-key
               ethlance.ui.page.new-invoice.events/state-default)]

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
