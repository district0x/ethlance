(ns ethlance.ui.subscriptions
  "Main entry point for all registered subscriptions within re-frame for ethlance."
  (:require
   [re-frame.core :as re]

   ;; Ethlance Local Component Subscriptions
   [ethlance.ui.component.modal.subscriptions]    ;; :modal/*

   ;; Ethlance Local Page Subscriptions
   [ethlance.ui.page.me.subscriptions]            ;; :page.me/*
   [ethlance.ui.page.jobs.subscriptions]          ;; :page.jobs/*
   [ethlance.ui.page.sign-up.subscriptions]))     ;; :page.sign-up/*

   ;; Ethlance Global Subscriptions
   ;; /Nothing here, yet/

