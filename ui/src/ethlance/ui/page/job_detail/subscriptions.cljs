(ns ethlance.ui.page.job-detail.subscriptions
  (:require
    [ethlance.ui.page.job-detail.events :as job-detail.events]
    [re-frame.core :as re]
    [ethlance.ui.subscription.utils :as subscription.utils]))

(def create-get-handler #(subscription.utils/create-get-handler job-detail.events/state-key %))

(re/reg-sub :page.job-detail/proposal-token-amount (create-get-handler :job/proposal-token-amount))
(re/reg-sub :page.job-detail/proposal-text (create-get-handler :job/proposal-text))

(re/reg-sub
  :page.job-detail/job-stories-updated
  (fn [db query-v]
    (vals (get-in db [:job-stories]))))

(re/reg-sub
  :page.job-detail/all-proposals
  (fn [db [_ queried-job-contract]]
    (let [current-user-address (get-in db [:active-session :user/id])
          contract-from-db (get-in db [:district.ui.router :active-page :params :id])
          contract (or queried-job-contract contract-from-db)
          stories (filter #(= contract (:job/id %)) (vals (:job-stories db)))]
      (->> stories
           (map (fn [story]
                  {:current-user? (= (clojure.string/lower-case current-user-address)
                                     (clojure.string/lower-case (get-in story [:candidate :user :user/id] "")))
                   :job-story/id (:job-story/id story)
                   :candidate-name (get-in story [:candidate :user :user/name])
                   :rate (get-in story [:job-story/proposal-rate])
                   :message (get-in story [:proposal-message :message/text])
                   :created-at (js/parseInt (get-in story [:job-story/date-created]))
                   :status (get-in story [:job-story/status])}))
           ; Keeps the current-user's proposal at the top of the list
           (sort-by (fn [story] [(if (:current-user? story) 1 0) (:created-at story)]))
           reverse))))

(re/reg-sub
  :page.job-detail/active-proposals
  :<- [:page.job-detail/all-proposals]
  (fn [proposals _]
    (filter #(not= :deleted (:status %)) proposals)))

(defn seek
  "Returns first item from coll for which (pred item) returns true.
   Returns nil if no such item is present, or the not-found value if supplied."
  ([pred coll] (seek pred coll nil))
  ([pred coll not-found]
   (reduce (fn [_ x]
             (if (pred x)
               (reduced x)
               not-found))
           not-found coll)))

(re/reg-sub
  :page.job-detail/my-proposal
  :<- [:page.job-detail/all-proposals]
  (fn [proposals _]
    (->> proposals
         (filter #(not= :deleted (:status %)) ,,,)
         (seek :current-user? ,,,))))
