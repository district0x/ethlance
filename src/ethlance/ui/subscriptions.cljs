(ns ethlance.ui.subscriptions
  "Main entry point for all registered subscriptions within re-frame for ethlance."
  (:require
   [re-frame.core :as re]

   ;; Ethlance Local Component Subscriptions
   [ethlance.ui.component.modal.subscriptions]     ;; :modal/*

   ;; Ethlance Local Page Subscriptions
   [ethlance.ui.page.me.subscriptions]             ;; :page.me/*
   [ethlance.ui.page.jobs.subscriptions]           ;; :page.jobs/*
   [ethlance.ui.page.sign-up.subscriptions]        ;; :page.sign-up/*
   [ethlance.ui.page.candidates.subscriptions]     ;; :page.candidates/*
   [ethlance.ui.page.arbiters.subscriptions]       ;; :page.arbiters/*
   [ethlance.ui.page.employers.subscriptions]      ;; :page.employers/*
   [ethlance.ui.page.profile.subscriptions]        ;; :page.profile/*
   [ethlance.ui.page.job-contract.subscriptions]   ;; :page.job-contract/*
   [ethlance.ui.page.job-detail.subscriptions]     ;; :page.job-detail/*
   [ethlance.ui.page.new-job.subscriptions]        ;; :page.new-job/*
   [ethlance.ui.page.invoices.subscriptions]       ;; :page.invoices/*
   [ethlance.ui.page.new-invoice.subscriptions]))  ;; :page.new-invoice/*

   ;; Ethlance Global Subscriptions
   ;; /Nothing here, yet/
