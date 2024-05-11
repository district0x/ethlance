(ns ethlance.ui.events
  (:require
    [akiroz.re-frame.storage]
    [day8.re-frame.forward-events-fx]
    [district.ui.web3.queries :as web3-queries]
    [cljs-web3-next.utils :as w3n-utils]
    [district.ui.web3-accounts.events :as accounts-events]
    [district.ui.web3-account-balances.events :as account-balances-events]
    [ethlance.ui.page.arbiters.events]
    [ethlance.ui.page.candidates.events]
    [ethlance.ui.page.home.events]
    [ethlance.ui.page.invoices.events]
    [ethlance.ui.page.job-contract.events]
    [ethlance.ui.page.job-detail.events]
    [ethlance.ui.page.jobs.events]
    [ethlance.ui.page.me.events]
    [ethlance.ui.page.new-invoice.events]
    [ethlance.ui.page.new-job.events]
    [ethlance.ui.page.profile.events]
    [ethlance.ui.page.sign-up.events]
    [print.foo]
    [re-frame.core :as re]))


(re/reg-event-fx
  ::accounts-changed
  (fn [{:keys [db]} [_event-name [new-active-account]]]
    {:fx [[:dispatch [::accounts-events/set-active-account
                      (w3n-utils/address->checksum (web3-queries/web3 db) new-active-account)]]
          [:dispatch [::account-balances-events/load-account-balances]]]}))

(re/reg-event-fx
  ::listen-account-changes
  (fn [_cofx _event]
    {:fx [[:district.ui.web3-accounts.effects/watch-accounts {:on-change [::accounts-changed]}]]}))

(re/reg-event-fx
  :ethlance/initialize
  [(re/inject-cofx :store)]
  (fn [{:keys [db]} [_ config]]
    (let [updated-db (-> db
                         (assoc :ethlance/config config)
                         (assoc :active-session (select-keys (akiroz.re-frame.storage/<-store :ethlance) [:jwt :user/id])))]
      {:db updated-db
       :dispatch-n
       [[:district.ui.graphql.events/set-authorization-token (get-in updated-db [:active-session :jwt])]
        [:page.home/initialize-page]
        [:page.jobs/initialize-page]
        [:page.me/initialize-page]
        [:page.sign-up/initialize-page]
        [:page.candidates/initialize-page]
        [:page.arbiters/initialize-page]
        [:page.profile/initialize-page]
        [:page.job-contract/initialize-page]
        [:page.job-detail/initialize-page]
        [:page.new-job/initialize-page]
        [:page.invoices/initialize-page]
        [:page.new-invoice/initialize-page]]
       :dispatch-later [{:ms 1000 :dispatch [::listen-account-changes]}]})))
