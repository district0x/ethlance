(ns ethlance.ui.page.candidates
  "General Candidate Listings on ethlance"
  (:require
   [reagent.core :as r]
   [re-frame.core :as re]
   [taoensso.timbre :as log]
   [district.ui.component.page :refer [page]]
   [district.ui.graphql.subs :as gql]

   [ethlance.shared.enumeration.currency-type :as enum.currency]
   [ethlance.shared.constants :as constants]

   ;; Ethlance Components
   [ethlance.ui.component.currency-input :refer [c-currency-input]]
   [ethlance.ui.component.inline-svg :refer [c-inline-svg]]
   [ethlance.ui.component.loading-spinner :refer [c-loading-spinner]]
   [ethlance.ui.component.main-layout :refer [c-main-layout]]
   [ethlance.ui.component.mobile-search-filter :refer [c-mobile-search-filter]]
   [ethlance.ui.component.profile-image :refer [c-profile-image]]
   [ethlance.ui.component.radio-select :refer [c-radio-select c-radio-search-filter-element]]
   [ethlance.ui.component.rating :refer [c-rating]]
   [ethlance.ui.component.search-input :refer [c-chip-search-input]]
   [ethlance.ui.component.select-input :refer [c-select-input]]
   [ethlance.ui.component.tag :refer [c-tag c-tag-label]]))


(defn cf-candidate-search-filter 
  "Component Fragment for the candidate search filter."
  []
  (let [*category (re/subscribe [:page.candidates/category])
        *feedback-max-rating (re/subscribe [:page.candidates/feedback-max-rating])
        *feedback-min-rating (re/subscribe [:page.candidates/feedback-min-rating])
        *payment-type (re/subscribe [:page.candidates/payment-type])
        *country (re/subscribe [:page.candidates/country])]
    (fn []
      [:<>
       [:div.category-selector
        [c-select-input
         {:selection @*category
          :color :secondary
          :selections constants/categories-with-default
          :on-select #(re/dispatch [:page.candidates/set-category %])}]]

       [:span.rating-label "Min. Rating"]
       [c-rating {:rating @*feedback-min-rating :color :white :size :small
                  :on-change #(re/dispatch [:page.candidates/set-feedback-min-rating %])}]

       [:span.rating-label "Max. Rating"]
       [c-rating {:rating @*feedback-max-rating :color :white :size :small
                  :on-change #(re/dispatch [:page.candidates/set-feedback-max-rating %])}]

       [:span.selection-label "Payment Type"]
       [c-radio-select 
        {:selection @*payment-type
         :on-selection #(re/dispatch [:page.candidates/set-payment-type %])}
        [:fixed-price [c-radio-search-filter-element "Fixed Price"]]
        [:percentage [c-radio-search-filter-element "Percentage of Dispute"]]]

       [:div.country-selector
        [c-select-input
         {:label "Country"
          :selections constants/countries
          :selection @*country
          :on-select #(re/dispatch [:page.candidates/set-country %])
          :search-bar? true
          :color :secondary
          :default-search-text "Search Countries"}]]])))


(defn c-candidate-search-filter []
  [:div.search-filter
   [cf-candidate-search-filter]])


(defn c-candidate-mobile-search-filter
  []
  [c-mobile-search-filter
   [cf-candidate-search-filter]])


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
       [c-tag {:on-click #(re/dispatch  [:page.candidates/add-skill tag-label])
               :title (str "Add '" tag-label "' to Search")}
        [c-tag-label tag-label]]))]
   [:div.rating
    [c-rating {:default-rating 3}]
    [:div.label "(4)"]]
   [:div.location "New York, United States"]])


(defn c-candidate-listing []
  (let [*candidate-listing-query
        (re/subscribe
         [::gql/query
          {:queries
           [[:candidate-search
             [[:items [:user/address :candidate/skills]]
              :total-count
              :end-cursor
              :has-next-page]]]}])]
    (fn []
      (let [{candidate-search :candidate-search
             preprocessing?   :graphql/preprocessing?
             loading?         :graphql/loading?} @*candidate-listing-query]
        (println @*candidate-listing-query)
        [:<>
         (cond
           (or preprocessing? loading?)
           [c-loading-spinner]
           
           :else
           (doall
            (for [candidate (range 10)]
              ^{:key (str "candidate-" candidate)}
              [c-candidate-element candidate])))]))))


(defmethod page :route.user/candidates []
  (let [*skills (re/subscribe [:page.candidates/skills])]
    (fn []
      [c-main-layout {:container-opts {:class :candidates-main-container}}
       [c-candidate-search-filter]
       [c-candidate-mobile-search-filter]
       [:div.candidate-listing.listing {:key "listing"}
        [:div.search-container
         [c-chip-search-input
          {:chip-listing @*skills
           :on-chip-listing-change #(re/dispatch [:page.candidates/set-skills %])
           :placeholder "Search Candidate Skills"
           :allow-custom-chips? false
           :auto-suggestion-listing constants/skills}]]
        [c-candidate-listing]]])))
