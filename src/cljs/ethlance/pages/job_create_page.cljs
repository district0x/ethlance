(ns ethlance.pages.job-create-page
  (:require
    [cljs-react-material-ui.icons :as icons]
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.category-select-field :refer [category-select-field]]
    [ethlance.components.language-select-field :refer [language-select-field]]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a center-layout]]
    [ethlance.components.radio-group :refer [radio-group]]
    [ethlance.components.skills-chip-input :refer [skills-chip-input]]
    [ethlance.ethlance-db :as ethlance-db]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [goog.string :as gstring]
    [re-frame.core :refer [subscribe dispatch]]
    [ethlance.constants :as constants]))

(defn add-job-form []
  (let [form (subscribe [:form.job/add-job])
        eth-config (subscribe [:eth/config])]
    (fn []
      (let [{:keys [:data :loading? :errors]} @form
            {:keys [:job/title :job/description :job/skills :job/language :job/budget
                    :job/category :job/payment-type :job/experience-level :job/estimated-duration :job/hours-per-week
                    :job/freelancers-needed]} data
            {:keys [:min-job-skills :max-job-skills]} @eth-config]
        [paper
         {:loading? loading?}
         [:h2 "New Job"]
         [:div
          [misc/text-field
           {:floating-label-text "Job Title"
            :form-key :form.job/add-job
            :field-key :job/title
            :max-length-key :max-job-title
            :min-length-key :min-job-title
            :default-value title}]
          [:div
           [category-select-field
            {:value (when (pos? category) category)
             :no-all-categories? true
             :on-change #(dispatch [:form/set-value :form.job/add-job :job/category %3])}]]
          [:div
           [misc/ether-field
            {:floating-label-text "Your budget (Ether)"
             :form-key :form.job/add-job
             :field-key :job/budget
             :value budget}]]
          [ui/text-field
           {:floating-label-text "Number of needed freelancers"
            :type :number
            :min 1
            :default-value freelancers-needed
            :on-change #(dispatch [:form/set-value :form.job/add-job :job/freelancers-needed %2 pos?])}]
          [:div
           [misc/subheader "Payment Type"]
           [radio-group
            {:name "payment-type"
             :form-key :form.job/add-job
             :field-key :job/payment-type
             :default-selected payment-type
             :options constants/payment-types}]]
          [:div
           [misc/subheader "Required Experience Level"]
           [radio-group
            {:name "experience-level"
             :form-key :form.job/add-job
             :field-key :job/experience-level
             :default-selected experience-level
             :options constants/experience-levels}]]
          [:div
           [misc/subheader "Estimated Project Length"]
           [radio-group
            {:name "estimated-duration"
             :form-key :form.job/add-job
             :field-key :job/estimated-duration
             :default-selected estimated-duration
             :options constants/estimated-durations}]]
          [:div
           [misc/subheader "Required Availability"]
           [radio-group
            {:name "hours-per-week"
             :form-key :form.job/add-job
             :field-key :job/hours-per-week
             :default-selected hours-per-week
             :options constants/hours-per-weeks}]]
          [:div
           [language-select-field
            {:value language
             :floating-label-text "Language of job description"
             :on-new-request #(dispatch [:form/set-value :form.job/add-job :job/language %2])}]]
          (let [validator #(<= min-job-skills (count %) max-job-skills)]
            [:div
             [skills-chip-input
              {:value skills
               :hint-text "Type required skills for the job"
               :on-change #(dispatch [:form/set-value :form.job/add-job :job/skills %1 validator])
               :error-text (when-not (validator skills)
                             (gstring/format "Choose from %s to %s skills" min-job-skills max-job-skills))}]
             [row {:end "xs"}
              [ui/raised-button
               {:label "Add more skills"
                :primary true
                :href (u/path-for :skills/create)
                :icon (icons/content-add)}]]])
          [misc/textarea
           {:floating-label-text "Job description"
            :form-key :form.job/add-job
            :field-key :job/description
            :max-length-key :max-job-description
            :min-length-key :min-job-description
            :default-value description}]
          [misc/send-button
           {:disabled (or loading? (boolean (seq errors)))
            :on-touch-tap #(dispatch [:contract.job/add-job data])}]]]))))

(defn job-create-page []
  [misc/only-registered
   [misc/only-employer
    [center-layout
     [add-job-form]]]])
