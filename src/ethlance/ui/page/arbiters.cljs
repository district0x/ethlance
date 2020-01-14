(ns ethlance.ui.page.arbiters
  "General Arbiter Listings on ethlance"
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

   [c-currency-input {:placeholder "Min. Hourly Rate" :color :secondary}]
   [c-currency-input {:placeholder "Max. Hourly Rate" :color :secondary}]
   [:div.feedback-input
    [c-text-input
     {:placeholder "Number of Feedbacks"
      :color :secondary}]]

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

   [c-currency-input {:placeholder "Min. Hourly Rate" :color :secondary}]
   [c-currency-input {:placeholder "Max. Hourly Rate" :color :secondary}]
   [:div.feedback-input
    [c-text-input
     {:placeholder "Number of Feedbacks"
      :color :secondary}]]

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
        [:div.search-container
         [c-chip-search-input
          {:*chip-listing *search-input-listing
           :auto-suggestion-listing constants/skills
           :allow-custom-chips? false
           :placeholder "Search Arbiter Skills"}]]
        [c-arbiter-listing]]])))
