(ns ethlance.ui.page.arbiters
  (:require [district.ui.component.page :refer [page]]
            [district.ui.router.events :as router-events]
            [ethlance.shared.constants :as constants]
            [ethlance.shared.enumeration.currency-type :as enum.currency]
            [ethlance.ui.component.currency-input :refer [c-currency-input]]
            [ethlance.ui.component.error-message :refer [c-error-message]]
            [ethlance.ui.component.info-message :refer [c-info-message]]
            [ethlance.ui.component.loading-spinner :refer [c-loading-spinner]]
            [ethlance.ui.component.main-layout :refer [c-main-layout]]
            [ethlance.ui.component.mobile-search-filter
             :refer
             [c-mobile-search-filter]]
            [ethlance.ui.component.pagination :refer [c-pagination]]
            [ethlance.ui.component.profile-image :refer [c-profile-image]]
            [ethlance.ui.component.rating :refer [c-rating]]
            [ethlance.ui.component.search-input :refer [c-chip-search-input]]
            [ethlance.ui.component.select-input :refer [c-select-input]]
            [ethlance.ui.component.tag :refer [c-tag c-tag-label]]
            [ethlance.ui.component.text-input :refer [c-text-input]]
            [re-frame.core :as re]))

(defn cf-arbiter-search-filter []
  (let [*category (re/subscribe [:page.arbiters/category])
        *feedback-max-rating (re/subscribe [:page.arbiters/feedback-max-rating])
        *feedback-min-rating (re/subscribe [:page.arbiters/feedback-min-rating])
        *min-hourly-rate (re/subscribe [:page.arbiters/min-hourly-rate])
        *max-hourly-rate (re/subscribe [:page.arbiters/max-hourly-rate])
        *min-num-feedbacks (re/subscribe [:page.arbiters/min-num-feedbacks])
        *country (re/subscribe [:page.arbiters/country])]
    (fn []
      [:<>
       [:div.category-selector
        [c-select-input
         {:selection @*category
          :color :secondary
          :selections constants/categories-with-default
          :on-select #(re/dispatch [:page.arbiters/set-category %])}]]
       [:span.rating-label "Min. Rating"]
       [c-rating {:rating @*feedback-min-rating :color :white :size :small
                  :on-change #(re/dispatch [:page.arbiters/set-feedback-min-rating %])}]

       [:span.rating-label "Max. Rating"]
       [c-rating {:rating @*feedback-max-rating :color :white :size :small
                  :on-change #(re/dispatch [:page.arbiters/set-feedback-max-rating %])}]

       [c-currency-input
        {:placeholder "Min. Hourly Rate"
         :currency-type ::enum.currency/usd
         :color :secondary
         :min 0
         :value @*min-hourly-rate
         :on-change #(re/dispatch [:page.arbiters/set-min-hourly-rate %])}]

       [c-currency-input
        {:placeholder "Max. Hourly Rate"
         :currency-type ::enum.currency/usd
         :color :secondary
         :min 0
         :value @*max-hourly-rate
         :on-change #(re/dispatch [:page.arbiters/set-max-hourly-rate %])}]

       [:div.feedback-input
        [c-text-input
         {:placeholder "Number of Feedbacks"
          :color :secondary
          :type :number :min 0
          :value @*min-num-feedbacks
          :on-change #(re/dispatch [:page.arbiters/set-min-num-feedbacks %])}]]

       [c-select-input
        {:label "Country"
         :selection @*country
         :on-select #(re/dispatch [:page.arbiters/set-country %])
         :selections constants/countries
         :search-bar? true
         :color :secondary
         :default-search-text "Search Countries"}]])))

(defn c-arbiter-search-filter []
  [:div.search-filter
   [cf-arbiter-search-filter]])

(defn c-arbiter-mobile-search-filter
  []
  [c-mobile-search-filter
   [cf-arbiter-search-filter]])

(defn c-arbiter-element
  [{:keys [:user/address]}]
  [:div.arbiter-element {:on-click #(re/dispatch [::router-events/navigate :route.user/profile {:address address} {}])}
   [:div.profile
    [:div.profile-image [c-profile-image {}]]
    [:div.name "Brian Curran"]
    [:div.title "Content Creator, Web Developer, Blockchain Analyst"]]
   [:div.price "$15"]
   [:div.tags
    (doall
     (for [tag-label #{"System Administration" "Game Design" "C++" "HopScotch Master"}]
       ^{:key (str "tag-" tag-label)}
       [c-tag {:on-click #(re/dispatch [:page.arbiters/add-skill tag-label])
               :title (str "Add '" tag-label "' to Search")}
        [c-tag-label tag-label]]))]
   [:div.rating
    [c-rating {:default-rating 3}]
    [:div.label "(4)"]]
   [:div.location "New York, United States"]])


(defn c-arbiter-listing []
  (let [*arbiter-listing-query (atom nil)
        *limit (re/subscribe [:page.arbiters/limit])
        *offset (re/subscribe [:page.arbiters/offset])]
    (fn []
      (let [{arbiter-search   :arbiter-search
             preprocessing?   :graphql/preprocessing?
             loading?         :graphql/loading?
             errors           :graphql/errors} @*arbiter-listing-query
            {arbiter-listing  :items
             total-count      :total-count} arbiter-search]
        [:<>
         (cond
           ;; Errors?
           (seq errors)
           [c-error-message "Failed to process GraphQL" (pr-str errors)]

           ;; Loading?
           (or preprocessing? loading?)
           [c-loading-spinner]

           ;; Empty?
           (empty? arbiter-listing)
           [c-info-message "No Arbiters"]

           :else
           (doall
            (for [arbiter arbiter-listing]
              ^{:key (str "arbiter-" (hash arbiter))}
              [c-arbiter-element arbiter])))

         ;; Pagination
         (when (seq arbiter-listing)
           [c-pagination
            {:total-count total-count
             :limit @*limit
             :offset @*offset
             :set-offset-event :page.arbiters/set-offset}])]))))

(defmethod page :route.user/arbiters []
  (let [*skills (re/subscribe [:page.arbiters/skills])]
    (fn []
      [c-main-layout {:container-opts {:class :arbiters-main-container}}
       [c-arbiter-search-filter]
       [c-arbiter-mobile-search-filter]
       [:div.arbiter-listing.listing {:key "listing"}
        [:div.search-container
         [c-chip-search-input
          {:chip-listing @*skills
           :on-chip-listing-change #(re/dispatch [:page.arbiters/set-skills %])
           :auto-suggestion-listing constants/skills
           :allow-custom-chips? false
           :placeholder "Search Arbiter Skills"}]]
        [c-arbiter-listing]]])))
