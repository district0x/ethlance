(ns ethlance.ui.page.candidates
  "General Candidate Listings on ethlance"
  (:require [cuerdas.core :as str]
            [district.ui.component.page :refer [page]]
            [district.ui.router.events :as router-events]
            [ethlance.shared.constants :as constants]
            [ethlance.ui.component.error-message :refer [c-error-message]]
            [ethlance.ui.component.info-message :refer [c-info-message]]
            [ethlance.ui.component.loading-spinner :refer [c-loading-spinner]]
            [ethlance.ui.component.main-layout :refer [c-main-layout]]
            [ethlance.ui.component.mobile-search-filter
             :refer
             [c-mobile-search-filter]]
            [ethlance.ui.component.pagination :refer [c-pagination]]
            [ethlance.ui.component.profile-image :refer [c-profile-image]]
            [ethlance.ui.component.radio-select
             :refer
             [c-radio-search-filter-element c-radio-select]]
            [ethlance.ui.component.rating :refer [c-rating]]
            [ethlance.ui.component.search-input :refer [c-chip-search-input]]
            [ethlance.ui.component.select-input :refer [c-select-input]]
            [ethlance.ui.component.tag :refer [c-tag c-tag-label]]
            [re-frame.core :as re]))

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

(defn c-candidate-search-filter
  []
  [:div.search-filter
   [cf-candidate-search-filter]])

(defn c-candidate-mobile-search-filter
  []
  [c-mobile-search-filter
   [cf-candidate-search-filter]])

(defn c-candidate-element
  [{:keys [:user/address
           :candidate/professional-title
           :candidate/skills]}]
  [:div.candidate-element {:on-click #(re/dispatch [::router-events/navigate :route.user/profile {:address address} {}])}
   [:div.profile
    [:div.profile-image [c-profile-image {}]]
    [:div.name "Brian Curran"]
    [:div.title (str/title professional-title)]]
   [:div.price "$15"]
   [:div.tags
    (doall
     (for [tag-label skills]
       ^{:key (str "tag-" tag-label)}
       [c-tag {:on-click #(re/dispatch  [:page.candidates/add-skill tag-label])
               :title (str "Add '" tag-label "' to Search")}
        [c-tag-label tag-label]]))]
   [:div.rating
    [c-rating {:default-rating 3}]
    [:div.label "(4)"]]
   [:div.location "New York, United States"]])


(defn c-candidate-listing []
  (let [*limit (re/subscribe [:page.candidates/limit])
        *offset (re/subscribe [:page.candidates/offset])
        *candidate-listing-query (atom nil)]
    (fn []
      (let [{candidate-search  :candidate-search
             preprocessing?    :graphql/preprocessing?
             loading?          :graphql/loading?
             errors            :graphql/errors} @*candidate-listing-query
            {candidate-listing :items
             total-count       :total-count} candidate-search]
        [:<>
         (cond
           ;; Errors?
           (seq errors)
           [c-error-message "Failed to process GraphQL" (pr-str errors)]

           ;; Loading?
           (or preprocessing? loading?)
           [c-loading-spinner]

           ;; Empty?
           (empty? candidate-listing)
           [c-info-message "No Candidates"]

           :else
           (doall
            (for [candidate candidate-listing]
              ^{:key (str "candidate-" (hash candidate))}
              [c-candidate-element candidate])))

         ;; Pagination
         (when (seq candidate-listing)
           [c-pagination
            {:total-count total-count
             :limit @*limit
             :offset @*offset
             :set-offset-event :page.candidates/set-offset}])]))))

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
