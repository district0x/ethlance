(ns ethlance.ui.subscriptions
  (:require
    [district.cljs-utils :as cljs-utils]
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
    [ethlance.ui.page.profile.subscriptions]
    [re-frame.core :as re-frame]))

(re-frame/reg-sub
  ::config
  (fn [db _]
    (get db :ethlance/config)))


(re-frame/reg-sub
  ::active-session
  (fn [db _]
    (get db :active-session)))


(re-frame/reg-sub
  ::active-account-has-session?
  :<- [::active-session]
  :<- [::accounts-subs/active-account]
  (fn [[active-session active-account]]
    (and (cljs-utils/not-nil? (:user/address active-session))
         (= (:user/address active-session) active-account))))


(re-frame/reg-sub
  ::users
  (fn [db _]
    (get db :users)))

(re-frame/reg-sub
  ::active-user
  :<- [::users]
  :<- [::accounts-subs/active-account]
  (fn [[users address]]
    (get users address)))

(re-frame/reg-sub
  ::candidates
  (fn [db _]
    (get db :candidates)))

(re-frame/reg-sub
  ::active-candidate
  :<- [::candidates]
  :<- [::accounts-subs/active-account]
  (fn [[candidates address]]
    (get candidates address)))

(re-frame/reg-sub
  ::employers
  (fn [db _]
    (get db :employers)))

(re-frame/reg-sub
  ::active-employer
  :<- [::employers]
  :<- [::accounts-subs/active-account]
  (fn [[employers address]]
    (get employers address)))

(re-frame/reg-sub
  ::arbiters
  (fn [db _]
    (get db :arbiters)))

(re-frame/reg-sub
  ::active-arbiter
  :<- [::arbiters]
  :<- [::accounts-subs/active-account]
  (fn [[arbiters address]]
    (get arbiters address)))
