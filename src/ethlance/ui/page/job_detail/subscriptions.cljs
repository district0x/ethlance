(ns ethlance.ui.page.job-detail.subscriptions
  (:require
   [re-frame.core :as re]
   
   [ethlance.ui.page.job-detail.events :as job-detail.events]
   [ethlance.ui.subscription.utils :as subscription.utils]))


(def create-get-handler #(subscription.utils/create-get-handler job-detail.events/state-key %))


;;
;; Registered Subscriptions
;;

;; /Nothing Here, Yet/
