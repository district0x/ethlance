(ns ethlance.ui.subscriptions
  (:require
    [district.cljs-utils :as cljs-utils]
    [ethlance.shared.utils :refer [ilike=]]
    [district.ui.web3-accounts.subs :as accounts-subs]
    [ethlance.ui.component.modal.subscriptions]
    [ethlance.ui.page.arbiters.subscriptions]
    [ethlance.ui.page.candidates.subscriptions]
    [ethlance.ui.page.employers.subscriptions]
    [ethlance.ui.page.invoices.subscriptions]
    [ethlance.ui.page.job-contract.subscriptions]
    [ethlance.ui.page.job-detail.subscriptions]
    [ethlance.ui.page.jobs.subscriptions]
    [ethlance.ui.page.me.subscriptions]
    [ethlance.ui.page.new-invoice.subscriptions]
    [ethlance.ui.page.new-job.subscriptions]
    [re-frame.core :as re]))


(re/reg-sub
  ::config
  (fn [db _]
    (get db :ethlance/config)))


(re/reg-sub
  ::active-session
  (fn [db _]
    (get db :active-session)))


(re/reg-sub
  ::active-account-has-session?
  :<- [::active-session]
  :<- [::accounts-subs/active-account]
  (fn [[active-session active-account]]
    (and (cljs-utils/not-nil? (:user/id active-session))
         (ilike= (:user/id active-session) active-account))))
