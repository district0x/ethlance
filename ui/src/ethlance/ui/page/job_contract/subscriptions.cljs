(ns ethlance.ui.page.job-contract.subscriptions
  (:require [ethlance.ui.page.job-contract.events :as job-contract.events]
            [ethlance.ui.subscription.utils :as subscription.utils]
            [ethlance.shared.utils :refer [ilike=]]
            [district.ui.graphql.queries :as ui-gql-queries]
            [district.ui.router.queries :as ui-router-queries]
            [district.parsers :refer [parse-int]]
            [re-frame.core :as re]))

(def create-get-handler #(subscription.utils/create-get-handler job-contract.events/state-key %))

(re/reg-sub
  :page.job-contract/job-story-id
  (fn [db _]
    (-> (ui-router-queries/active-page-params db) :job-story-id parse-int)))

(re/reg-sub :page.job-contract/message-text (create-get-handler :message-text))
(re/reg-sub :page.job-contract/message-recipient (create-get-handler :message-recipient))
(re/reg-sub :page.job-contract/feedback-rating (create-get-handler :feedback-rating))
(re/reg-sub :page.job-contract/feedback-text (create-get-handler :feedback-text))
(re/reg-sub :page.job-contract/feedback-recipient (create-get-handler :feedback-recipient))
