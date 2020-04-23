(ns ethlance.ui.page.jobs
  "General Job Listings on ethlance"
  (:require
   [cuerdas.core :as str]
   [reagent.core :as r]
   [re-frame.core :as re]
   [taoensso.timbre :as log]
   [district.ui.component.page :refer [page]]

   [ethlance.shared.enumeration.currency-type :as enum.currency]
   [ethlance.shared.constants :as constants]

   ;; Ethlance Components
   [ethlance.ui.component.currency-input :refer [c-currency-input]]
   [ethlance.ui.component.inline-svg :refer [c-inline-svg]]
   [ethlance.ui.component.loading-spinner :refer [c-loading-spinner]]
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
  [{:keys [] :as employer}]
  [:div.user-detail.employer
   [:div.name "Brian Curran"]
   [:div.rating-container
    [c-rating {:size :small :color :primary :default-rating 3}]
    [:div.rating-label "(6)"]]
   [:div.location "United States, New York"]])


(defn c-user-arbiter-detail
  [{:keys [] :as arbiter}]
  [:div.user-detail.arbiter
   [c-inline-svg {:class "arbiter-icon" :src "images/svg/hammer.svg"}]
   [:div.name "Brian Curran"]
   [:div.rating-container
    [c-rating {:size :small :color :primary :default-rating 3}]
    [:div.rating-label "(6)"]]
   [:div.location "United States, New York"]])


(defn c-job-detail-table
  [{:keys [payment-type experience-level project-length availability] :as job}]
  (let [formatted-experience-level (case experience-level
                                     :novice "Novice ($)"
                                     :professional "Professional ($$)"
                                     :expert "Expert ($$$)")]
    [:div.job-detail-table
     [:div.name "Payment Type"]
     [:div.value (str/title payment-type)]

     [:div.name "Experience Level"]
     [:div.value formatted-experience-level]

     [:div.name "Project Length"]
     [:div.value (str/title project-length)]

     [:div.name "Availability"]
     [:div.value (str/title availability)]]))


(defn c-job-search-filter
  "Sidebar component for changing the search criteria."
  []
  (let [*feedback-max-rating (re/subscribe [:page.jobs/feedback-max-rating])
        *feedback-min-rating (re/subscribe [:page.jobs/feedback-min-rating])]
    (fn []
      (let []
        [:div.job-search-filter.search-filter
         [:div.category-selector
          [c-select-input
           {:label "All Categories"
            :default-selection "All Categories"
            :color :secondary
            :selections ["All Categories" "Software Development" "Web Design"]}]]

         [:span.rating-label "Min. Rating"]
         [c-rating {:rating @*feedback-min-rating :color :white :size :small
                    :on-change #(re/dispatch [:page.jobs/set-feedback-min-rating %])}]

         [:span.rating-label "Max. Rating"]
         [c-rating {:rating @*feedback-max-rating :color :white :size :small
                    :on-change #(re/dispatch [:page.jobs/set-feedback-max-rating %])}]

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
           :default-search-text "Search Countries"}]]))))

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
   [c-rating {:default-rating 1 :color :white :size :small
              :on-change (fn [index] (log/debug "Min. Rating: " index))}]

   [:span.rating-label "Max. Rating"]
   [c-rating {:default-rating 5 :color :white :size :small
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


(defn c-job-element
  "A single job element component composed from the job data."
  [{:keys [title description date-created skills arbiter employer] :as job}]
  [:div.job-element
   [:div.title title]
   [:div.description description]
   [:div.date (or date-created "Posted 1 day ago | 5 Proposals")]
   [:div.tags
    (doall
     (for [skill-label skills]
       ^{:key (str "tag-" skill-label)}
       [c-tag {:on-click #(swap! *search-input-listing conj skill-label)
               :title (str "Add '" skill-label "' to Search")}
        [c-tag-label skill-label]]))]

   [:div.users
    [c-user-employer-detail employer]
    [c-user-arbiter-detail arbiter]]

   [:div.details
    [c-job-detail-table job]]])


(defn c-job-listing []
  (let [*job-listing (re/subscribe [:page.jobs/job-listing])
        *job-listing-state (re/subscribe [:page.jobs/job-listing-state])]
    (fn []
      (let [job-listing @*job-listing
            job-listing-state @*job-listing-state
            loading? (contains? #{:start :loading} job-listing-state)]
                         
        [:<>
         (cond
           ;; Is the job listing loading?
           loading?
           [c-loading-spinner]
           
           ;; Is the job listing empty?
           (empty? job-listing)
           [:div.empty-listing "No Jobs"]

           :else
           (doall
            (for [job job-listing]
              ^{:key (str "job-" (:index job))}
              [c-job-element job])))]))))


(defmethod page :route.job/jobs []
  (let [*job-listing (re/subscribe [:page.jobs/job-listing])]
    (fn []
      [c-main-layout {:container-opts {:class :jobs-main-container}}
       [c-job-search-filter]
       [c-job-mobile-search-filter]
       [:div.job-listing.listing {:key "listing"}
        [:div.search-container
         [c-chip-search-input 
          {:*chip-listing *search-input-listing
           :placeholder "Search Job Skill Requirements"
           :allow-custom-chips? false
           :auto-suggestion-listing constants/skills}]]
        [c-job-listing]]])))

