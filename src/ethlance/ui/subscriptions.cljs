(ns ethlance.ui.subscriptions
  "Ethlance re-frame Subscriptions"
  (:require
   [re-frame.core :as re]

   ;; Ethlance Local Component Subscriptions
   [ethlance.ui.component.modal.subscriptions]    ;; :modal/*

   ;; Ethlance Local Page Subscriptions
   [ethlance.ui.page.me.subscriptions]            ;; :page.me/*

   ;; Ethlance Global Subscriptions
   [ethlance.ui.subscription.job-listing]))       ;; :job-listing/*


