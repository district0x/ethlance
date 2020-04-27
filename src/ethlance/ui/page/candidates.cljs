(ns ethlance.ui.page.candidates
  "General Candidate Listings on ethlance"
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


(defn c-candidate-search-filter []
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

   [:span.selection-label "Payment Type"]
   [c-radio-select 
    {:on-selection (fn [selection] (log/debug (str "Payment Selection: " selection)))
     :default-selection :fixed}
    [:fixed [c-radio-search-filter-element "Fixed Price"]]
    [:percentage [c-radio-search-filter-element "Percentage of Dispute"]]]

   [:div.country-selector
    [c-select-input
     {:label "Select Country"
      :selections constants/countries
      :search-bar? true
      :color :secondary
      :default-search-text "Search Countries"}]]])


(defn c-candidate-mobile-search-filter
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

   [:span.selection-label "Payment Type"]
   [c-radio-select 
    {:on-selection (fn [selection] (log/debug (str "Payment Selection: " selection)))
     :default-selection :fixed}
    [:fixed [c-radio-search-filter-element "Fixed Price"]]
    [:percentage [c-radio-search-filter-element "Percentage of Dispute"]]]

   [:div.country-selector
    [c-select-input
     {:label "Select Country"
      :selections constants/countries
      :search-bar? true
      :color :secondary
      :default-search-text "Search Countries"}]]])


(defn c-candidate-element
  [candidate]
  [:div.candidate-element
   [:div.profile
    [:div.profile-image [c-profile-image {}]]
    [:div.name "Brian Curran"]
    [:div.title "Content Creator, Web Developer, Blockchain Analyst"]]
   [:div.price "$15"]
   [:div.tags
    (doall
     (for [tag-label #{"System Administration" "Game Design" "C++" "HopScotch Master"}]
       ^{:key (str "tag-" tag-label)}
       [c-tag {:on-click #(swap! *search-input-listing conj tag-label)
               :title (str "Add '" tag-label "' to Search")}
        [c-tag-label tag-label]]))]
   [:div.rating
    [c-rating {:default-rating 3}]
    [:div.label "(4)"]]
   [:div.location "New York, United States"]])


(defn c-candidate-listing []
  [:<>
   (doall
    (for [candidate (range 10)]
      ^{:key (str "candidate-" candidate)}
      [c-candidate-element candidate]))])


(defmethod page :route.user/candidates []
  (let []
    (fn []
      [c-main-layout {:container-opts {:class :candidates-main-container}}
       [c-candidate-search-filter]
       [c-candidate-mobile-search-filter]
       [:div.candidate-listing.listing {:key "listing"}
        [:div.search-container
         [c-chip-search-input
          {:*chip-listing *search-input-listing
           :placeholder "Search Candidate Skills"
           :allow-custom-chips? false
           :auto-suggestion-listing constants/skills}]]
        [c-candidate-listing]]])))
