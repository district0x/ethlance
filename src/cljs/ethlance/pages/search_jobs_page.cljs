(ns ethlance.pages.search-jobs-page
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.category-select-field :refer [category-select-field]]
    [ethlance.components.checkbox-group :refer [checkbox-group]]
    [ethlance.components.country-auto-complete :refer [country-auto-complete]]
    [ethlance.components.icons :as icons]
    [ethlance.components.language-select-field :refer [language-select-field]]
    [ethlance.components.misc :as misc :refer [col row paper-thin row-plain a currency]]
    [ethlance.components.search :refer [skills-input]]
    [ethlance.components.search-results :as search-results]
    [ethlance.components.skills-chip-input :refer [skills-chip-input]]
    [ethlance.components.skills-chips :refer [skills-chips]]
    [ethlance.components.star-rating :refer [star-rating]]
    [ethlance.components.state-select-field :refer [state-select-field]]
    [ethlance.constants :as constants]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn filter-sidebar []
  (let [form-data (subscribe [:form/search-jobs])]
    (fn []
      (let [{:keys [:search/category :search/min-employer-avg-rating :search/country :search/state
                    :search/language :search/experience-levels :search/payment-types
                    :search/estimated-durations :search/hours-per-weeks :search/min-budget :search/min-budget-currency
                    :search/min-employer-ratings-count]} @form-data]
        [misc/call-on-change
         {:load-on-mount? true
          :args @form-data
          :on-change #(dispatch [:after-eth-contracts-loaded [:contract.search/search-jobs-deb @form-data]])}
         [search-results/search-paper-thin
          [category-select-field
           {:value category
            :full-width true
            :on-change #(dispatch [:form.search/set-value :search/category %3])}]
          [misc/subheader "Min. Employer Rating"]
          [star-rating
           {:value (u/rating->star min-employer-avg-rating)
            :on-star-click #(dispatch [:form.search/set-value :search/min-employer-avg-rating (u/star->rating %1)])}]
          [misc/subheader "Payment Type"]
          [checkbox-group
           {:options constants/payment-types
            :values payment-types
            :on-change #(dispatch [:form.search/set-value :search/payment-types %2])}]
          [misc/subheader "Experience Level"]
          [checkbox-group
           {:options constants/experience-levels
            :values experience-levels
            :on-change #(dispatch [:form.search/set-value :search/experience-levels %2])}]
          [misc/subheader "Project Length"]
          [checkbox-group
           {:options constants/estimated-durations
            :values estimated-durations
            :on-change #(dispatch [:form.search/set-value :search/estimated-durations %2])}]
          [misc/subheader "Availability"]
          [checkbox-group
           {:options constants/hours-per-weeks
            :values hours-per-weeks
            :on-change #(dispatch [:form.search/set-value :search/hours-per-weeks %2])}]
          [misc/ether-field-with-currency-select-field
           {:ether-field-props
            {:floating-label-text "Min. Budget"
             :floating-label-fixed true
             :full-width true
             :allow-empty? true
             :value min-budget
             :on-change #(dispatch [:form.search/set-value :search/min-budget %2 u/non-neg-or-empty-ether-value?])}
            :currency-select-field-props
            {:value min-budget-currency
             :on-change (fn [_ _ currency]
                          (dispatch [:form.search/set-value :search/min-budget-currency currency])
                          (dispatch [:selected-currency/set currency]))}}]
          [misc/text-field-base
           {:floating-label-text "Min. Employer Feedbacks"
            :floating-label-fixed true
            :type :number
            :value min-employer-ratings-count
            :full-width true
            :min 0
            :on-change #(dispatch [:form.search/set-value :search/min-employer-ratings-count %2])}]
          [country-auto-complete
           {:value country
            :full-width true
            :on-new-request #(dispatch [:form.search/set-value :search/country %2])}]
          (when (u/united-states? country)
            [state-select-field
             {:value state
              :full-width true
              :on-change #(dispatch [:form.search/set-value :search/state %3])}])
          [language-select-field
           {:value language
            :full-width true
            :on-new-request #(dispatch [:form.search/set-value :search/language %2])}]
          [misc/search-filter-reset-button]
          [misc/search-filter-done-button
           {:on-touch-tap #(dispatch [:search-filter.jobs/set-open? false])}]]]))))

(defn change-page [new-offset]
  (dispatch [:form.search/set-value :search/offset new-offset])
  (dispatch [:window/scroll-to-top]))

(defn search-results []
  (let [list (subscribe [:list/search-jobs])
        selected-skills (subscribe [:form/search-job-skills])
        form-data (subscribe [:form/search-jobs])]
    (fn []
      (let [{:keys [:loading? :items]} @list
            {:keys [:search/offset :search/limit]} @form-data]
        [search-results/search-results
         {:items-count (count items)
          :loading? loading?
          :offset offset
          :limit limit
          :no-items-found-text "No jobs match your search criteria"
          :no-more-items-text "No more jobs found"
          :next-button-text "Older"
          :prev-button-text "Newer"
          :on-page-change change-page}
         (for [{:keys [:job/title :job/id :job/payment-type :job/estimated-duration
                       :job/experience-level :job/hours-per-week :job/created-on
                       :job/description :job/budget :job/skills :job/reference-currency
                       :job/sponsorable?] :as item} items]
           [:div {:key id}
            [:h2
             {:style styles/overflow-ellipsis}
             [a {:style styles/search-result-headline
                 :route :job/detail
                 :route-params {:job/id id}} title]]
            [:div {:style styles/job-info}
             [:span (u/time-ago created-on)]
             (when sponsorable?
               [:span " - " [:span
                             {:style styles/sponsorable-search-result-text}
                             "Looking for Sponsors"]])
             [:span " - " (constants/payment-types payment-type)]
             [:span " - " (constants/experience-levels experience-level)]
             [:span " - Est. Time: " (constants/estimated-durations estimated-duration)]
             [:span " - " (constants/hours-per-weeks hours-per-week)]
             (when (u/big-num-pos? budget)
               [:span " - Budget: " [:span
                                     {:style styles/dark-text}
                                     [misc/currency budget {:value-currency reference-currency}]]])]
            [skills-chips
             {:selected-skills skills
              :always-show-all? true
              :on-touch-tap (fn [skill-id]
                              (when-not (contains? (set @selected-skills) skill-id)
                                (dispatch [:form.search/set-value :search/skills
                                           (conj (into [] @selected-skills) skill-id)])))}]
            [search-results/search-results-employer
             {:user (:job/employer item)
              :show-balance? true}]
            [misc/hr-small]])]))))

(defn search-jobs-page []
  [misc/search-layout
   {:filter-drawer-props {:open @(subscribe [:db/search-jobs-filter-open?])
                          :on-request-change #(dispatch [:search-filter.jobs/set-open? %])}
    :filter-open-button-props {:on-touch-tap #(dispatch [:search-filter.jobs/set-open? true])}}
   [filter-sidebar]
   [skills-input
    {:selected-skills-subscribe [:form/search-job-skills]
     :selected-skills-or-subscribe [:form/search-job-skills-or]
     :open-subscribe [:db/search-jobs-skills-open?]
     :skills-hint-text "Type skills required for a job"
     :skills-and-hint-text "All of entered skills are required for a job"
     :skills-or-hint-text "At least one of entered skills is required for a job"
     :skills-and-floating-label-text "All of skills are required"
     :skills-or-floating-label-text "Any of skills is required"
     :on-toggle-open #(dispatch [:toggle-search-skills-input :search-jobs-skills-open? :form/search-jobs %])}]
   [search-results]])
