(ns ethlance.ui.page.new-invoice.subscriptions
  (:require
   [re-frame.core :as re]
   
   [ethlance.ui.page.new-invoice.events :as new-invoice.events]
   [ethlance.ui.subscription.utils :as subscription.utils]))


(def create-get-handler #(subscription.utils/create-get-handler new-invoice.events/state-key %))


;;
;; Registered Subscriptions
;;

;; /Nothing Here, Yet/
