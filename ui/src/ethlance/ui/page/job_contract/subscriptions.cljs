(ns ethlance.ui.page.job-contract.subscriptions
  (:require
    [district.parsers :refer [parse-int]]
    [district.ui.router.queries :as ui-router-queries]
    [ethlance.ui.page.job-contract.events :as job-contract.events]
    [ethlance.ui.subscription.utils :as subscription.utils]
    [re-frame.core :as re]))


(def create-get-handler #(subscription.utils/create-get-handler job-contract.events/state-key %))


(re/reg-sub
  :page.job-contract/job-story-id
  (fn [db _]
    (-> (ui-router-queries/active-page-params db) :job-story-id parse-int)))


(re/reg-sub :page.job-contract/message-text (create-get-handler :message-text))

(re/reg-sub :page.job-contract/accept-proposal-message-text (create-get-handler :accept-proposal-message-text))
(re/reg-sub :page.job-contract/accept-invitation-message-text (create-get-handler :accept-invitation-message-text))

(re/reg-sub :page.job-contract/dispute-text (create-get-handler :dispute-text))
(re/reg-sub :page.job-contract/dispute-candidate-percentage (create-get-handler :dispute-candidate-percentage))

(re/reg-sub :page.job-contract/feedback-rating (create-get-handler :feedback-rating))
(re/reg-sub :page.job-contract/feedback-text (create-get-handler :feedback-text))
