(ns ethlance.ui.page.profile.subscriptions
  (:require [ethlance.ui.page.profile.events :as profile.events]
            [ethlance.ui.subscription.utils :as subscription.utils]
            [re-frame.core :as re]))

(def create-get-handler #(subscription.utils/create-get-handler profile.events/state-key %))

(re/reg-sub
  ::candidate-jobs
  (fn [db candidate-id]
    [{:title "Sometimes lose, always win" :accepted-at "2021-02-01"}]))

(re/reg-sub
  ::employer-jobs
  (fn [db employer-id]
    [{:title "Make Dogecoin great again" :accepted-at "2021-02-02" :status "🚀🌙"}]))

(re/reg-sub
  ::arbiter-jobs
  (fn [db employer-id]
    [{:title "Watch me now, I'm going down" :accepted-at "2021-02-03"}]))
