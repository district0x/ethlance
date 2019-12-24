(ns ethlance.ui.page.jobs
  "General Job Listings on ethlance"
  (:require
   [reagent.core :as r]
   [taoensso.timbre :as log]
   [district.ui.component.page :refer [page]]

   [ethlance.shared.enumeration.currency-type :as enum.currency]
   [ethlance.shared.constants :as constants]

   ;; Ethlance Components
   [ethlance.ui.component.currency-input :refer [c-currency-input]]
   [ethlance.ui.component.inline-svg :refer [c-inline-svg]]
   [ethlance.ui.component.main-layout :refer [c-main-layout]]
   [ethlance.ui.component.mobile-search-filter :refer [c-mobile-search-filter]]
   [ethlance.ui.component.radio-select :refer [c-radio-select c-radio-search-filter-element]]
   [ethlance.ui.component.rating :refer [c-rating]]
   [ethlance.ui.component.search-input :refer [c-chip-search-input]]
   [ethlance.ui.component.select-input :refer [c-select-input]]
   [ethlance.ui.component.tag :refer [c-tag c-tag-label]]
   [ethlance.ui.component.text-input :refer [c-text-input]]))


;;
;; Page State
;;
(def *search-input-listing (r/atom #{}))


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
   [c-inline-svg {:class "arbiter-icon" :src "images/svg/hammer.svg"}]
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
  [:div.job-search-filter.search-filter
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
     :color :secondary
     :on-change #(println "Currency Min Change: " %)}]
   
   [c-currency-input
    {:placeholder "Max. Hourly Rate"
     :currency-type ::enum.currency/usd
     :color :secondary
     :on-change #(println "Currency Max Change: " %)}]

   [:div.feedback-input
    [c-text-input
     {:placeholder "Number of Feedbacks"
      :color :secondary}]]

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
    [:expert [c-radio-search-filter-element "Expert ($$$)"]]]

   [c-select-input
    {:label "Country"
     :selections constants/countries
     :search-bar? true
     :color :secondary
     :default-search-text "Search Countries"}]])

(defn c-job-mobile-search-filter
  []
  [c-mobile-search-filter
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

   [:div.feedback-input
    [c-text-input
     {:placeholder "Number of Feedbacks"
      :color :secondary}]]

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
    [:expert [c-radio-search-filter-element "Expert ($$$)"]]]
   
   [c-select-input
    {:label "Country"
     :selections constants/countries
     :search-bar? true
     :color :secondary
     :default-search-text "Search Countries"}]])


(defn c-job-element
  "A single job element component composed from the job data."
  [job]
  [:div.job-element
   [:div.title "Ethereum Contract Implementation"]
   [:div.description "Lorem ipsum dolor sit amet, consectetur
   adipiscing elit. Morbi ac ex non ipsum laoreet fringilla quis vel
   nibh. Praesent sed condimentum ex, consectetur gravida felis. Sed
   tincidunt vestibulum ante elementum pellentesque."]
   [:div.date "Posted 1 day ago | 5 Proposals"]
   [:div.tags
    (doall
     (for [tag-label #{"System Administration" "Game Design" "C++" "HopScotch Master"}]
       ^{:key (str "tag-" tag-label)}
       [c-tag {:on-click #(swap! *search-input-listing conj tag-label)
               :title (str "Add '" tag-label "' to Search")}
        [c-tag-label tag-label]]))]

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
       [c-job-mobile-search-filter]
       [:div.job-listing.listing {:key "listing"}
        [c-chip-search-input 
         {:*chip-listing *search-input-listing
          :placeholder "Search Job Skill Requirements"
          :allow-custom-chips? false
          :auto-suggestion-listing constants/skills}]
        [c-job-listing]]])))

