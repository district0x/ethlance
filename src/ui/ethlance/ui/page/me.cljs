(ns ethlance.ui.page.me
  (:require
   [taoensso.timbre :as log]
   [district.ui.component.page :refer [page]]

   [ethlance.shared.enumeration.currency-type :as enum.currency]

   ;; Ethlance Components
   [ethlance.ui.component.circle-button :refer [c-circle-icon-button]]
   [ethlance.ui.component.currency-input :refer [c-currency-input]]
   [ethlance.ui.component.inline-svg :refer [c-inline-svg]]
   [ethlance.ui.component.main-layout :refer [c-main-layout]]
   [ethlance.ui.component.radio-select :refer [c-radio-select c-radio-search-filter-element]]
   [ethlance.ui.component.rating :refer [c-rating]]
   [ethlance.ui.component.search-input :refer [c-chip-search-input]]
   [ethlance.ui.component.table :refer [c-table]]
   [ethlance.ui.component.tabular-layout :refer [c-tabular-layout]]
   [ethlance.ui.component.tag :refer [c-tag c-tag-label]]))


(defn c-me-sidebar []
  [:div.sidebar
   [:span.label "Employer"]
   [:a {:href "#/me/jobs"} "My Jobs"]
   [:a {:href "#/me/candidates"} "My Contracts"]
   [:a {:href "#"} "My Invoices"]
   [:hr]
   [:span.label "Candidate"]
   [:a {:href "#"} "My Jobs"]
   [:a {:href "#"} "My Contracts"]
   [:a {:href "#"} "My Invoices"]
   [:hr]
   [:span.label "Arbiter"]
   [:a {:href "#"} "My Jobs"]
   [:a {:href "#"} "My Contracts"]
   [:a {:href "#"} "My Invoices"]])


(defn c-invitations-tab []
  [:div.invitations-tab
   [c-table
    {:headers ["Title" "Candidate" "Rate" "Total Spent"]}
    [[:span "Cryptoeconomics Research Intern"]
     [:span "Keegan Quigley"]
     [:span "$30/hr"]
     [:span "12.2 ETH"]]

    [[:span "Smart Contract Hacker"]
     [:span "Cyrus Karsen"]
     [:span "$25"]
     [:span "1000 SNT"]]

    [[:span "Interactive Developer"]
     [:span "Ari Kaplan"]
     [:span "$75"]
     [:span "5.4 ETH"]]

    [[:span "Cryptoeconomics Research Intern"]
     [:span "Keegan Quigley"]
     [:span "$30/hr"]
     [:span "12.2 ETH"]]]

   [:div.button-listing
    [c-circle-icon-button {:name :ic-arrow-left2}]
    [c-circle-icon-button {:name :ic-arrow-left}]
    [c-circle-icon-button {:name :ic-arrow-right}]
    [c-circle-icon-button {:name :ic-arrow-right2}]]])


(defmethod page :route.me/index []
  (let []
    (fn []
      [c-main-layout {:container-opts {:class :me-main-container}}
       [c-me-sidebar]
       [c-tabular-layout
        {:key "me-tabular-layout"
         :default-tab 0}
        
        {:label "Invitations"}
        [c-invitations-tab]

        {:label "Pending Proposals"}
        [:div.pending-proposals-tab
         "Pending Proposals"]

        {:label "Active Contracts"}
        [:div.active-contracts-tab
         "Active Contracts Tab"]

        {:label "Finished Contracts"}
        [:div.finished-contracts-tab
         "Finished Contrafts Tab"]

        {:label "Cancelled Contracts"}
        [:div.cancelled-contracts-tab
         "Cancelled Contracts"]]])))
