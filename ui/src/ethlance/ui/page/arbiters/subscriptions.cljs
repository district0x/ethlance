(ns ethlance.ui.page.arbiters.subscriptions
  (:require
    [ethlance.ui.page.arbiters.events :as arbiters.events]
    [ethlance.ui.subscription.utils :as subscription.utils]
    [ethlance.ui.util.graphql :as graphql-util]
    [re-frame.core :as re]))


(def create-get-handler #(subscription.utils/create-get-handler arbiters.events/state-key %))


;;
;; Registered Subscriptions
;;
(re/reg-sub :page.arbiters/offset (create-get-handler :offset))
(re/reg-sub :page.arbiters/limit (create-get-handler :limit))
(re/reg-sub :page.arbiters/skills (create-get-handler :skills))
(re/reg-sub :page.arbiters/category (create-get-handler :category))
(re/reg-sub :page.arbiters/feedback-max-rating (create-get-handler :feedback-max-rating))
(re/reg-sub :page.arbiters/feedback-min-rating (create-get-handler :feedback-min-rating))
(re/reg-sub :page.arbiters/min-fee (create-get-handler :min-fee))
(re/reg-sub :page.arbiters/max-fee (create-get-handler :max-fee))
(re/reg-sub :page.arbiters/min-num-feedbacks (create-get-handler :min-num-feedbacks))
(re/reg-sub :page.arbiters/payment-type (create-get-handler :payment-type))
(re/reg-sub :page.arbiters/country (create-get-handler :country))


(re/reg-sub
  :page.arbiters/search-params
  (fn [db _]
    {:offset (get-in db [arbiters.events/state-key :offset])
     :limit (get-in db [arbiters.events/state-key :limit])  :search-params
     (graphql-util/prepare-search-params
       (get db arbiters.events/state-key {})
       [[:skills #(into [] %)]
        [:category second]
        [:feedback-min-rating]
        [:feedback-max-rating]
        [:min-fee]
        [:max-fee]
        [:min-num-feedbacks]
        [:country]])}))
