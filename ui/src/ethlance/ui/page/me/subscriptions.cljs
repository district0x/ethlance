(ns ethlance.ui.page.me.subscriptions
  (:require
    [ethlance.ui.page.me.events :as me.events]
    [ethlance.ui.subscription.utils :as subscription.utils]
    [re-frame.core :as re]))


(def create-get-handler #(subscription.utils/create-get-handler me.events/state-key %))

(re/reg-sub :page.me/pagination-offset (create-get-handler :pagination-offset))
(re/reg-sub :page.me/pagination-limit (create-get-handler :pagination-limit))
