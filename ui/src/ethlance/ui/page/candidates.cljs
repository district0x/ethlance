(ns ethlance.ui.page.candidates
  "General Candidate Listings on ethlance"
  (:require
    [cuerdas.core :as str]
    [district.ui.component.page :refer [page]]
    [district.ui.graphql.subs :as gql]
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
    [ethlance.ui.util.navigation :as navigation]
    [ethlance.ui.util.tokens :as tokens]
    [re-frame.core :as re]))


(defn cf-candidate-search-filter
  "Component Fragment for the candidate search filter."
  []
  (let [*category (re/subscribe [:page.candidates/category])
        *feedback-max-rating (re/subscribe [:page.candidates/feedback-max-rating])
        *feedback-min-rating (re/subscribe [:page.candidates/feedback-min-rating])
        *min-hourly-rate (re/subscribe [:page.candidates/min-hourly-rate])
        *max-hourly-rate (re/subscribe [:page.candidates/max-hourly-rate])
        *min-num-feedbacks (re/subscribe [:page.candidates/min-num-feedbacks])
        *country (re/subscribe [:page.candidates/country])]
    (fn []
      [:<>
       [:div.category-selector
        [c-select-input
         {:selection @*category
          :color :secondary
          :label-fn first
          :value-fn second
          :selections constants/categories-with-default
          :on-select #(re/dispatch [:page.candidates/set-category %])}]]

       [:span.rating-label "Min. Rating"]
       [c-rating {:rating @*feedback-min-rating :color :white :size :small
                  :on-change #(re/dispatch [:page.candidates/set-feedback-min-rating %])}]

       [:span.rating-label "Max. Rating"]
       [c-rating {:rating @*feedback-max-rating :color :white :size :small
                  :on-change #(re/dispatch [:page.candidates/set-feedback-max-rating %])}]

       [c-currency-input
        {:placeholder "Min. Hourly Rate"
         :currency-type ::enum.currency/usd
         :color :secondary
         :min 0
         :value @*min-hourly-rate
         :on-change #(re/dispatch [:page.candidates/set-min-hourly-rate %])}]

       [c-currency-input
        {:placeholder "Max. Hourly Rate"
         :currency-type ::enum.currency/usd
         :color :secondary
         :min 0
         :value @*max-hourly-rate
         :on-change #(re/dispatch [:page.candidates/set-max-hourly-rate %])}]

       [:div.feedback-input
        [c-text-input
         {:placeholder "Number of Feedbacks"
          :color :secondary
          :type :number :min 0
          :value @*min-num-feedbacks
          :on-change #(re/dispatch [:page.candidates/set-min-num-feedbacks %])}]]

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
  [candidate]
  [:a.candidate-element (navigation/link-params {:route :route.user/profile
                                                 :params {:address (-> candidate :user/id)}
                                                 :query {:tab "candidate"}})
   [:div.profile
    [:div.profile-image [c-profile-image {:src (-> candidate :user :user/profile-image)}]]
    [:div.name (-> candidate :user :user/name)]
    [:div.title (str/title (-> candidate :candidate/professional-title)) (str " (" (-> candidate :user :user/country) ")")]]
   [:div.price (tokens/fiat-amount-with-symbol (-> candidate :candidate/rate-currency-id) (-> candidate :candidate/rate))]
   [:div.tags
    (doall
      (for [tag-label (-> candidate :candidate/skills)]
        ^{:key (str "tag-" tag-label)}
        [c-tag {:on-click #(re/dispatch  [:page.candidates/add-skill tag-label])
                :title (str "Add '" tag-label "' to Search")}
         [c-tag-label tag-label]]))]
   [:div.rating
    [c-rating {:rating (-> candidate :candidate/rating)}]
    [:div.label {:title "Number of reviews"} (str "(" (get-in candidate [:candidate/feedback :total-count] 0) ")")]]])


(defn c-candidate-listing
  []
  (let [*limit (re/subscribe [:page.candidates/limit])
        *offset (re/subscribe [:page.candidates/offset])
        query-params (re/subscribe [:page.candidates/search-params])]
    (fn []
      (let [query [:candidate-search @query-params
                   [:total-count
                    [:items [:user/id
                             [:user [:user/id
                                     :user/name
                                     :user/country
                                     :user/profile-image]]
                             [:candidate/feedback [:total-count]]
                             :candidate/professional-title
                             :candidate/categories
                             :candidate/skills
                             :candidate/rating
                             :candidate/rate
                             :candidate/rate-currency-id]]]]
            *candidate-listing-query (re/subscribe [::gql/query {:queries [query]} {:id @query-params}])
            {candidate-search  :candidate-search
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
             :limit (or @*limit 20)
             :offset (or @*offset 0)
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
