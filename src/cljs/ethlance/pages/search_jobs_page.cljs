(ns ethlance.pages.search-jobs-page
  (:require
    [cljs-react-material-ui.icons :as icons]
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.category-select-field :refer [category-select-field]]
    [ethlance.components.checkbox-group :refer [checkbox-group]]
    [ethlance.components.country-auto-complete :refer [country-auto-complete]]
    [ethlance.components.language-select-field :refer [language-select-field]]
    [ethlance.components.misc :as misc :refer [col row paper-thin row-plain a]]
    [ethlance.components.skills-chip-input :refer [skills-chip-input]]
    [ethlance.components.skills-chips :refer [skills-chips]]
    [ethlance.components.slider-with-counter :refer [slider-with-counter]]
    [ethlance.components.star-rating :refer [star-rating]]
    [ethlance.components.truncated-text :refer [truncated-text]]
    [ethlance.constants :as constants]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn filter-sidebar []
  (let [form-data (subscribe [:form/search-jobs])]
    (fn []
      (let [{:keys [:search/category :search/min-employer-avg-rating :search/country
                    :search/language :search/experience-levels :search/payment-types
                    :search/estimated-durations :search/hours-per-weeks :search/min-budget
                    :search/min-employer-ratings-count]} @form-data]
        [misc/call-on-change
         {:load-on-mount? true
          :args @form-data
          :on-change #(dispatch [:after-eth-contracts-loaded [:contract.search/search-jobs @form-data]])}
         [paper-thin
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
          [misc/ether-field
           {:floating-label-text "Min. Budget (Ether)"
            :value min-budget
            :full-width true
            :on-change #(dispatch [:form.search/set-value :search/min-budget %])}]
          [misc/text-field
           {:floating-label-text "Min. Employer Feedbacks"
            :type :number
            :value min-employer-ratings-count
            :full-width true
            :min 0
            :on-change #(dispatch [:form.search/set-value :search/min-employer-ratings-count %2])}]
          [country-auto-complete
           {:value country
            :full-width true
            :on-new-request #(dispatch [:form.search/set-value :search/country %2])}]
          [language-select-field
           {:value language
            :full-width true
            :on-new-request #(dispatch [:form.search/set-value :search/language %2])}]
          [misc/search-reset-button]]]))))

(defn search-results-employer [{:keys [:employer/jobs-count :employer/avg-rating :employer/total-paid
                                       :user/name :user/id :employer/ratings-count :user/country :user/balance]}]
  [:div {:style styles/employer-info-wrap}
   (when (seq name)
     [row-plain
      {:middle "xs"
       :style styles/employer-info}
      [:span [a {:route :employer/detail
                 :route-params {:user/id id}} name]]
      [star-rating
       {:value (u/rating->star avg-rating)
        :small? true
        :style styles/employer-rating-search}]
      [:span
       {:style styles/employer-info-item}
       ratings-count (u/pluralize " feedback" ratings-count)]
      [:span
       {:style styles/employer-info-item}
       [:span {:style styles/dark-text} (u/eth total-paid)] " spent"]
      [:span
       {:style styles/employer-info-item}
       [:span {:style styles/dark-text} (u/eth balance)] " balance"]
      [misc/country-marker
       {:country country
        :row-props {:style styles/employer-info-item}}]])])


(defn change-page [new-offset]
  (dispatch [:form.search/set-value :search/offset new-offset])
  (dispatch [:window/scroll-to-top]))

(defn pagination []
  (let [form-data (subscribe [:form/search-jobs])]
    (fn [items-count]
      (let [{:keys [:search/limit :search/offset]} @form-data]
        [row-plain {:end "xs"}
         (when (pos? offset)
           [ui/flat-button
            {:secondary true
             :label "Newer"
             :icon (icons/navigation-chevron-left)
             :on-touch-tap #(change-page (- offset limit))}])
         (when (= items-count limit)
           [ui/flat-button
            {:secondary true
             :label "Older"
             :label-position :before
             :icon (icons/navigation-chevron-right)
             :on-touch-tap #(change-page (+ offset limit))}])]))))

(defn search-results []
  (let [list (subscribe [:list/search-jobs])
        selected-skills (subscribe [:form/search-job-skills])
        form-data (subscribe [:form/search-jobs])]
    (fn []
      (let [{:keys [:loading? :items]} @list
            {:keys [:search/offset :search/limit]} @form-data]
        [misc/search-results
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
                       :job/description :job/budget :job/skills] :as item} items]
           [:div {:key id}
            [:h2
             {:style styles/overflow-ellipsis}
             [a {:style styles/primary-text
                 :route :job/detail
                 :route-params {:job/id id}} title]]
            [:div {:style styles/job-info}
             [:span (u/time-ago created-on)]
             [:span " - " (constants/payment-types payment-type)]
             [:span " - " (constants/experience-levels experience-level)]
             [:span " - Est. Time: " (constants/estimated-durations estimated-duration)]
             [:span " - " (constants/hours-per-weeks hours-per-week)]
             [:span " - Budget: " [:span {:style styles/dark-text} (u/eth budget)]]
             #_(when (.greaterThan budget 0)
                 [:span " - Budget: " (.toNumber budget) " ETH"])]
            [:div {:style styles/job-list-description}
             [truncated-text
              {:lines 2}
              description]]
            [skills-chips
             {:selected-skills skills
              :on-touch-tap (fn [skill-id]
                              (when-not (contains? (set @selected-skills) skill-id)
                                (dispatch [:form.search/set-value :search/skills
                                           (conj (into [] @selected-skills) skill-id)])))}]
            [search-results-employer (:job/employer item)]
            [misc/hr-small]])]))))

(defn skills-input []
  (let [selected-skills (subscribe [:form/search-job-skills])]
    (fn []
      [paper-thin
       [skills-chip-input
        {:value @selected-skills
         :hint-text "Type skills required for a job"
         :on-change #(dispatch [:form.search/set-value :search/skills %1])}]])))

(defn search-jobs-page []
  [misc/search-layout
   [filter-sidebar]
   [skills-input]
   [search-results]])
