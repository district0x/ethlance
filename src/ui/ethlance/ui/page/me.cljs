(ns ethlance.ui.page.me
  (:require
   [taoensso.timbre :as log]
   [district.ui.component.page :refer [page]]

   [ethlance.shared.enumeration.currency-type :as enum.currency]

   ;; Ethlance Components
   [ethlance.ui.component.main-layout :refer [c-main-layout]]
   [ethlance.ui.component.rating :refer [c-rating]]
   [ethlance.ui.component.tag :refer [c-tag c-tag-label]]
   [ethlance.ui.component.radio-select :refer [c-radio-select c-radio-search-filter-element]]
   [ethlance.ui.component.search-input :refer [c-chip-search-input]]
   [ethlance.ui.component.currency-input :refer [c-currency-input]]
   [ethlance.ui.component.inline-svg :refer [c-inline-svg]]
   [ethlance.ui.component.tabular-layout :refer [c-tabular-layout]]))


(defn c-me-sidebar []
  [:div.sidebar
   [:span.label "Employer"]
   [:a "My Jobs"]
   [:a "My Contracts"]
   [:a "My Invoices"]
   [:hr]
   [:span.label "Candidate"]
   [:a "My Jobs"]
   [:a "My Contracts"]
   [:a "My Invoices"]
   [:hr]
   [:span.label "Arbiter"]
   [:a "My Jobs"]
   [:a "My Contracts"]
   [:a "My Invoices"]])


(defmethod page :route.me/index []
  (let []
    (fn []
      [c-main-layout {:container-opts {:class :me-main-container}}
       [c-me-sidebar]
       [c-tabular-layout
        {:default-tab 0}
        
        {:label "Invitations"}
        [:div.invitations-tab
         "Invitations"]

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
