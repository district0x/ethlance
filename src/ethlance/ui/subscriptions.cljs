(ns ethlance.ui.subscriptions
  (:require
   [re-frame.core :as re-frame]
   [district.ui.web3-accounts.subs :as accounts-subs]
   [ethlance.ui.component.modal.subscriptions]
   [ethlance.ui.page.me.subscriptions]
   [ethlance.ui.page.jobs.subscriptions]
   [ethlance.ui.page.sign-up.subscriptions]
   [ethlance.ui.page.candidates.subscriptions]
   [ethlance.ui.page.arbiters.subscriptions]
   [ethlance.ui.page.employers.subscriptions]
   [ethlance.ui.page.profile.subscriptions]
   [ethlance.ui.page.job-contract.subscriptions]
   [ethlance.ui.page.job-detail.subscriptions]
   [ethlance.ui.page.new-job.subscriptions]
   [ethlance.ui.page.invoices.subscriptions]
   [ethlance.ui.page.new-invoice.subscriptions]))

(re-frame/reg-sub
 ::config
 (fn [db _]
   (get db :ethlance/config)))

(re-frame/reg-sub
 ::users
 (fn [db _]
   (get db :users)))

(re-frame/reg-sub
 ::user
 :<- [::users]
 :<- [::accounts-subs/active-account]
 (fn [[users address]]
   (get users address)))

(re-frame/reg-sub
 ::candidates
 (fn [db _]
   (get db :candidates)))

(re-frame/reg-sub
 ::candidate
 :<- [::candidates]
 :<- [::accounts-subs/active-account]
 (fn [[candidates address]]
   (get candidates address)))
