(ns ethlance.ui.page.new-job.subscriptions
  (:require
   [re-frame.core :as re]

   [ethlance.ui.page.new-job.events :as new-job.events]
   [ethlance.ui.subscription.utils :as subscription.utils]))


(def create-get-handler #(subscription.utils/create-get-handler new-job.events/state-key %))


;;
;; Registered Subscriptions
;;

(re/reg-sub :page.new-job/type (create-get-handler :type))
(re/reg-sub :page.new-job/name (create-get-handler :name))
(re/reg-sub :page.new-job/category (create-get-handler :category))
(re/reg-sub :page.new-job/bid-option (create-get-handler :bid-option))
(re/reg-sub :page.new-job/required-experience-level (create-get-handler :required-experience-level))
(re/reg-sub :page.new-job/estimated-project-length (create-get-handler :estimated-project-length))
(re/reg-sub :page.new-job/required-availability (create-get-handler :required-availability))
(re/reg-sub :page.new-job/required-skills (create-get-handler :required-skills))
(re/reg-sub :page.new-job/description (create-get-handler :description))
(re/reg-sub :page.new-job/form-of-payment (create-get-handler :form-of-payment))
(re/reg-sub :page.new-job/token-address (create-get-handler :token-address))
(re/reg-sub :page.new-job/with-arbiter? (create-get-handler :with-arbiter?))
