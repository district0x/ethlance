(ns ethlance.ui.page.job-detail.events
  (:require [district.ui.router.effects :as router.effects]
            [ethlance.ui.event.utils :as event.utils]
            [district.ui.web3-accounts.queries :as accounts-queries]
            [re-frame.core :as re]))

;; Page State
(def state-key :page.job-detail)
(def state-default
  {})

(def interceptors [re/trim-v])

(defn initialize-page
  "Event FX Handler. Setup listener to dispatch an event when the page is active/visited."
  []
  {::router.effects/watch-active-page
   [{:id :page.job-detail/initialize-page
     :name :route.job/detail
     :dispatch [:page.job-detail/fetch-proposals]}]})

;;
;; Registered Events
;;
(def create-assoc-handler (partial event.utils/create-assoc-handler state-key))

;; TODO: switch based on dev environment
(re/reg-event-fx :page.job-detail/initialize-page initialize-page)
(re/reg-event-fx :page.job-detail/set-proposal-token-amount (create-assoc-handler :job/proposal-token-amount))
(re/reg-event-fx :page.job-detail/set-proposal-text (create-assoc-handler :job/proposal-text))

(def job-story-requested-fields
  [:job-story/id
   :job-story/status
   :job-story/proposal-rate
   :job-story/date-created
   :job/id
   [:candidate
    [:user/id
     [:user [:user/id :user/name]]]]])

(re/reg-event-fx
  :page.job-proposal/send
  [interceptors]
  (fn [{:keys [db]} [contract-address]]
    (let [user-address (accounts-queries/active-account db)
          text (get-in db [state-key :job/proposal-text])
          token-amount (get-in db [state-key :job/proposal-token-amount])
          proposal {:contract contract-address
                    :text text
                    :rate token-amount}]
      {:dispatch [:district.ui.graphql.events/mutation
                  {:queries [[:create-job-proposal {:input proposal}
                              job-story-requested-fields]]
                   :on-success [:page.job-detail/fetch-proposals]}]})))


(re/reg-event-fx
  :page.job-proposal/remove
  [interceptors]
  (fn [{:keys [db]} [job-story-id]]
    (let [user-address (accounts-queries/active-account db)]
      {:db (-> db
               (assoc-in ,,, [state-key :job/proposal-token-amount] nil)
               (assoc-in ,,, [state-key :job/proposal-text] nil))
       :dispatch [:district.ui.graphql.events/mutation
                  {:queries [[:remove-job-proposal {:job-story/id job-story-id}
                              job-story-requested-fields]]
                   :on-success [:page.job-detail/fetch-proposals]}]})))

(re/reg-event-fx
  :page.job-detail/fetch-proposals
  [interceptors]
  (fn [{:keys [db]} [_ router-params]]
    (let [queried-contract-address (:contract router-params)
          contract-from-db (get-in db [:district.ui.router :active-page :params :id])
          contract (or queried-contract-address contract-from-db)]
      {:dispatch [:district.ui.graphql.events/query!
                  {:queries
                   [[:job-story-list {:job-contract contract}
                     [:job-story/id
                      :job/id
                      :job-story/status
                      :job-story/proposal-rate
                      :job-story/date-created
                      :job-story/candidate
                      [:candidate
                       [:user/id
                        [:user [:user/id :user/name]]]]
                      [:proposal-message
                       [:message/id
                        :message/type
                        :message/text
                        :message/creator]]]]]
                  :on-success [:proposal-stories-success]
                  :on-error [:proposal-stories-error]}]})))

(re/reg-event-fx
  :proposal-stories-success
  [interceptors]
  (fn [{:keys [db]} data]
    (let [stories (some :job-story-list data)
          id-mapped (reduce
                      (fn [acc job-story]
                      (assoc acc (:job-story/id job-story) job-story))
                      {}
                      stories)]
    {:db (assoc db :job-stories id-mapped)})))

(re/reg-event-fx
  :proposal-stories-error
  [interceptors]
  (fn [{:keys [db]} error]
    (merge db [:page.job-detail :graphql-error] error)))
