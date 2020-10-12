(ns ethlance.ui.page.invoices
  (:require [district.ui.component.page :refer [page]]
            [ethlance.ui.component.icon :refer [c-icon]]
            [ethlance.ui.component.main-layout :refer [c-main-layout]]
            [ethlance.ui.component.profile-image :refer [c-profile-image]]
            [ethlance.ui.component.rating :refer [c-rating]]))

(defmethod page :route.invoice/index []
  (fn []
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
      [c-icon {:name :ic-arrow-right :size :small :color :white}]]]))
