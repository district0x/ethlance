(ns ethlance.ui.page.jobs.subscriptions
  (:require
    [ethlance.ui.page.jobs.events :as jobs.events]
    [ethlance.ui.subscription.utils :as subscription.utils]
    [ethlance.ui.util.graphql :as util.graphql]
    [re-frame.core :as re]))


(def create-get-handler #(subscription.utils/create-get-handler jobs.events/state-key %))


;;
;; Registered Subscriptions
;;

(re/reg-sub :page.jobs/offset (create-get-handler :offset))
(re/reg-sub :page.jobs/limit (create-get-handler :limit))
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
    (let [page-state (get db jobs.events/state-key {})
          filters [[:skills #(into [] %)]
                   [:category second]
                   [:feedback-max-rating]
                   [:feedback-min-rating]
                   [:min-hourly-rate]
                   [:max-hourly-rate]
                   [:min-num-feedbacks]
                   [:payment-type]
                   [:experience-level]]
          filter-params (util.graphql/prepare-search-params page-state filters)]
      {:search-params filter-params
       :offset (:offset page-state)
       :order-by :date-created
       :limit (:limit page-state)})))
