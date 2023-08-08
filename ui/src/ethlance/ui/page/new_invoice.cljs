(ns ethlance.ui.page.new-invoice
  (:require [district.ui.component.page :refer [page]]
            [district.ui.graphql.subs :as gql]
            [ethlance.ui.component.icon :refer [c-icon]]
            [ethlance.ui.component.main-layout :refer [c-main-layout]]
            [ethlance.ui.component.select-input :refer [c-select-input]]
            [ethlance.ui.component.textarea-input :refer [c-textarea-input]]
            [re-frame.core :as re]))

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
                      [:token-details
                       [:token-detail/id
                        :token-detail/name
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
            token-symbol (-> @job-token :symbol (or ,,, "") name)]
        [c-main-layout {:container-opts {:class :new-invoice-main-container}}
         [:div.title "New Invoice"]
         [:div.left-form
          [:div.input-stripe
           [:div.label "Job"]
           [c-select-input
            {:selections *job-listing
             :value-fn (fn [job-story] (str (:job/id job-story) "-" (:job-story/id job-story)))
             :label-fn (comp :job/title :job)
             :selection @*invoiced-job
             :on-select #(re/dispatch [:page.new-invoice/set-invoiced-job %])}]]
          [:div.input-stripe
           [:div.label "Hours Worked (Optional)"]
           [:input
            {:type "number"
             :min 0
             :value @*hours-worked
             :on-change #(re/dispatch [:page.new-invoice/set-hours-worked (-> % .-target .-value)])}]]
          [:div.input-stripe
           [:div.label "Hourly Rate"]
           [:input
            {:type "number"
             :min 0
             :value @*hourly-rate
             :on-change #(re/dispatch [:page.new-invoice/set-hourly-rate (-> % .-target .-value)])}]
           [:div.post-label "$"]]
          [:div.input-stripe
           [:div.label "Invoice Amount"]
           [:input
            {:type "number"
             :min 0
             :step 0.01
             :value @*invoice-amount
             :on-change #(re/dispatch [:page.new-invoice/set-invoice-amount (-> % .-target .-value)])}]
           [:div.post-label token-symbol]]
          [:div.usd-estimate @estimated-usd]]

         [:div.right-form
          [:div.label "Message"]
          [c-textarea-input
           {:value @*message
            :on-change #(re/dispatch [:page.new-invoice/set-message %])
            :placeholder ""}]]

         [:div.button {:on-click #(re/dispatch [:page.new-invoice/send])}
          [:div.label "Send"]
          [c-icon {:name :ic-arrow-right :size :small}]]]))))
