(ns ethlance.ui.page.job-contract.subscriptions
  (:require
   [re-frame.core :as re]
   
   [ethlance.ui.page.job-contract.events :as job-contract.events]
   [ethlance.ui.subscription.utils :as subscription.utils]))


(def create-get-handler #(subscription.utils/create-get-handler job-contract.events/state-key %))


;;
;; Registered Subscriptions
;;

;; /Nothing Here, Yet/
