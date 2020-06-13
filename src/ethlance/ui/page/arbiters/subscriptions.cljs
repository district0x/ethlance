(ns ethlance.ui.page.arbiters.subscriptions
  (:require
   [re-frame.core :as re]

   [ethlance.ui.page.arbiters.events :as arbiters.events]
   [ethlance.ui.subscription.utils :as subscription.utils]))

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
(re/reg-sub :page.arbiters/min-hourly-rate (create-get-handler :min-hourly-rate))
(re/reg-sub :page.arbiters/max-hourly-rate (create-get-handler :max-hourly-rate))
(re/reg-sub :page.arbiters/min-num-feedbacks (create-get-handler :min-num-feedbacks))
(re/reg-sub :page.arbiters/payment-type (create-get-handler :payment-type))
(re/reg-sub :page.arbiters/country (create-get-handler :country))
