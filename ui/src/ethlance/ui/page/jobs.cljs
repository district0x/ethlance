(ns ethlance.ui.page.jobs
  "General Job Listings on ethlance"
  (:require [cuerdas.core :as str]
            [district.format :as format]
            [district.ui.component.page :refer [page]]
            [district.ui.router.events :as router-events]
            [inflections.core :as inflections]
            [district.ui.graphql.subs :as gql]
            [ethlance.shared.constants :as constants]
            [ethlance.shared.enumeration.currency-type :as enum.currency]
            [ethlance.ui.util.navigation :as util.navigation]
            [ethlance.ui.component.currency-input :refer [c-currency-input]]
            [ethlance.ui.component.inline-svg :refer [c-inline-svg]]
            [ethlance.ui.component.loading-spinner :refer [c-loading-spinner]]
            [ethlance.ui.component.main-layout :refer [c-main-layout]]
            [ethlance.ui.component.mobile-search-filter
             :refer
             [c-mobile-search-filter]]
            [ethlance.ui.component.radio-select
             :refer
             [c-radio-search-filter-element c-radio-select]]
            [ethlance.ui.component.rating :refer [c-rating]]
            [ethlance.ui.component.search-input :refer [c-chip-search-input]]
            [ethlance.ui.component.select-input :refer [c-select-input]]
            [ethlance.ui.component.tag :refer [c-tag c-tag-label]]
            [ethlance.ui.component.text-input :refer [c-text-input]]
            [ethlance.ui.page.jobs.graphql :as j-gql]
            [re-frame.core :as re]))

(defn c-user-employer-detail
  [employer]
  (let [name (get-in employer [:user :user/name])
        rating (get-in employer [:employer/rating])
        rating-count (get-in employer [:employer/feedback :total-count])
        country (get-in employer [:user :user/country])
        address (get-in employer [:user/id])]
    [:div.user-detail.employer
     {:on-click (util.navigation/create-handler {:route :route.user/profile
                                                 :params {:address address}
                                                 :query {:tab :employer}})
      :href (util.navigation/resolve-route {:route :route.user/profile
                                            :params {:address address}
                                            :query {:tab :employer}})}
     [:div.name name]
     [:div.rating-container
      [c-rating {:size :small :color :primary :default-rating rating}]
      (when rating-count [:div.rating-label (str "(" rating-count ")")])]
     [:div.location country]]))

(defn c-user-arbiter-detail
  [arbiter]
  (let [name (get-in arbiter [:user :user/name])
        rating (get-in arbiter [:employer/rating])
        rating-count (get-in arbiter [:arbiter/feedback :total-count])
        country (get-in arbiter [:user :user/country])
        address (get-in arbiter [:user/id])
        ]
    [:div.user-detail.arbiter
     {:on-click (util.navigation/create-handler {:route :route.user/profile
                                                 :params {:address address}
                                                 :query {:tab :arbiter}})
      :href (util.navigation/resolve-route {:route :route.user/profile
                                            :params {:address address}
                                            :query {:tab :arbiter}})}
     [c-inline-svg {:class "arbiter-icon" :src "images/svg/hammer.svg"}]
     [:div.name name]
     [:div.rating-container
      [c-rating {:size :small :color :primary :default-rating rating}]
      (when rating-count [:div.rating-label (str "(" rating-count ")")])]
     [:div.location country]]))

(defn c-job-detail-table
  [{:job/keys [bid-option required-experience-level estimated-project-length required-availability]}]
  (let [experience-level (keyword required-experience-level)
        formatted-experience-level (case experience-level
                                     :beginner "Novice ($)"
                                     :intermediate "Professional ($$)"
                                     :expert "Expert ($$$)")]
    [:div.job-detail-table
     [:div.name "Payment Type"]
     [:div.value (str/title bid-option)]

     [:div.name "Experience Level"]
     [:div.value formatted-experience-level]

     [:div.name "Project Length"]
     [:div.value (str/title estimated-project-length)]

     [:div.name "Availability"]
     [:div.value (str/title required-availability)]]))

(defn cf-job-search-filter
  "Component Fragment for the job search filter."
  []
  (let [*category (re/subscribe [:page.jobs/category])
        *feedback-max-rating (re/subscribe [:page.jobs/feedback-max-rating])
        *feedback-min-rating (re/subscribe [:page.jobs/feedback-min-rating])
        *min-hourly-rate (re/subscribe [:page.jobs/min-hourly-rate])
        *max-hourly-rate (re/subscribe [:page.jobs/max-hourly-rate])
        *min-num-feedbacks (re/subscribe [:page.jobs/min-num-feedbacks])
        *payment-type (re/subscribe [:page.jobs/payment-type])
        *experience-level (re/subscribe [:page.jobs/experience-level])]
    (re/dispatch [:page.jobs/set-feedback-max-rating 5])
    (fn []
      [:<>
       [:div.category-selector
        [c-select-input
         {:selection @*category
          :color :secondary
          :label-fn first
          :value-fn second
          :selections constants/categories-with-default
          :on-select #(re/dispatch [:page.jobs/set-category %])}]]

       [:span.rating-label "Min. Rating"]
       [c-rating {:rating @*feedback-min-rating :color :white :size :small
                  :on-change #(re/dispatch [:page.jobs/set-feedback-min-rating %])}]

       [:span.rating-label "Max. Rating"]
       [c-rating {:rating @*feedback-max-rating :color :white :size :small
                  :on-change #(re/dispatch [:page.jobs/set-feedback-max-rating %])}]

       [c-currency-input
        {:placeholder "Min. Hourly Rate"
         :currency-type ::enum.currency/usd
         :color :secondary
         :min 0
         :value @*min-hourly-rate
         :on-change #(re/dispatch [:page.jobs/set-min-hourly-rate %])}]

       [c-currency-input
        {:placeholder "Max. Hourly Rate"
         :currency-type ::enum.currency/usd
         :color :secondary
         :min 0
         :value @*max-hourly-rate
         :on-change #(re/dispatch [:page.jobs/set-max-hourly-rate %])}]

       [:div.feedback-input
        [c-text-input
         {:placeholder "Number of Feedbacks"
          :color :secondary
          :type :number :min 0
          :value @*min-num-feedbacks
          :on-change #(re/dispatch [:page.jobs/set-min-num-feedbacks %])}]]

       [:span.selection-label "Payment Type"]
       [c-radio-select
        {:selection (keyword @*payment-type)
         :on-selection #(re/dispatch [:page.jobs/set-payment-type (name %)])}
        [:hourly-rate [c-radio-search-filter-element "Hourly Rate"]]
        [:fixed-price [c-radio-search-filter-element "Fixed Price"]]
        [:annual-salary [c-radio-search-filter-element "Annual Salary"]]]

       [:span.selection-label "Experience Level"]
       [c-radio-select
        {:selection (keyword @*experience-level) ; FIXME: the string -> keyword shouldn't be necessary after switching to district-ui-graphql
         :on-selection #(re/dispatch [:page.jobs/set-experience-level (name %)])} ; FIXME: the keyword -> string shouldn't be necessary after switching to district-ui-graphql
        [:beginner [c-radio-search-filter-element "Novice ($)"]]
        [:intermediate [c-radio-search-filter-element "Professional ($$)"]]
        [:expert [c-radio-search-filter-element "Expert ($$$)"]]]])))


(defn c-job-search-filter
  "Sidebar component for changing the search criteria."
  []
  [:div.job-search-filter.search-filter
   [cf-job-search-filter]])


(defn c-job-mobile-search-filter
  []
  [c-mobile-search-filter
   [cf-job-search-filter]])


(defn c-job-element
  "A single job element component composed from the job data."
  [{:job/keys [title description date-created required-skills arbiter employer id] :as job}]
  (let [proposals-count (get-in job [:job-stories :total-count])
        ; TODO: remove new js/Date after switching to district.ui.graphql that converts Date GQL type automatically
        relative-ago (format/time-ago (new js/Date date-created))
        pluralized-proposals (inflections/pluralize proposals-count "proposal")]
    [:div.job-element {:on-click (fn [event]
                                    (re/dispatch [::router-events/navigate :route.job/detail {:id id}]))}
     [:div.title title]
     [:div.description description]
     [:div.date (str "Posted " relative-ago " | " pluralized-proposals)]
     [:div.tags
      (doall
       (for [skill-label required-skills]
         ^{:key (str "tag-" skill-label)}
         [c-tag {:on-click #(re/dispatch [:page.jobs/add-skill skill-label])
                 :title (str "Add '" skill-label "' to Search")}
          [c-tag-label skill-label]]))]

     [:div.users
      [c-user-employer-detail employer]
      (when arbiter [c-user-arbiter-detail arbiter])]

     [:div.details
      [c-job-detail-table job]]]))


(defn c-job-listing []
  (fn []
    (let [query-params (re/subscribe [:page.jobs/job-search-params])
          query (j-gql/jobs-query @query-params)
          search-results @(re/subscribe [::gql/query {:queries [query]} {:id @query-params :refetch-on #{:gimme-jobs}}])
          job-listing-state (get search-results :graphql/loading?)
          loading? (contains? #{:start :loading} job-listing-state)
          job-listing (get-in (first search-results) [:job-search :items])]
      [:<>
       (cond
         ;; Is the job listing loading?
         loading?
         [c-loading-spinner]

         ;; Is the job listing empty?
         (empty? job-listing)
         [:div.empty-listing  "No jobs found for these search parameters"]
         :else
         (doall
           (for [job job-listing]
             ^{:key (str "job-" (:job/id job))}
             [c-job-element job])))])))

(defmethod page :route.job/jobs []
  (let [*skills (re/subscribe [:page.jobs/skills])]
    [c-main-layout {:container-opts {:class :jobs-main-container}}
     [c-job-search-filter]
     [c-job-mobile-search-filter]
     [:div.job-listing.listing {:key "listing"}
      [:div.search-container
       [c-chip-search-input
        {:chip-listing @*skills
         :on-chip-listing-change #(re/dispatch [:page.jobs/set-skills %])
         :placeholder "Search Job Skill Requirements"
         :allow-custom-chips? false
         :auto-suggestion-listing constants/skills}]]
      [c-job-listing]]]))
