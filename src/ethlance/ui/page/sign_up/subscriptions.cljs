(ns ethlance.ui.page.sign-up.subscriptions
  (:require
   [re-frame.core :as re]

   [ethlance.ui.page.sign-up.events :as sign-up.events]
   [ethlance.ui.subscription.utils :as subscription.utils]))


(def create-get-handler #(subscription.utils/create-get-handler sign-up.events/state-key %))


;;
;; Registered Subscriptions
;;

;; Nothing here, yet
