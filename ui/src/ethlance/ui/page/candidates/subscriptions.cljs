(ns ethlance.ui.page.candidates.subscriptions
  (:require
   [re-frame.core :as re]
   [ethlance.ui.util.graphql :as graphql-util]
   [ethlance.ui.page.candidates.events :as candidates.events]
   [ethlance.ui.subscription.utils :as subscription.utils]))

(def create-get-handler #(subscription.utils/create-get-handler candidates.events/state-key %))


;;
;; Registered Subscriptions
;;

(re/reg-sub :page.candidates/offset (create-get-handler :offset))
(re/reg-sub :page.candidates/limit (create-get-handler :limit))
(re/reg-sub :page.candidates/skills (create-get-handler :skills))
(re/reg-sub :page.candidates/category (create-get-handler :category))
(re/reg-sub :page.candidates/feedback-max-rating (create-get-handler :feedback-max-rating))
(re/reg-sub :page.candidates/feedback-min-rating (create-get-handler :feedback-min-rating))
(re/reg-sub :page.candidates/min-hourly-rate (create-get-handler :min-hourly-rate))
(re/reg-sub :page.candidates/max-hourly-rate (create-get-handler :max-hourly-rate))
(re/reg-sub :page.candidates/min-num-feedbacks (create-get-handler :min-num-feedbacks))
(re/reg-sub :page.candidates/country (create-get-handler :country))

(re/reg-sub
  :page.candidates/search-params
  (fn [db _]
    {:search-params
     (graphql-util/prepare-search-params
       (get-in db [candidates.events/state-key] {})
       [[:skills #(into [] %)]
        [:category second]
        [:feedback-max-rating]
        [:feedback-min-rating]
        [:min-hourly-rate]
        [:max-hourly-rate]
        [:min-num-feedbacks]
        [:country]])}))
