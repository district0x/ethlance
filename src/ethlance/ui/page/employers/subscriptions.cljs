(ns ethlance.ui.page.employers.subscriptions
  (:require
   [re-frame.core :as re]
   
   [ethlance.ui.page.employers.events :as employers.events]
   [ethlance.ui.subscription.utils :as subscription.utils]))


(def create-get-handler #(subscription.utils/create-get-handler employers.events/state-key %))


;;
;; Registered Subscriptions
;;
(re/reg-sub :page.employers/offset (create-get-handler :offset))
(re/reg-sub :page.employers/limit (create-get-handler :limit))
(re/reg-sub :page.employers/skills (create-get-handler :skills))
(re/reg-sub :page.employers/category (create-get-handler :category))
(re/reg-sub :page.employers/feedback-max-rating (create-get-handler :feedback-max-rating))
(re/reg-sub :page.employers/feedback-min-rating (create-get-handler :feedback-min-rating))
(re/reg-sub :page.employers/min-num-feedbacks (create-get-handler :min-num-feedbacks))
(re/reg-sub :page.employers/country (create-get-handler :country))
