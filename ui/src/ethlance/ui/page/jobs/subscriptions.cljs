(ns ethlance.ui.page.jobs.subscriptions
  (:require
   [re-frame.core :as re]

   [ethlance.ui.page.jobs.events :as jobs.events]
   [ethlance.ui.subscription.utils :as subscription.utils]))


(def create-get-handler #(subscription.utils/create-get-handler jobs.events/state-key %))


;;
;; Registered Subscriptions
;;

(re/reg-sub :page.jobs/job-listing (create-get-handler :job-listing))
(re/reg-sub :page.jobs/job-listing-state (create-get-handler :job-listing/state))
(re/reg-sub :page.jobs/skills (create-get-handler :skills))
(re/reg-sub :page.jobs/category (create-get-handler :category))
(re/reg-sub :page.jobs/feedback-max-rating (create-get-handler :feedback-max-rating))
(re/reg-sub :page.jobs/feedback-min-rating (create-get-handler :feedback-min-rating))
(re/reg-sub :page.jobs/min-hourly-rate (create-get-handler :min-hourly-rate))
(re/reg-sub :page.jobs/max-hourly-rate (create-get-handler :max-hourly-rate))
(re/reg-sub :page.jobs/min-num-feedbacks (create-get-handler :min-num-feedbacks))
(re/reg-sub :page.jobs/payment-type (create-get-handler :payment-type))
(re/reg-sub :page.jobs/experience-level (create-get-handler :experience-level))

(re/reg-sub
  :page.jobs/job-search-params
  (fn [db _]
    (let [page-state (get-in db [jobs.events/state-key] {})
          filters [[:skills #(into [] %)]
                   [:category second]
                   [:feedback-max-rating]
                   [:feedback-min-rating]
                   [:min-hourly-rate]
                   [:max-hourly-rate]
                   [:min-num-feedbacks]
                   [:payment-type]
                   [:experience-level]]
          filter-params (reduce (fn [acc [filter-key & transformers]]
                                  (let [filter-val (reduce #(%2 %1)
                                                           (get-in db [jobs.events/state-key filter-key])
                                                           (or transformers []))]
                                    (if (or (nil? filter-val) ; Don't add nil or empty collections to the search
                                            (and (sequential? filter-val)
                                                 (empty? filter-val)))
                                      acc
                                      (assoc acc filter-key filter-val))))
                                {}
                                filters)]
      {:search-params filter-params})))
