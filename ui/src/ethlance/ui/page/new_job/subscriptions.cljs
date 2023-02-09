(ns ethlance.ui.page.new-job.subscriptions
  (:require
   [re-frame.core :as re]

   [ethlance.ui.page.new-job.events :as new-job.events]
   [ethlance.ui.subscription.utils :as subscription.utils]))


(def create-get-handler #(subscription.utils/create-get-handler new-job.events/state-key %))


;;
;; Registered Subscriptions
;;

(re/reg-sub :page.new-job/bid-option (create-get-handler :job/bid-option))
(re/reg-sub :page.new-job/category (create-get-handler :job/category))
(re/reg-sub :page.new-job/description (create-get-handler :job/description))
(re/reg-sub :page.new-job/estimated-project-length (create-get-handler :job/estimated-project-length))
(re/reg-sub :page.new-job/form-of-payment (create-get-handler :job/form-of-payment))
(re/reg-sub :page.new-job/required-availability (create-get-handler :job/required-availability))
(re/reg-sub :page.new-job/required-experience-level (create-get-handler :job/required-experience-level))
(re/reg-sub :page.new-job/required-skills (create-get-handler :job/required-skills))
(re/reg-sub :page.new-job/title (create-get-handler :job/title))
(re/reg-sub :page.new-job/token-address (create-get-handler :job/token-address))
(re/reg-sub :page.new-job/with-arbiter? (create-get-handler :job/with-arbiter?))


(re/reg-sub :page.new-job/form (fn [db] (get db new-job.events/state-key)))
