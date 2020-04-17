(ns ethlance.ui.subscriptions
  "Ethlance re-frame Subscriptions"
  (:require
   [re-frame.core :as re]

   ;; Ethlance Local Component Subscriptions
   [ethlance.ui.component.modal.subscriptions]    ;; :modal/*

   ;; Ethlance Local Page Subscriptions
   [ethlance.ui.page.me.subscriptions]            ;; :page.me/*
   [ethlance.ui.page.jobs.subscriptions]))        ;; :page.jobs/*

   ;; Ethlance Global Subscriptions
   ;; /Nothing here, yet/

