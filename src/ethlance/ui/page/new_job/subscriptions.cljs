(ns ethlance.ui.page.new-job.subscriptions
  (:require
   [re-frame.core :as re]
   
   [ethlance.ui.page.new-job.events :as new-job.events]
   [ethlance.ui.subscription.utils :as subscription.utils]))


(def create-get-handler #(subscription.utils/create-get-handler new-job.events/state-key %))


;;
;; Registered Subscriptions
;;

;; /Nothing Here, Yet/
