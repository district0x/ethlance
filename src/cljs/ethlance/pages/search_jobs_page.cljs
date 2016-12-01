(ns ethlance.pages.search-jobs-page
  (:require
    [cljs-react-material-ui.icons :as icons]
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.category-select-field :refer [category-select-field]]
    [ethlance.components.country-select-field :refer [country-select-field]]
    [ethlance.components.language-select-field :refer [language-select-field]]
    [ethlance.components.slider-with-counter :refer [slider-with-counter]]
    [ethlance.components.truncated-text :refer [truncated-text]]
    [ethlance.components.checkbox-group :refer [checkbox-group]]
    [ethlance.components.layout :refer [col row paper]]
    [ethlance.components.skills-chips :refer [skills-chips]]
    [ethlance.components.star-rating :refer [star-rating]]
    [ethlance.components.chip-input :refer [chip-input]]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(def payment-types-opts
  {1 "Hourly"
   2 "Fixed"})

(def experience-levels-opts
  {1 "Beginner ($)"
   2 "Intermediate ($$)"
   3 "Expert ($$$)"})

(def estimated-durations-opts
  {1 "Hours or Days"
   2 "Weeks"
   3 "Months"
   4 "> 6 months"})

(def hours-per-weeks-opts
  {1 "Part Time"
   2 "Full Time"})

(defn filter-sidebar []
  (let [form-data (subscribe [:form/search-job])]
    (dispatch [:contract.search/search-jobs-initiate @form-data])
    (fn []
      (let [{:keys [:search/category :search/min-employer-avg-rating :search/country
                    :search/language :search/experience-levels :search/payment-types
                    :search/estimated-durations :search/hours-per-weeks :search/min-budget]} @form-data]
        [paper
         [category-select-field
          {:value category
           :full-width true
           :on-change #(dispatch [:form/search-job-changed :search/category %3])}]
         [u/subheader "Min. Employer Rating"]
         [star-rating {:value min-employer-avg-rating
                       :on-star-click #(dispatch [:form/search-job-changed :search/min-employer-avg-rating %1])}]
         [u/subheader "Payment Type"]
         [checkbox-group
          {:options payment-types-opts
           :values payment-types
           :on-change #(dispatch [:form/search-job-changed :search/payment-types %2])}]
         [u/subheader "Experience Level"]
         [checkbox-group
          {:options experience-levels-opts
           :values experience-levels
           :on-change #(dispatch [:form/search-job-changed :search/experience-levels %2])}]
         [u/subheader "Project Length"]
         [checkbox-group
          {:options estimated-durations-opts
           :values estimated-durations
           :on-change #(dispatch [:form/search-job-changed :search/estimated-durations %2])}]
         [u/subheader "Availability"]
         [checkbox-group
          {:options hours-per-weeks-opts
           :values hours-per-weeks
           :on-change #(dispatch [:form/search-job-changed :search/hours-per-weeks %2])}]
         [country-select-field
          {:value country
           :full-width true
           :on-new-request #(dispatch [:form/search-job-changed :search/country %2])}]
         [language-select-field
          {:value language
           :full-width true
           :on-new-request #(dispatch [:form/search-job-changed :search/language %2])}]
         [u/subheader "Min Budget"]
         [slider-with-counter
          {:max 200
           :step 5
           :value min-budget
           :on-change #(dispatch [:form/search-job-changed :search/min-budget %2])}
          (str min-budget " ETH")]

         ]))))

(defn search-results []
  (let [list-search-jobs (subscribe [:list/search-job])]
    (fn []
      (let [{:keys [items loading?]} @list-search-jobs]
        [paper
         {:loading? loading?}
         (if (seq items)
           (for [{:keys [:job/title :job/id :job/payment-type :job/estimated-duration
                         :job/experience-level :job/hours-per-week :job/created-on
                         :job/description :job/budget :job/skills]} items]
             [:div {:key id}
              [:h2
               (u/anchor {:style styles/job-title} title :job/detail {:job/id id})]
              [:div {:style styles/job-info}
               [:span (u/time-ago created-on)]
               [:span " - " (payment-types-opts payment-type)]
               [:span " - " (experience-levels-opts experience-level)]
               [:span " - Est. Time: " (estimated-durations-opts estimated-duration)]
               [:span " - " (hours-per-weeks-opts hours-per-week)]
               (when (.greaterThan budget 0)
                 [:span " - Budget: " (.toNumber budget) " ETH"])]
              [:div {:style styles/job-description}
               [truncated-text description]]
              [skills-chips skills]
              [:hr]])
           [row {:center "xs" :middle "xs"
                 :style {:min-height 200}}
            (when-not loading?
              [:h3 "No jobs match your search criteria :("])])]))))

(defn skills-input []
  (let [skills (subscribe [:app/skills])
        selected-skills (subscribe [:form/search-job-skills])]
    (fn []
      (let [skills-data-source (u/create-data-source @skills :skill/name)
            selected-skills-ds (u/create-data-source (select-keys @skills @selected-skills) :skill/name)]
        [paper
         [chip-input
          {:default-value selected-skills-ds
           :dataSource skills-data-source
           :dataSourceConfig u/data-source-config
           :new-chip-key-codes []
           :full-width true
           :floating-label-text "Skills"
           :max-search-results 10
           :hint-text "Type skills required for a job"
           :on-change #(dispatch [:form/search-job-changed :search/skills (u/data-source-values %1)])}]]))))

(defn search-jobs-page []
  (fn []
    [row
     [col {:xs 4}
      [filter-sidebar]]
     [col {:xs 8}
      [row
       [col {:xs 12}
        [skills-input]]
       [col {:xs 12}
        [search-results]]]]]))
