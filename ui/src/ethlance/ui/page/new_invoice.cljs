(ns ethlance.ui.page.new-invoice
  (:require
    [district.ui.component.page :refer [page]]
    [district.ui.graphql.subs :as gql]
    [ethlance.ui.component.button :refer [c-button c-button-label]]
    [ethlance.ui.component.main-layout :refer [c-main-layout]]
    [ethlance.ui.component.select-input :refer [c-select-input]]
    [ethlance.ui.component.textarea-input :refer [c-textarea-input]]
    [ethlance.ui.component.token-amount-input :refer [c-token-amount-input]]
    [ethlance.ui.component.token-info :refer [c-token-info]]
    [re-frame.core :as re]))


(defn truncate [length ending string]
  (cond
    (nil? string)
    ""

    (< (count string) (+ length 3))
    string

    :else
    (str
      (subs string 0 (min (count string) length))
      ending)))

(defmethod page :route.invoice/new []
  (let [active-user (:user/id @(re/subscribe [:ethlance.ui.subscriptions/active-session]))
        query [:candidate {:user/id active-user}
               [:user/id
                [:job-stories
                 [:total-count
                  [:items
                   [:job-story/id
                    :job/id
                    :job-story/date-created
                    [:job
                     [:job/id
                      :job/title
                      :job/token-address
                      :job/token-amount
                      :job/token-type
                      :job/token-id
                      :balance
                      [:token-details
                       [:token-detail/id
                        :token-detail/type
                        :token-detail/name
                        :token-detail/decimals
                        :token-detail/symbol]]]]]]]]]]
        search-results (re/subscribe [::gql/query {:queries [query]}
                                      {:id :CandidateJobStoriesForInvoice
                                       :refetch-on [:page.new-invoice/set-invoiced-job]}])]
    (fn []
      (let [*invoiced-job (re/subscribe [:page.new-invoice/invoiced-job])
            *hours-worked (re/subscribe [:page.new-invoice/hours-worked])
            *hourly-rate (re/subscribe [:page.new-invoice/hourly-rate])
            *invoice-amount (re/subscribe [:page.new-invoice/invoice-amount])
            *message (re/subscribe [:page.new-invoice/message])
            job-token (re/subscribe [:page.new-invoice/job-token])
            estimated-usd (re/subscribe [:page.new-invoice/estimated-usd])
            *job-listing (->> @search-results
                              first
                              :candidate
                              :job-stories
                              :items
                              (sort-by :job-story/date-created ,,,)
                              reverse)
            token-display-name (name (or (@job-token :symbol) (@job-token :type) ""))
            job-token-details (get-in @*invoiced-job [:job :token-details])
            job-token-decimals (:token-detail/decimals job-token-details)
            balance-left (get-in @*invoiced-job [:job :balance])
            show-balance-left? (not (nil? balance-left))
            no-job-selected? (nil? @*invoiced-job)
            focus-on-element (fn [id _event] (.focus (.getElementById js/document id)))
            validations (re/subscribe [:page.new-invoice/validations])
            tx-in-progress? @(re/subscribe [:page.new-invoice/tx-in-progress?])
            button-disabled? (not (every? true? (vals @validations)))]
        [c-main-layout {:container-opts {:class :new-invoice-main-container}}
         [:div.title "New Invoice"]
         [:div.left-form
          [:div.input-stripe
           [:div.label "Job"]
           [c-select-input
            {:selections *job-listing
             :value-fn (fn [job-story] (str (:job/id job-story) "-" (:job-story/id job-story)))
             :label-fn (comp #(truncate 50 "..." %)
                             :job/title
                             :job)
             :selection @*invoiced-job
             :on-select #(re/dispatch [:page.new-invoice/set-invoiced-job %])}]]
          [:div.input-stripe {:on-click (partial focus-on-element "invoice-hours-input")}
           [:div.label "Hours Worked"]
           [:input
            {:id "invoice-hours-input"
             :type "number"
             :min 0
             :value @*hours-worked
             :disabled no-job-selected?
             :on-change #(re/dispatch [:page.new-invoice/set-hours-worked (-> % .-target .-value)])}]
           [:div.post-label "h"]]
          [:div.input-stripe {:on-click (partial focus-on-element "invoice-hourly-rate-input")}
           [:div.label "Hourly Rate"]
           [:input
            {:id "invoice-hourly-rate-input"
             :type "number"
             :min 0
             :value @*hourly-rate
             :disabled no-job-selected?
             :on-change #(re/dispatch [:page.new-invoice/set-hourly-rate (-> % .-target .-value)])}]
           [:div.post-label "$"]]
          [:div.input-stripe {:on-click (partial focus-on-element "invoice-amount-input")}
           [:div.label "Invoice Amount"]
           [c-token-amount-input
            {:id "invoice-amount-input"
             :value (:human-amount @*invoice-amount)
             :decimals job-token-decimals
             :disabled no-job-selected?
             :on-change #(re/dispatch [:page.new-invoice/set-invoice-amount %])}]
           [:div.post-label token-display-name]]
          [:div.usd-estimate @estimated-usd]
          (when show-balance-left?
            [:div.max-available "Max available:" [c-token-info balance-left job-token-details]])]
         [:div.right-form
          [:div.label "Message"]
          [c-textarea-input
           {:value @*message
            :disabled no-job-selected?
            :on-change #(re/dispatch [:page.new-invoice/set-message %])
            :placeholder ""}]]

         [c-button
          {:on-click #(re/dispatch [:page.new-invoice/send])
           :disabled? (or button-disabled? tx-in-progress?)}
          [c-button-label "Send"]]]))))
