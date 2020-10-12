(ns ethlance.ui.page.new-invoice
  (:require [district.ui.component.page :refer [page]]
            [ethlance.ui.component.icon :refer [c-icon]]
            [ethlance.ui.component.main-layout :refer [c-main-layout]]
            [ethlance.ui.component.select-input :refer [c-select-input]]
            [ethlance.ui.component.textarea-input :refer [c-textarea-input]]
            [re-frame.core :as re]))

(defmethod page :route.invoice/new []
  (let [*job-name-listing (re/subscribe [:page.new-invoice/job-name-listing])
        *job-name (re/subscribe [:page.new-invoice/job-name])
        *hours-worked (re/subscribe [:page.new-invoice/hours-worked])
        *hourly-rate (re/subscribe [:page.new-invoice/hourly-rate])
        *invoice-amount (re/subscribe [:page.new-invoice/invoice-amount])
        *message (re/subscribe [:page.new-invoice/message])]
    (fn []
      [c-main-layout {:container-opts {:class :new-invoice-main-container}}
       [:div.title "New Invoice"]
       [:div.left-form
        [:div.input-stripe
         [:div.label "Job"]
         [c-select-input
          {:selections @*job-name-listing
           :selection @*job-name
           :on-select #(re/dispatch [:page.new-invoice/set-job-name %])}]]
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
           :value @*invoice-amount
           :on-change #(re/dispatch [:page.new-invoice/set-invoice-amount (-> % .-target .-value)])}]
         [:div.post-label "ETH"]]
        [:div.usd-estimate
         "$645.23 (1 ETH = 243.34 USD)"]]

       [:div.right-form
        [:div.label "Message"]
        [c-textarea-input
         {:value @*message
          :on-change #(re/dispatch [:page.new-invoice/set-message %])
          :placeholder ""}]]

       [:div.button
        [:div.label "Send"]
        [c-icon {:name :ic-arrow-right :size :small}]]])))
