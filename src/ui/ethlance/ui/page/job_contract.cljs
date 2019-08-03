(ns ethlance.ui.page.job-contract
  "For viewing individual job contracts"
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
   [ethlance.ui.component.select-input :refer [c-select-input]]
   [ethlance.ui.component.mobile-search-filter :refer [c-mobile-search-filter]]))


(defn c-job-detail-table
  [{:keys [] :as job}]
  [:div.job-detail-table

   [:div.name "Status"]
   [:div.value "Active"]

   [:div.name "Funds Available"]
   [:div.value "12,900 SNT"]

   [:div.name "Employer"]
   [:div.value "Cyrus Karsan"]

   [:div.name "Candidate"]
   [:div.value "Clement Lesaege"]
   
   [:div.name "Arbiter"]
   [:div.value "Keegan Quigley"]])


(defn c-header-profile
  [{:keys [] :as job}]
  [:div.header-profile
   [:div.title "Job Contract"]
   [:div.job-name "Finality Labs Full Stack Developer"]
   [:div.job-details
    [c-job-detail-table {}]]])
    

(defmethod page :route.job/contract []
  (let []
    (fn []
      [c-main-layout {:container-opts {:class :job-contract-main-container}}
       [c-header-profile {}]])))
