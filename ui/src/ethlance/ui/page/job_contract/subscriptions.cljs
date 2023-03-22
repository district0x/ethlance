(ns ethlance.ui.page.job-contract.subscriptions
  (:require [ethlance.ui.page.job-contract.events :as job-contract.events]
            [ethlance.ui.subscription.utils :as subscription.utils]
            [re-frame.core :as re]))

(def create-get-handler #(subscription.utils/create-get-handler job-contract.events/state-key %))

(re/reg-sub :page.job-contract/feedback-rating (create-get-handler :feedback-rating))
(re/reg-sub :page.job-contract/feedback-text (create-get-handler :feedback-text))
(re/reg-sub :page.job-contract/feedback-recipient (create-get-handler :feedback-recipient))
