(ns ethlance.ui.page.employers
  "General Employer Listings on ethlance"
  (:require
   [reagent.core :as r]
   [taoensso.timbre :as log]
   [district.ui.component.page :refer [page]]

   [ethlance.shared.enumeration.currency-type :as enum.currency]
   [ethlance.shared.constants :as constants]

   ;; Ethlance Components
   [ethlance.ui.component.main-layout :refer [c-main-layout]]
   [ethlance.ui.component.rating :refer [c-rating]]
   [ethlance.ui.component.tag :refer [c-tag c-tag-label]]
   [ethlance.ui.component.text-input :refer [c-text-input]]
   [ethlance.ui.component.radio-select :refer [c-radio-select c-radio-search-filter-element]]
   [ethlance.ui.component.search-input :refer [c-chip-search-input]]
   [ethlance.ui.component.currency-input :refer [c-currency-input]]
   [ethlance.ui.component.inline-svg :refer [c-inline-svg]]
   [ethlance.ui.component.select-input :refer [c-select-input]]
   [ethlance.ui.component.mobile-search-filter :refer [c-mobile-search-filter]]
   [ethlance.ui.component.profile-image :refer [c-profile-image]]))


;;
;; Page State
;;
(def *search-input-listing (r/atom #{}))


(defn c-employer-search-filter []
  [:div.search-filter
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

   [:div.feedback-input
    [c-text-input {:placeholder "Number of Feedbacks" :color :secondary}]]

   [:div.country-selector
    [c-select-input
     {:label "Select Country"
      :selections constants/countries
      :search-bar? true
      :color :secondary
      :default-search-text "Search Countries"}]]])


(defn c-employer-mobile-search-filter
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

   [:div.feedback-input
    [c-text-input {:placeholder "Number of Feedbacks" :color :secondary}]]

   [:div.country-selector
    [c-select-input
     {:label "Select Country"
      :selections constants/countries
      :search-bar? true
      :color :secondary
      :default-search-text "Search Countries"}]]])


(defn c-employer-element
  [employer]
  [:div.employer-element
   [:div.profile
    [:div.profile-image [c-profile-image {}]]
    [:div.name "Brian Curran"]
    [:div.title "Content Creator, Web Developer, Blockchain Analyst"]]
   #_[:div.price "$15 / Fixed Price"]
   [:div.tags
    (doall
     (for [tag-label #{"System Administration" "Game Design" "C++" "HopScotch Master"}]
       ^{:key (str "tag-" tag-label)}
       [c-tag {:on-click #(swap! *search-input-listing conj tag-label)
               :title (str "Add '" tag-label "' to Search")}
        [c-tag-label tag-label]]))]
   [:div.rating
    [c-rating {:rating 3}]
    [:div.label "(4)"]]
   [:div.location "New York, United States"]])


(defn c-employer-listing []
  [:<>
   (doall
    (for [employer (range 10)]
      ^{:key (str "employer-" employer)}
      [c-employer-element employer]))])


(defmethod page :route.user/employers []
  (let []
    (fn []
      [c-main-layout {:container-opts {:class :employers-main-container}}
       [c-employer-search-filter]
       [c-employer-mobile-search-filter]
       [:div.employer-listing.listing {:key "listing"}
        [:div.search-container
         [c-chip-search-input
          {:*chip-listing *search-input-listing
           :auto-suggestion-listing constants/skills
           :allow-custom-chips? false
           :placeholder "Search Employer Skills"}]]
        [c-employer-listing]]])))
