(ns ethlance.ui.page.new-invoice.subscriptions
  (:require
   [re-frame.core :as re]
   
   [ethlance.ui.page.new-invoice.events :as new-invoice.events]
   [ethlance.ui.subscription.utils :as subscription.utils]))


(def create-get-handler #(subscription.utils/create-get-handler new-invoice.events/state-key %))


;;
;; Registered Subscriptions
;;


(re/reg-sub :page.new-invoice/job-name-listing (create-get-handler :job-name-listing))
(re/reg-sub :page.new-invoice/job-name (create-get-handler :job-name))
(re/reg-sub :page.new-invoice/hours-worked (create-get-handler :hours-worked))
(re/reg-sub :page.new-invoice/hourly-rate (create-get-handler :hourly-rate))
(re/reg-sub :page.new-invoice/invoice-amount (create-get-handler :invoice-amount))
(re/reg-sub :page.new-invoice/message (create-get-handler :message))
