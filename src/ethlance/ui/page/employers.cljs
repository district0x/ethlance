(ns ethlance.ui.page.employers
  "General Employer Listings on ethlance"
  (:require
   [reagent.core :as r]
   [re-frame.core :as re]
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


(defn cf-employer-search-filter []
  (let [*category (re/subscribe [:page.employers/category])
        *feedback-max-rating (re/subscribe [:page.employers/feedback-max-rating])
        *feedback-min-rating (re/subscribe [:page.employers/feedback-min-rating])
        *min-num-feedbacks (re/subscribe [:page.employers/min-num-feedbacks])
        *country (re/subscribe [:page.employers/country])]
    (fn []
      [:<>
       [:div.category-selector
        [c-select-input
         {:selection @*category
          :color :secondary
          :selections constants/categories-with-default
          :on-select #(re/dispatch [:page.employers/set-category %])}]]

       [:span.rating-label "Min. Rating"]
       [c-rating {:rating @*feedback-min-rating :color :white :size :small
                  :on-change #(re/dispatch [:page.employers/set-feedback-min-rating %])}]

       [:span.rating-label "Max. Rating"]
       [c-rating {:rating @*feedback-max-rating :color :white :size :small
                  :on-change #(re/dispatch [:page.employers/set-feedback-max-rating %])}]

       [:div.feedback-input
        [c-text-input
         {:placeholder "Number of Feedbacks"
          :color :secondary
          :type :number :min 0
          :value @*min-num-feedbacks
          :on-change #(re/dispatch [:page.employers/set-min-num-feedbacks %])}]]

       [:div.country-selector
        [c-select-input
         {:label "Country"
          :selection @*country
          :on-select #(re/dispatch [:page.employers/set-country %])
          :selections constants/countries
          :search-bar? true
          :color :secondary
          :default-search-text "Search Countries"}]]])))


(defn c-employer-search-filter []
  [:div.search-filter
   [cf-employer-search-filter]])


(defn c-employer-mobile-search-filter
  []
  [c-mobile-search-filter
   [cf-employer-search-filter]])


(defn c-employer-element
  [employer]
  [:div.employer-element
   [:div.profile
    [:div.profile-image [c-profile-image {}]]
    [:div.name "Brian Curran"]
    [:div.title "Content Creator, Web Developer, Blockchain Analyst"]]
   [:div.tags
    (doall
     (for [tag-label #{"System Administration" "Game Design" "C++" "HopScotch Master"}]
       ^{:key (str "tag-" tag-label)}
       [c-tag {:on-click #(re/dispatch [:page.employers/add-skill tag-label])
               :title (str "Add '" tag-label "' to Search")}
        [c-tag-label tag-label]]))]
   [:div.rating
    [c-rating {:default-rating 3}]
    [:div.label "(4)"]]
   [:div.location "New York, United States"]])


(defn c-employer-listing []
  [:<>
   (doall
    (for [employer (range 10)]
      ^{:key (str "employer-" employer)}
      [c-employer-element employer]))])


(defmethod page :route.user/employers []
  (let [*skills (re/subscribe [:page.employers/skills])]
    (fn []
      [c-main-layout {:container-opts {:class :employers-main-container}}
       [c-employer-search-filter]
       [c-employer-mobile-search-filter]
       [:div.employer-listing.listing {:key "listing"}
        [:div.search-container
         [c-chip-search-input 
          {:chip-listing @*skills
           :on-chip-listing-change #(re/dispatch [:page.employers/set-skills %])
           :placeholder "Search Job Skill Requirements"
           :allow-custom-chips? false
           :auto-suggestion-listing constants/skills}]]
        [c-employer-listing]]])))
