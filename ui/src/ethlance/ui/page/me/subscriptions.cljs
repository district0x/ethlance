(ns ethlance.ui.page.me.subscriptions
  (:require
   [re-frame.core :as re]
   [ethlance.ui.subscription.utils :as subscription.utils]
   [ethlance.ui.page.me.events :as me.events]))

(def create-get-handler #(subscription.utils/create-get-handler me.events/state-key %))

(re/reg-sub :page.me/pagination-offset (create-get-handler :pagination-offset))
(re/reg-sub :page.me/pagination-limit (create-get-handler :pagination-limit))


