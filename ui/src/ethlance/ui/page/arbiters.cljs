(ns ethlance.ui.page.arbiters
  (:require
    [district.ui.component.page :refer [page]]
    [district.ui.graphql.subs :as gql]
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
    [ethlance.ui.util.navigation :as navigation]
    [ethlance.ui.util.tokens :as tokens]
    [re-frame.core :as re]))


(defn cf-arbiter-search-filter
  []
  (let [*category (re/subscribe [:page.arbiters/category])
        *feedback-max-rating (re/subscribe [:page.arbiters/feedback-max-rating])
        *feedback-min-rating (re/subscribe [:page.arbiters/feedback-min-rating])
        *min-fee (re/subscribe [:page.arbiters/min-fee])
        *max-fee (re/subscribe [:page.arbiters/max-fee])
        *min-num-feedbacks (re/subscribe [:page.arbiters/min-num-feedbacks])
        *country (re/subscribe [:page.arbiters/country])]
    (fn []
      [:<>
       [:div.category-selector
        [c-select-input
         {:selection @*category
          :color :secondary
          :label-fn first
          :value-fn second
          :selections constants/categories-with-default
          :on-select #(re/dispatch [:page.arbiters/set-category %])}]]
       [:span.rating-label "Min. Rating"]
       [c-rating {:rating @*feedback-min-rating :color :white :size :small
                  :on-change #(re/dispatch [:page.arbiters/set-feedback-min-rating %])}]

       [:span.rating-label "Max. Rating"]
       [c-rating {:rating @*feedback-max-rating :color :white :size :small
                  :on-change #(re/dispatch [:page.arbiters/set-feedback-max-rating %])}]

       [c-currency-input
        {:placeholder "Min. Fee"
         :currency-type ::enum.currency/usd
         :color :secondary
         :min 0
         :value @*min-fee
         :on-change #(re/dispatch [:page.arbiters/set-min-fee %])}]

       [c-currency-input
        {:placeholder "Max. Fee"
         :currency-type ::enum.currency/usd
         :color :secondary
         :min 0
         :value @*max-fee
         :on-change #(re/dispatch [:page.arbiters/set-max-fee %])}]

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


(defn c-arbiter-search-filter
  []
  [:div.search-filter
   [cf-arbiter-search-filter]])


(defn c-arbiter-mobile-search-filter
  []
  [c-mobile-search-filter
   [cf-arbiter-search-filter]])


(defn c-arbiter-element
  [{:keys [:user/id] :as arbiter}]
  [:a.arbiter-element (navigation/link-params {:route :route.user/profile
                                               :params {:address id}
                                               :query {:tab "arbiter"}})
   [:div.profile
    [:div.profile-image [c-profile-image {:src (-> arbiter :user :user/profile-image)}]]
    [:div.name (get-in arbiter [:user :user/name])]]
   [:div.price (tokens/fiat-amount-with-symbol (-> arbiter :arbiter/fee-currency-id)
                                               (-> arbiter :arbiter/fee))]
   [:div.tags
    (doall
      (for [tag-label (get arbiter :skills)]
        ^{:key (str "tag-" tag-label)}
        [c-tag {:on-click #(re/dispatch [:page.arbiters/add-skill tag-label])
                :title (str "Add '" tag-label "' to Search")}
         [c-tag-label tag-label]]))]
   [:div.rating
    [c-rating {:rating (-> arbiter :arbiter/rating)}]
    [:div.label {:title "Number of reviews"} (str "(" (get-in arbiter [:arbiter/feedback :total-count] 0) ")")]]
   [:div.location (get-in arbiter [:user :user/country])]])


(defn c-arbiter-listing
  []
  (let [query-params (re/subscribe [:page.arbiters/search-params])
        query [:arbiter-search @query-params
               [:total-count
                [:items [:user/id
                         [:user [:user/id
                                 :user/name
                                 :user/country
                                 :user/profile-image]]
                         [:arbiter/feedback [:total-count]]
                         :arbiter/categories
                         :arbiter/skills
                         :arbiter/rating
                         :arbiter/fee
                         :arbiter/fee-currency-id]]]]
        results (re/subscribe [::gql/query {:queries [query]} {:id @query-params}])
        *limit (re/subscribe [:page.arbiters/limit])
        *offset (re/subscribe [:page.arbiters/offset])
        {arbiter-search   :arbiter-search
         preprocessing?   :graphql/preprocessing?
         loading?         :graphql/loading?
         errors           :graphql/errors} @results
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
         :set-offset-event :page.arbiters/set-offset}])]))


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
