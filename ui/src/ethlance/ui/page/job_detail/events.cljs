(ns ethlance.ui.page.job-detail.events
  (:require [district.ui.router.effects :as router.effects]
            [ethlance.ui.event.utils :as event.utils]
            [ethlance.shared.utils :refer [eth->wei]]
            [district.ui.web3-accounts.queries :as accounts-queries]
            [re-frame.core :as re]))

;; Page State
(def state-key :page.job-detail)
(def state-default
  {:proposal-offset 0
   :proposal-limit 3})

(def interceptors [re/trim-v])

(defn initialize-page
  "Event FX Handler. Setup listener to dispatch an event when the page is active/visited."
  [{:keys [db]}]
  (let [page-state (get db state-key)]
    {::router.effects/watch-active-page
     [{:id :page.job-detail/initialize-page
       :name :route.job/detail
       :dispatch [:page.job-detail/fetch-proposals]
       }]
     :db (assoc-in db [state-key] state-default)}))

;;
;; Registered Events
;;
(def create-assoc-handler (partial event.utils/create-assoc-handler state-key))

;; TODO: switch based on dev environment
(re/reg-event-fx :page.job-detail/initialize-page initialize-page)
(re/reg-event-fx :page.job-detail/set-proposal-token-amount (create-assoc-handler :job/proposal-token-amount))
(re/reg-event-fx :page.job-detail/set-proposal-text (create-assoc-handler :job/proposal-text))
(re/reg-event-fx :page.job-detail/set-proposal-offset (create-assoc-handler :proposal-offset))

(def job-story-requested-fields
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
     :message/creator]]])

(re/reg-event-fx
  :page.job-proposal/send
  [interceptors]
  (fn [{:keys [db]} [contract-address]]
    (let [user-address (accounts-queries/active-account db)
          text (get-in db [state-key :job/proposal-text])
          token-amount (get-in db [state-key :job/proposal-token-amount])
          proposal {:contract contract-address
                    :text text
                    :rate (js/parseFloat (eth->wei token-amount))}]
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
                   [[:job-story-search {:search-params {:job contract}
                                        :limit (get-in db [state-key :proposal-limit])
                                        :offset (get-in db [state-key :proposal-offset])}
                     [:total-count
                      [:items job-story-requested-fields]]]]
                  :on-success [:proposal-stories-success]
                  :on-error [:proposal-stories-error]}]})))

(re/reg-event-fx
  :proposal-stories-success
  [interceptors]
  (fn [{:keys [db]} data]
    (let [result (some :job-story-search data)
          stories (get-in result [:items])
          id-mapped (reduce
                      (fn [acc job-story]
                      (assoc acc (:job-story/id job-story) job-story))
                      {}
                      stories)]
      {:db (-> db
               (assoc ,,, :job-stories id-mapped)
               (assoc-in ,,, [state-key :proposal-total-count] (:total-count result)))})))

(re/reg-event-fx
  :proposal-stories-error
  [interceptors]
  (fn [{:keys [db]} error]
    (merge db [state-key :graphql-error] error)))
