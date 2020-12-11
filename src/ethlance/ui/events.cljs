(ns ethlance.ui.events
  (:require
    [day8.re-frame.forward-events-fx]
    [ethlance.ui.page.arbiters.events]
    [ethlance.ui.page.candidates.events]
    [ethlance.ui.page.employers.events]
    [ethlance.ui.page.invoices.events]
    [ethlance.ui.page.job-contract.events]
    [ethlance.ui.page.job-detail.events]
    [ethlance.ui.page.jobs.events]
    [ethlance.ui.page.me.events]
    [ethlance.ui.page.new-invoice.events]
    [ethlance.ui.page.new-job.events]
    [ethlance.ui.page.profile.events]
    [ethlance.ui.page.sign-up.events]
    [district.ui.web3-accounts.events]
    [print.foo]
    [re-frame.core :as re]))

(re/reg-event-fx
  :ethlance/initialize
  [(re/inject-cofx :store)]
  (fn [{:keys [db store]} [_ config]]
    {:db (-> db
           (assoc :ethlance/config config)
           (merge (print.foo/look store)))
     :dispatch-n [[:page.jobs/initialize-page]
                  [:page.sign-up/initialize-page]
                  [:page.candidates/initialize-page]
                  [:page.arbiters/initialize-page]
                  [:page.employers/initialize-page]
                  [:page.profile/initialize-page]
                  [:page.job-contract/initialize-page]
                  [:page.job-detail/initialize-page]
                  [:page.new-job/initialize-page]
                  [:page.invoices/initialize-page]
                  [:page.new-invoice/initialize-page]]}))
