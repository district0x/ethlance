(ns ethlance.ui.page.new-invoice
  (:require
   [taoensso.timbre :as log]
   [district.ui.component.page :refer [page]]
   [reagent.core :as r]
   [re-frame.core :as re]

   ;; Ethlance Components
   [ethlance.ui.component.button :refer [c-button c-button-icon-label c-button-label]]
   [ethlance.ui.component.carousel :refer [c-carousel c-feedback-slide]]
   [ethlance.ui.component.circle-button :refer [c-circle-icon-button]]
   [ethlance.ui.component.currency-input :refer [c-currency-input]]
   [ethlance.ui.component.icon :refer [c-icon]]
   [ethlance.ui.component.inline-svg :refer [c-inline-svg]]
   [ethlance.ui.component.main-layout :refer [c-main-layout]]
   [ethlance.ui.component.profile-image :refer [c-profile-image]]
   [ethlance.ui.component.radio-select :refer [c-radio-select c-radio-search-filter-element c-radio-secondary-element]]
   [ethlance.ui.component.rating :refer [c-rating]]
   [ethlance.ui.component.search-input :refer [c-chip-search-input]]
   [ethlance.ui.component.select-input :refer [c-select-input]]
   [ethlance.ui.component.table :refer [c-table]]
   [ethlance.ui.component.tabular-layout :refer [c-tabular-layout]]
   [ethlance.ui.component.tag :refer [c-tag c-tag-label]]
   [ethlance.ui.component.text-input :refer [c-text-input]]
   [ethlance.ui.component.textarea-input :refer [c-textarea-input]]))


(defmethod page :route.invoice/new []
  (let [*job-name-listing (re/subscribe [:page.new-invoice/job-name-listing])
        *job-name (re/subscribe [:page.new-invoice/job-name])
        *hours-worked (re/subscribe [:page.new-invoice/hours-worked])
        *hourly-rate (re/subscribe [:page.new-invoice/hourly-rate])
        *invoice-amount (re/subscribe [:page.new-invoice/invoice-amount])
        *message (re/subscribe [:page.new-invoice/message])]
    (fn []
      (let []
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
          [c-icon {:name :ic-arrow-right :size :small}]]]))))
