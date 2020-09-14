(ns ethlance.ui.events
  "Main entry point for all registered events within re-frame for ethlance."
  (:require
   ethlance.ui.page.arbiters.events
   ethlance.ui.page.candidates.events
   ethlance.ui.page.employers.events
   ethlance.ui.page.invoices.events
   ethlance.ui.page.job-contract.events
   ethlance.ui.page.job-detail.events
   ethlance.ui.page.jobs.events
   ethlance.ui.page.me.events
   ethlance.ui.page.new-invoice.events
   ethlance.ui.page.new-job.events
   ethlance.ui.page.profile.events
   ethlance.ui.page.sign-up.events
   [taoensso.timbre :as log]
   [re-frame.core :as re]))

(def forwarded-events
  "Forwarded Events.

   Notes:

   - district.ui.router/watch-active-page effect handler uses forwarded events
   - Additional info: https://github.com/day8/re-frame-forward-events-fx"
  [[:page.jobs/initialize-page]
   [:page.sign-up/initialize-page]
   [:page.candidates/initialize-page]
   [:page.arbiters/initialize-page]
   [:page.employers/initialize-page]
   [:page.profile/initialize-page]
   [:page.job-contract/initialize-page]
   [:page.job-detail/initialize-page]
   [:page.new-job/initialize-page]
   [:page.invoices/initialize-page]
   [:page.new-invoice/initialize-page]])

(defn initialize
  "Sets initial db state for local components, local pages, and site-wide events."
  [{:keys [db] :as cofx} [_ config]]
  {:db (assoc db :ethlance/config config)
   :dispatch-n forwarded-events})

(re/reg-event-fx :ethlance/initialize initialize)
