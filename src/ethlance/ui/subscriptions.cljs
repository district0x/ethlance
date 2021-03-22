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
    (and (cljs-utils/not-nil? (:user/address active-session))
         (= (:user/address active-session) active-account))))


(re/reg-sub
  ::users
  (fn [db _]
    (get db :users)))

(re/reg-sub
  ::user
  (fn [db [_ address]]
    (get-in db [:users address])))

(re/reg-sub
  ::active-user
  :<- [::users]
  :<- [::accounts-subs/active-account]
  (fn [[users address]]
    (get users address)))

(re/reg-sub
  ::candidates
  (fn [db _]
    (get db :candidates)))

(re/reg-sub
  ::candidate
  (fn [db [_ address]]
    (get-in db [:candidates address])))

(re/reg-sub
  ::active-candidate
  :<- [::candidates]
  :<- [::accounts-subs/active-account]
  (fn [[candidates address]]
    (get candidates address)))

(re/reg-sub
  ::employers
  (fn [db _]
    (get db :employers)))

(re/reg-sub
  ::active-employer
  :<- [::employers]
  :<- [::accounts-subs/active-account]
  (fn [[employers address]]
    (get employers address)))

(re/reg-sub
  ::arbiters
  (fn [db _]
    (get db :arbiters)))

(re/reg-sub
  ::active-arbiter
  :<- [::arbiters]
  :<- [::accounts-subs/active-account]
  (fn [[arbiters address]]
    (get arbiters address)))

(re/reg-sub
  ::api-errors
  (fn [db _]
    (:api-errors db)))

(re/reg-sub
  ::api-request-in-progress
  (fn [db _]
    (:api-request-in-progress db)))
