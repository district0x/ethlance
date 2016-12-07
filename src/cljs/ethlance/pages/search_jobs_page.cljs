(ns ethlance.pages.search-jobs-page
  (:require
    [cljs-react-material-ui.icons :as icons]
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.category-select-field :refer [category-select-field]]
    [ethlance.components.checkbox-group :refer [checkbox-group]]
    [ethlance.components.country-select-field :refer [country-select-field]]
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
    (dispatch [:after-eth-contracts-loaded :contract.search/search-jobs @form-data])
    (fn []
      (let [{:keys [:search/category :search/min-employer-avg-rating :search/country
                    :search/language :search/experience-levels :search/payment-types
                    :search/estimated-durations :search/hours-per-weeks :search/min-budget
                    :search/min-employer-ratings-count]} @form-data]
        [paper-thin
         [category-select-field
          {:value category
           :full-width true
           :on-change #(dispatch [:form/search-jobs-changed :search/category %3])}]
         [u/subheader "Min. Employer Rating"]
         [star-rating
          {:value (u/rating->star min-employer-avg-rating)
           :on-star-click #(dispatch [:form/search-jobs-changed :search/min-employer-avg-rating (u/star->rating %1)])}]
         [u/subheader "Payment Type"]
         [checkbox-group
          {:options constants/payment-types
           :values payment-types
           :on-change #(dispatch [:form/search-jobs-changed :search/payment-types %2])}]
         [u/subheader "Experience Level"]
         [checkbox-group
          {:options constants/experience-levels
           :values experience-levels
           :on-change #(dispatch [:form/search-jobs-changed :search/experience-levels %2])}]
         [u/subheader "Project Length"]
         [checkbox-group
          {:options constants/estimated-durations
           :values estimated-durations
           :on-change #(dispatch [:form/search-jobs-changed :search/estimated-durations %2])}]
         [u/subheader "Availability"]
         [checkbox-group
          {:options constants/hours-per-weeks
           :values hours-per-weeks
           :on-change #(dispatch [:form/search-jobs-changed :search/hours-per-weeks %2])}]
         [ui/text-field
          {:floating-label-text "Min. Budget"
           :type :number
           :default-value min-budget
           :full-width true
           :min 0
           :on-change #(dispatch [:form/search-jobs-changed :search/min-budget %2])}]
         [ui/text-field
          {:floating-label-text "Min. Number of Feedbacks"
           :type :number
           :default-value min-employer-ratings-count
           :full-width true
           :min 0
           :on-change #(dispatch [:form/search-jobs-changed :search/min-employer-ratings-count %2])}]
         [country-select-field
          {:value country
           :full-width true
           :on-new-request #(dispatch [:form/search-jobs-changed :search/country %2])}]
         [language-select-field
          {:value language
           :full-width true
           :on-new-request #(dispatch [:form/search-jobs-changed :search/language %2])}]
         #_[u/subheader "Min Budget"]
         #_[slider-with-counter
            {:max 200
             :step 5
             :value min-budget
             :on-change #(dispatch [:form/search-jobs-changed :search/min-budget %2])}
            (str min-budget " ETH")]

         ]))))

(defn search-results-employer [{:keys [:employer/jobs-count :employer/avg-rating :employer/total-paid
                                       :user/name :job/employer :employer/ratings-count :user/country]}]
  [:div {:style styles/employer-info-wrap}
   (when-not (empty? name)
     [row-plain
      {:middle "xs"
       :style styles/employer-info}
      [:span [a {:route :employer/detail
                 :route-params {:user/id employer}} name]]
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
      [misc/country-marker
       {:country country
        :row-props {:style styles/employer-info-item}}]])])


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
             :on-touch-tap #(dispatch [:form/search-jobs-changed :search/offset (- offset limit)])}])
         (when (= items-count limit)
           [ui/flat-button
            {:secondary true
             :label "Older"
             :label-position :before
             :icon (icons/navigation-chevron-right)
             :on-touch-tap #(dispatch [:form/search-jobs-changed :search/offset (+ offset limit)])}])]))))

(defn search-results []
  (let [list (subscribe [:list/search-jobs])]
    (fn []
      (let [{:keys [loading? items]} @list]
        [paper-thin
         {:loading? loading?}
         (if (seq items)
           [:div
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
                [:span " - Budget: " (u/eth budget)]
                #_(when (.greaterThan budget 0)
                    [:span " - Budget: " (.toNumber budget) " ETH"])]
               [:div {:style styles/job-list-description}
                [truncated-text
                 {:more-text-props {:style {:color styles/primary1-color}}}
                 description]]
               [skills-chips
                {:selected-skills skills}]
               [search-results-employer item]
               [misc/hr-small]])
            [pagination (count items)]]
           [row {:center "xs" :middle "xs"
                 :style {:min-height 200}}
            (when-not loading?
              [:h3 "No jobs match your search criteria :("])])]))))

(defn skills-input []
  (let [selected-skills (subscribe [:form/search-job-skills])]
    (fn []
      [paper-thin
       [skills-chip-input
        {:value @selected-skills
         :hint-text "Type skills required for a job"
         :on-change #(dispatch [:form/search-jobs-changed :search/skills %1])}]])))

(defn search-jobs-page []
  [misc/search-layout
   [filter-sidebar]
   [skills-input]
   [search-results]])
