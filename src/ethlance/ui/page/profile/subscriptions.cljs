(ns ethlance.ui.page.profile.subscriptions
  (:require
   [re-frame.core :as re]
   
   [ethlance.ui.page.profile.events :as profile.events]
   [ethlance.ui.subscription.utils :as subscription.utils]))


(def create-get-handler #(subscription.utils/create-get-handler profile.events/state-key %))


;;
;; Registered Subscriptions
;;

;; /Nothing Here, Yet/
