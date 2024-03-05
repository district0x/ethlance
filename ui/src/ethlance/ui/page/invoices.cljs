(ns ethlance.ui.page.invoices
  (:require
    [district.ui.component.page :refer [page]]
    [district.ui.graphql.subs :as gql]
    [district.ui.router.subs :as router.subs]
    [ethlance.shared.utils :as shared-utils]
    [ethlance.ui.component.icon :refer [c-icon]]
    [ethlance.ui.component.main-layout :refer [c-main-layout]]
    [ethlance.ui.component.profile-image :refer [c-profile-image]]
    [ethlance.ui.component.rating :refer [c-rating]]
    [ethlance.ui.component.token-info :refer [c-token-info]]
    [ethlance.ui.util.dates :refer [relative-ago formatted-date]]
    [ethlance.ui.util.navigation :as util.navigation]
    [ethlance.ui.util.tokens :as tokens]
    [re-frame.core :as re]
    [reagent.ratom]))


(defn c-participant-user-info
  [data-prefix data]
  [:div.profile.employer
   [:div.label (clojure.string/capitalize (name data-prefix))]
   [c-profile-image {:src (get-in data [:user :user/profile-image])}]
   [:div.name (get-in data [:user :user/name])]
   [:div.rating
    [c-rating {:rating (get-in data [(keyword data-prefix "rating")])}]
    [:span.num-feedback (str "(" (get-in data [(keyword data-prefix "feedback") :total-count]) ")")]]
   [:div.location (get-in data [:user :user/country])]])


(defmethod page :route.invoice/index []
  (fn []
    (let [page-params (re/subscribe [:district.ui.router.subs/active-page-params])
          contract-address (:job-id @page-params)
          invoice-id (int (:invoice-id @page-params))
          query [:job {:job/id contract-address}
                 [:job/token-type
                  :job/title
                  [:job/employer
                   [:user/id
                    :employer/rating
                    [:employer/feedback [:total-count]]
                    [:user [:user/name :user/country :user/profile-image]]]]
                  [:job/arbiter
                   [:user/id
                    :arbiter/rating
                    [:arbiter/feedback [:total-count]]
                    [:user [:user/name :user/country :user/profile-image]]]]
                  [:token-details
                   [:token-detail/id
                    :token-detail/type
                    :token-detail/name
                    :token-detail/symbol
                    :token-detail/decimals]]
                  [:invoice {:invoice/id invoice-id :job/id contract-address}
                   [:id
                    :job/id
                    :invoice/id
                    :job-story/id
                    :invoice/status
                    :invoice/amount-requested
                    :invoice/amount-paid
                    :invoice/hours-worked
                    :invoice/hourly-rate
                    [:job-story
                     [:job-story/id
                      [:candidate
                       [:user/id
                        :candidate/rating
                        [:candidate/feedback [:total-count]]
                        [:user
                         [:user/name
                          :user/country
                          :user/profile-image]]]]]]
                    [:creation-message
                     [:message/id :message/date-created]]]]]]

          result @(re/subscribe [::gql/query {:queries [query]}
                                 {:refetch-on #{:page.invoices/refetch-invoice}}])

          job-token-symbol (get-in result [:job :token-details :token-detail/symbol])
          job (:job result)
          job-title (:title job)
          invoice (get-in job [:invoice])
          employer (get-in job [:job/employer])
          arbiter (get-in job [:job/arbiter])
          candidate (get-in invoice [:job-story :candidate])

          invoice-to-pay {:job/id contract-address
                          :invoice-id (:invoice/id invoice)
                          :invoice-message-id (get-in invoice [:creation-message :message/id])
                          :job-story/id (:job-story/id invoice)
                          :payer (:user/id employer)
                          :receiver (:user/id candidate)}

          invoice-payable? (not= "paid" (get-in invoice [:invoice/status]))
          info-panel [["Invoiced Amount" (when-not (:graphql/loading? result)
                                           [c-token-info (:invoice/amount-requested invoice)
                                            (:token-details job)])]
                      ["Hours Worked" (get-in invoice [:invoice/hours-worked])]
                      ["Hourly Rate" (get-in invoice [:invoice/hourly-rate])]
                      ["Invoiced On" (formatted-date #(get-in % [:creation-message :message/date-created]) invoice)]]]
      [c-main-layout {:container-opts {:class :invoice-detail-main-container}}
       [:div.title "Invoice"]
       [:a.sub-title (util.navigation/link-params {:route :route.job/detail
                                                   :params {:id (:job/id invoice)}}) (:job/title job)]
       [:div.invoice-status [:span.label (:invoice/status invoice)]]

       [:div.left
        [c-participant-user-info :employer employer]
        [c-participant-user-info :candidate candidate]
        (when arbiter [c-participant-user-info :arbiter arbiter])]

       [:div.right
        [:div.ethlance-table
         [:table (into [:tbody] (map (fn [[label content]] [:tr [:th label] [:td content]]) info-panel))]]]

       (if invoice-payable?
         [:div.button {:on-click #(re/dispatch [:page.invoices/pay invoice-to-pay])}
          [:span "Pay Invoice"]
          [c-icon {:name :ic-arrow-right :size :small :color :white}]]
         [:div.button {:style {:background-color :gray}}
          [:span "Invoice Paid"]])])))
