(ns ethlance.ui.page.employers
  "General Employer Listings on ethlance"
  (:require
    [district.ui.component.page :refer [page]]
    [district.ui.graphql.subs :as gql]
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
    [ethlance.ui.component.rating :refer [c-rating]]
    [ethlance.ui.component.search-input :refer [c-chip-search-input]]
    [ethlance.ui.component.select-input :refer [c-select-input]]
    [ethlance.ui.component.text-input :refer [c-text-input]]
    [ethlance.ui.util.navigation :as navigation]
    [re-frame.core :as re]))


(defn cf-employer-search-filter
  []
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


(defn c-employer-search-filter
  []
  [:div.search-filter
   [cf-employer-search-filter]])


(defn c-employer-mobile-search-filter
  []
  [c-mobile-search-filter
   [cf-employer-search-filter]])


(defn c-employer-element
  [{:employer/keys [professional-title] :as employer}]
  [:a.employer-element (navigation/link-params {:route :route.user/profile
                                                :params {:address (:user/id employer)}
                                                :query {:tab "employer"}})
   [:div.profile
    [:div.profile-image [c-profile-image {:src (get-in employer [:user :user/profile-image])}]]
    [:div.name (get-in employer [:user :user/name])]
    [:div.title professional-title]]
   [:div.rating
    [c-rating {:rating (-> employer :employer/rating)}]
    [:div.label {:title "Number of reviews"} (str "(" (get-in employer [:employer/feedback :total-count] 0) ")")]]
   [:div.location (get-in employer [:user :user/country])]])


(defn c-employer-listing
  []
  (let [*limit (re/subscribe [:page.employers/limit])
        *offset (re/subscribe [:page.employers/offset])
        *employer-listing-query
        (re/subscribe
          [::gql/query
           {:queries
            [[:employer-search
              {:limit @*limit
               :offset @*offset}
              [[:items [:user/id
                        [:user
                         [:user/id
                          :user/name
                          :user/country
                          :user/profile-image]]
                        :employer/bio
                        :employer/professional-title]]
               :total-count
               :end-cursor]]]}])]
    (fn []
      (let [{employer-search  :employer-search
             preprocessing?   :graphql/preprocessing?
             loading?         :graphql/loading?
             errors           :graphql/errors} @*employer-listing-query
            {employer-listing :items
             total-count      :total-count} employer-search]
        [:<>
         (cond
           ;; Errors?
           (seq errors)
           [c-error-message "Failed to process GraphQL" (pr-str errors)]

           ;; Loading?
           (or preprocessing? loading?)
           [c-loading-spinner]

           ;; Empty?
           (empty? employer-listing)
           [c-info-message "No Employers"]

           :else
           (doall
             (for [employer employer-listing]
               ^{:key (str "employer-" (hash employer))}
               [c-employer-element employer])))

         ;; Pagination
         (when (seq employer-listing)
           [c-pagination
            {:total-count total-count
             :limit @*limit
             :offset @*offset
             :set-offset-event :page.employers/set-offset}])]))))


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
