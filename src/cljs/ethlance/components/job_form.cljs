(ns ethlance.components.job-form
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [clojure.set :as set]
    [ethlance.components.addresses-chip-input :refer [addresses-chip-input]]
    [ethlance.components.category-select-field :refer [category-select-field]]
    [ethlance.components.currency-select-field :refer [currency-select-field]]
    [ethlance.components.icons :as icons]
    [ethlance.components.language-select-field :refer [language-select-field]]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a center-layout]]
    [ethlance.components.radio-group :refer [radio-group]]
    [ethlance.components.skills-chip-input :refer [skills-chip-input]]
    [ethlance.components.validated-chip-input :refer [validated-chip-input]]
    [ethlance.constants :as constants]
    [ethlance.ethlance-db :as ethlance-db]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [goog.string :as gstring]
    [re-frame.core :refer [subscribe dispatch]]))

(defn only-me-allowed? [my-address allowed-users]
  (= [my-address] allowed-users))

(defn job-form []
  (let [eth-config (subscribe [:eth/config])
        active-address (subscribe [:db/active-address])]
    (fn [{:keys [:data :form-key :errors :loading? :budget-enabled?]}]
      (let [{:keys [:job/title :job/description :job/skills :job/language :job/budget
                    :job/category :job/payment-type :job/experience-level :job/estimated-duration :job/hours-per-week
                    :job/freelancers-needed :job/reference-currency :job/sponsorable? :job/allowed-users]} data
            {:keys [:min-job-skills :max-job-skills]} @eth-config]
        [paper
         {:loading? loading?}
         [:h2 "New Job"]
         [:div
          [misc/text-field
           {:floating-label-text "Job Title"
            :form-key form-key
            :field-key :job/title
            :max-length-key :max-job-title
            :min-length-key :min-job-title
            :value title}]
          [:div
           [category-select-field
            {:value (when (pos? category) category)
             :no-all-categories? true
             :on-change #(dispatch [:form/set-value form-key :job/category %3])}]]
          [:div
           [misc/subheader "Candidates should bid for"]
           [radio-group
            {:name "payment-type"
             :form-key form-key
             :field-key :job/payment-type
             :default-selected payment-type
             :options constants/payment-types}]]
          [misc/subheader "Candidates should bid in currency"]
          [currency-select-field
           {:value reference-currency
            :style {:width 80}
            :on-change #(dispatch [:form/set-value form-key :job/reference-currency %3])}]
          [:div
           [ui/toggle
            {:label "Set a Budget"
             :label-position "right"
             :toggled budget-enabled?
             :on-toggle #(dispatch [:form.job.add-job/set-budget-enabled? form-key %2])}]
           [misc/ether-field-with-currency
            {:floating-label-text "Your budget"
             :form-key form-key
             :field-key :job/budget
             :value budget
             :disabled (not budget-enabled?)
             :currency reference-currency}]]
          [:div
           [misc/subheader "Required Experience Level"]
           [radio-group
            {:name "experience-level"
             :form-key form-key
             :field-key :job/experience-level
             :default-selected experience-level
             :options constants/experience-levels}]]
          [:div
           [misc/subheader "Estimated Project Length"]
           [radio-group
            {:name "estimated-duration"
             :form-key form-key
             :field-key :job/estimated-duration
             :default-selected estimated-duration
             :options constants/estimated-durations}]]
          [:div
           [misc/subheader "Required Availability"]
           [radio-group
            {:name "hours-per-week"
             :form-key form-key
             :field-key :job/hours-per-week
             :default-selected hours-per-week
             :options constants/hours-per-weeks}]]
          [ui/text-field
           {:floating-label-text "Number of needed freelancers"
            :type :number
            :min 1
            :value freelancers-needed
            :on-change #(dispatch [:form/set-value form-key :job/freelancers-needed %2 pos?])}]
          [:div
           [language-select-field
            {:value language
             :floating-label-text "Language of job description"
             :on-new-request #(dispatch [:form/set-value form-key :job/language %2])}]]
          (let [validator #(<= min-job-skills (count %) max-job-skills)]
            [:div
             [skills-chip-input
              {:value skills
               :hint-text "Type required skills for the job"
               :on-change #(dispatch [:form/set-value form-key :job/skills %1 validator])
               :error-text (when-not (validator skills)
                             (gstring/format "Choose from %s to %s skills" min-job-skills max-job-skills))}]])
          [misc/textarea
           {:floating-label-text "Job description"
            :form-key form-key
            :field-key :job/description
            :max-length-key :max-job-description
            :min-length-key :min-job-description
            :value description}]
          [ui/toggle
           {:label "Accept sponsorships for this job"
            :label-position "right"
            :toggled sponsorable?
            :style styles/margin-top-gutter-more
            :on-toggle #(dispatch [:form/set-value form-key :job/sponsorable? %2])}]
          (when sponsorable?
            [:div
             [ui/raised-button
              {:primary true
               :label "Add me"
               :icon (icons/plus)
               :style styles/margin-top-gutter-less
               :disabled (contains? (set allowed-users) @active-address)
               :on-touch-tap #(dispatch [:form.job.add-job/add-me-as-allowed form-key allowed-users])}]
             [addresses-chip-input
              {:value allowed-users
               :floating-label-text "Addresses allowed to spend sponsorships"
               :form-key form-key
               :field-key :job/allowed-users
               :min-length-key :min-job-allowed-users
               :max-length-key :max-job-allowed-users
               :chip-backgroud-color styles/allowed-user-not-approved-color
               :style styles/margin-top-gutter-more}]])
          [misc/send-button
           {:label (if (or (not sponsorable?)
                           (only-me-allowed? @active-address allowed-users))
                     "Publish"
                     "Save for Approval")
            :disabled (or loading? (cond-> errors
                                     (not sponsorable?) (set/difference #{:job/allowed-users})
                                     true (set/difference #{:invalid-address})
                                     true seq
                                     true boolean))
            :on-touch-tap #(dispatch [:contract.job/set-job (cond-> data
                                                              (not sponsorable?) (assoc :job/allowed-users []))])}]
          (when (and sponsorable?
                     (not (only-me-allowed? @active-address allowed-users)))
            [row-plain
             {:end "xs"
              :style styles/margin-top-gutter-less}
             [:small
              "Job will be automatically published after all allowed addresses approve it."]])]]))))
