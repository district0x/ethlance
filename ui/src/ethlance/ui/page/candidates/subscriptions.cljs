(ns ethlance.ui.page.candidates.subscriptions
  (:require
   [re-frame.core :as re]

   [ethlance.ui.page.candidates.events :as candidates.events]
   [ethlance.ui.subscription.utils :as subscription.utils]))

(def create-get-handler #(subscription.utils/create-get-handler candidates.events/state-key %))


;;
;; Registered Subscriptions
;;

(re/reg-sub :page.candidates/offset (create-get-handler :offset))
(re/reg-sub :page.candidates/limit (create-get-handler :limit))
(re/reg-sub :page.candidates/skills (create-get-handler :skills))
(re/reg-sub :page.candidates/category (create-get-handler :category))
(re/reg-sub :page.candidates/feedback-max-rating (create-get-handler :feedback-max-rating))
(re/reg-sub :page.candidates/feedback-min-rating (create-get-handler :feedback-min-rating))
(re/reg-sub :page.candidates/country (create-get-handler :country))

(re/reg-sub
  :page.candidates/search-params
  (fn [db _]
    (let [page-state (get-in db [candidates.events/state-key] {})
          filters [[:skills #(into [] %)]
                   [:category second]
                   [:feedback-max-rating]
                   [:feedback-min-rating]
                   [:min-hourly-rate]
                   [:max-hourly-rate]
                   [:min-num-feedbacks]
                   [:country]]
          filter-params (reduce (fn [acc [filter-key & transformers]]
                                  (let [filter-val (reduce #(%2 %1)
                                                           (get-in db [candidates.events/state-key filter-key])
                                                           (or transformers []))]
                                    (if (or (nil? filter-val) ; Don't add nil or empty collections to the search
                                            (and (sequential? filter-val)
                                                 (empty? filter-val)))
                                      acc
                                      (assoc acc filter-key filter-val))))
                                {}
                                filters)]
      {:search-params filter-params})
    ))
