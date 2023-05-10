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
(re/reg-sub :page.candidates/payment-type (create-get-handler :payment-type))
(re/reg-sub :page.candidates/country (create-get-handler :country))

(re/reg-sub
  :page.candidates/search-params
  (fn [db _]
    (let [page-state (get-in db [candidates.events/state-key] {})
          filter-keys [:skills
                       :category
                       :feedback-max-rating
                       :feedback-min-rating
                       :payment-type]
          filter-params (reduce (fn [acc filter-key]
                                  (let [filter-val (get-in db [candidates.events/state-key filter-key])
                                        set-type (type #{})
                                        final-val (if (= set-type (type filter-val))
                                                    (into [] filter-val)
                                                    filter-val)]
                                    (if (not (nil? final-val))
                                      (assoc acc filter-key final-val)
                                      acc)))
                                {}
                                filter-keys)]
      (println ">>> page.candidates/search-params" filter-params)
      {:search-params filter-params})
    ))
