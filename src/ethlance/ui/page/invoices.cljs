(ns ethlance.ui.page.invoices
  (:require
   [taoensso.timbre :as log]
   [district.ui.component.page :refer [page]]
   [district.parsers :refer [parse-int]]
   [reagent.core :as r]
   [re-frame.core :as re]
   [district.ui.graphql.subs :as gql]
   [district.ui.router.subs :as router.subs]

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


(defmethod page :route.invoice/index []
  (let [*active-page-params (re/subscribe [::router.subs/active-page-params])]
    (fn []
      (let [invoice-id (-> @*active-page-params :id parse-int)
            invoice-query
            @(re/subscribe
              [::gql/query
               {:queries
                [[:invoice
                  {:invoice/id invoice-id}
                  [:invoice/id]]]}])
            {invoice           :invoice
             preprocessing?    :graphql/preprocessing?
             loading?          :graphql/loading?
             errors            :graphql/errors} invoice-query]
        [c-main-layout {:container-opts {:class :invoice-detail-main-container}}
         [:div.title "Invoice"]
         [:a.sub-title
          {:on-click (fn [])}
          "Finality Labs Full-Stack dApp Dev"]
         [:div.invoice-status [:span.label "Pending"]]

         [:div.left
          [:div.profile.employer
           [:div.label "Employer"]
           [c-profile-image {}]
           [:div.name "Brian Curran"]
           [:div.rating
            [c-rating {:default-rating 3}]
            [:span.num-feedback (str "(" 5 ")")]]
           [:div.location "United States, New York"]]

          [:div.profile.candidate
           [:div.label "Candidate"]
           [c-profile-image {}]
           [:div.name "Brian Curran"]
           [:div.rating
            [c-rating {:default-rating 3}]
            [:span.num-feedback (str "(" 5 ")")]]
           [:div.location "United States, New York"]]

          [:div.profile.arbiter
           [:div.label "Arbiter"]
           [c-profile-image {}]
           [:div.name "Brian Curran"]
           [:div.rating
            [c-rating {:default-rating 3}]
            [:span.num-feedback (str "(" 5 ")")]]
           [:div.location "United States, New York"]]]

         [:div.right
          [:div.ethlance-table
           [:table
            [:tbody
             [:tr
              [:th "Hours Worked"]
              [:td "12"]]

             [:tr
              [:th "Invoiced Amount"]
              [:td "12000SNT"]]

             [:tr
              [:th "Hourly Rate"]
              [:td "100SNT / Hour"]]

             [:tr
              [:th "Worked From"]
              [:td "Monday, February 21, 2018"]]

             [:tr
              [:th "Worked To"]
              [:td "Tuesday, February 22, 2018"]]

             [:tr
              [:th "Invoiced On"]
              [:td "Friday, February 25, 2018"]]]]]]
         [:div.button
          [:span "Pay Invoice"]
          [c-icon {:name :ic-arrow-right :size :small :color :white}]]]))))
         
