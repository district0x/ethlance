(ns ethlance.ui.page.jobs
  "General Job Listings on ethlance"
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
   [ethlance.ui.component.select-input :refer [c-select-input]]))


(defn c-user-employer-detail
  [{:keys [] :as user}]
  [:div.user-detail.employer
   [:div.name "Brian Curran"]
   [c-rating {:size :small :color :primary :rating 3}]
   [:div.rating-label "(6)"]
   [:div.location "United States, New York"]])

   
(defn c-user-arbiter-detail
  [{:keys [] :as user}]
  [:div.user-detail.arbiter
   [c-inline-svg {:class "arbiter-icon"}]
   [:div.name "Brian Curran"]
   [c-rating {:size :small :color :primary :rating 3}]
   [:div.rating-label "(6)"]
   [:div.location "United States, New York"]])


(defn c-job-detail-table
  [{:keys [] :as job}]
  [:div.job-detail-table
   [:div.name "Payment Type"]
   [:div.value "Hourly Rate"]

   [:div.name "Experience Level"]
   [:div.value "Expert ($$$)"]

   [:div.name "Project Length"]
   [:div.value "Months"]
   
   [:div.name "Availability"]
   [:div.value "Full Time"]])


(defn c-job-search-filter
  "Sidebar component for changing the search criteria."
  []
  [:div.job-search-filter
   {:key "search-filter"}
   
   [:div.category-selector
    [c-select-input
     {:label "All Categories"
      :default-selection "All Categories"
      :color :secondary
      :selections ["All Categories" "Software Development" "Web Design"]}]]

   [:span.rating-label "Min. Rating"]
   [c-rating {:rating 1 :color :white :size :small
              :on-change (fn [index] (log/debug "Min. Rating: " index))}]

   [:span.rating-label "Max. Rating"]
   [c-rating {:rating 5 :color :white :size :small
              :on-change (fn [index] (log/debug "Max. Rating: " index))}]

   [c-currency-input
    {:placeholder "Min. Hourly Rate"
     :currency-type ::enum.currency/usd
     :on-change #(println "Currency Min Change: " %)}]
   
   [c-currency-input
    {:placeholder "Max. Hourly Rate"
     :currency-type ::enum.currency/usd
     :on-change #(println "Currency Max Change: " %)}]

   [:span.selection-label "Payment Type"]
   [c-radio-select 
    {:on-selection (fn [selection] (log/debug (str "Payment Selection: " selection)))
     :default-selection :hourly-rate}
    [:hourly-rate [c-radio-search-filter-element "Hourly Rate"]]
    [:fixed-price [c-radio-search-filter-element "Fixed Price"]]
    [:annual-salary [c-radio-search-filter-element "Annual Salary"]]]

   [:span.selection-label "Experience Level"]
   [c-radio-select 
    {:on-selection (fn [selection] (log/debug (str "Experience Selection: " selection)))
     :default-selection :novice}
    [:novice [c-radio-search-filter-element "Novice ($)"]]
    [:professional [c-radio-search-filter-element "Professional ($$)"]]
    [:expert [c-radio-search-filter-element "Expert ($$$)"]]]])


(defn c-job-element
  "A single job element component composed from the job data."
  [job]
  [:div.job-element
   [:div.title "Ethereum Contract Implementation"]
   [:div.description "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi ac ex non ipsum laoreet fringilla quis vel nibh. Praesent sed condimentum ex, consectetur gravida felis. Sed tincidunt vestibulum ante elementum pellentesque."]
   [:div.date "Posted 1 day ago | 5 Proposals"]
   [:div.tags
    [c-tag {} [c-tag-label "System Administration"]]
    [c-tag {} [c-tag-label "Game Design"]]
    [c-tag {} [c-tag-label "C++ Programming"]]
    [c-tag {} [c-tag-label "HopScotch Master"]]]

   [:div.users
    [c-user-employer-detail {}]
    [c-user-arbiter-detail {}]]

   [:div.details
    [c-job-detail-table job]]])


(defn c-job-listing []
  [:<>
   (doall
    (for [job (range 10)]
      ^{:key (str "job-" job)}
      [c-job-element job]))])


(defmethod page :route.job/jobs []
  (let []
    (fn []
      [c-main-layout {:container-opts {:class :jobs-main-container}}
       [c-job-search-filter]
       [:div.job-listing {:key "listing"}
        [c-chip-search-input {:default-chip-listing #{"C++" "Python"}}]
        [c-job-listing]]])))

