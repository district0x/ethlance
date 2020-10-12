(ns ethlance.ui.page.invoices.subscriptions
  (:require [ethlance.ui.page.invoices.events :as invoices.events]
            [ethlance.ui.subscription.utils :as subscription.utils]))

(def create-get-handler #(subscription.utils/create-get-handler invoices.events/state-key %))
