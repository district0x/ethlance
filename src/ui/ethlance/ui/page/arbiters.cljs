(ns ethlance.ui.page.arbiters
  "General Arbiter Listings on ethlance"
  (:require
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


(defn c-arbiter-search-filter []
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

   [c-currency-input {:placeholder "Min. Hourly Rate"}]
   [c-currency-input {:placeholder "Max. Hourly Rate"}]
   [c-text-input {:placeholder "Number of Feedbacks"}]

   [:div.country-selector
    [c-select-input
     {:label "Country"
      :selections constants/countries
      :search-bar? true
      :color :secondary
      :default-search-text "Search Countries"}]]])


(defn c-arbiter-mobile-search-filter
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

   [c-currency-input {:placeholder "Min. Hourly Rate"}]
   [c-currency-input {:placeholder "Max. Hourly Rate"}]
   [c-text-input {:placeholder "Number of Feedbacks"}]

   [:div.country-selector
    [c-select-input
     {:label "Country"
      :selections constants/countries
      :search-bar? true
      :color :secondary
      :default-search-text "Search Countries"}]]])


(defn c-arbiter-element
  [arbiter]
  [:div.arbiter-element
   [:div.profile
    [c-profile-image {}]
    [:div.name "Brian Curran"]
    [:div.title "Content Creator, Web Developer, Blockchain Analyst"]]
   [:div.price "$15"]
   [:div.tags
    [c-tag {} [c-tag-label "System Administration"]]
    [c-tag {} [c-tag-label "Game Design"]]
    [c-tag {} [c-tag-label "C++ Programming"]]
    [c-tag {} [c-tag-label "HopScotch Master"]]]
   [:div.rating
    [c-rating {:rating 3}]
    [:div.label "(4)"]]
   [:div.location "New York, United States"]])


(defn c-arbiter-listing []
  [:<>
   (doall
    (for [arbiter (range 10)]
      ^{:key (str "arbiter-" arbiter)}
      [c-arbiter-element arbiter]))])


(defmethod page :route.user/arbiters []
  (let []
    (fn []
      [c-main-layout {:container-opts {:class :arbiters-main-container}}
       [c-arbiter-search-filter]
       [c-arbiter-mobile-search-filter]
       [:div.arbiter-listing.listing {:key "listing"}
        [c-chip-search-input
         {:auto-suggestion-listing constants/skills
          :allow-custom-chips? false
          :placeholder "Search Tags"}]
        [c-arbiter-listing]]])))
