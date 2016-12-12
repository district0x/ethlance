(ns ethlance.pages.job-create-page
  (:require
    [cljs-react-material-ui.icons :as icons]
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.category-select-field :refer [category-select-field]]
    [ethlance.components.jobs-table :refer [jobs-table]]
    [ethlance.components.language-select-field :refer [language-select-field]]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a center-layout]]
    [ethlance.components.radio-group :refer [radio-group]]
    [ethlance.components.skills-chip-input :refer [skills-chip-input]]
    [ethlance.ethlance-db :as ethlance-db]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [re-frame.core :refer [subscribe dispatch]]
    [ethlance.constants :as constants]))

(defn payment-type-radio [{:keys [:value]}]
  [ui/radio-button-group
   {:name "payment-type"
    :default-selected value
    :on-change #(dispatch [:form/value-changed :form.job/add-job :job/payment-type %2])}
   (for [[key label] constants/payment-types]
     [ui/radio-button
      {:value key
       :key key
       :label label}])])

(defn add-job-form []
  (let [form (subscribe [:form.job/add-job])]
    (fn []
      (let [{:keys [:data :loading? :errors]} @form
            {:keys [:job/title :job/description :job/skills :job/language :job/budget
                    :job/category :job/payment-type :job/experience-level :job/estimated-duration :job/hours-per-week
                    :job/freelancers-needed]} data]
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
           {:value category
            :no-all-categories? true
            :on-change #(dispatch [:form/value-changed :form.job/add-job :job/category %3])}]]
         [:div
          [misc/ether-field
           {:floating-label-text "Your budget in Ether"
            :form-key :form.job/add-job
            :field-key :job/budget
            :default-value budget}]]
         [ui/text-field
          {:floating-label-text "Number of needed freelancers"
           :type :number
           :min 1
           :default-value freelancers-needed
           :on-change #(dispatch [:form/value-changed :form.job/add-job :job/freelancers-needed %2 pos?])}]
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
            :on-new-request #(dispatch [:form/value-changed :form.job/add-job :job/language %2])}]]
         [skills-chip-input
          {:value skills
           :hint-text "Type required skills for the job"
           :on-change #(dispatch [:form/value-changed :form.job/add-job :job/skills %1])}]
         [misc/textarea
          {:floating-label-text "Job description"
           :form-key :form.job/add-job
           :field-key :job/description
           :max-length-key :max-job-description
           :min-length-key :min-job-description
           :default-value description}]
         [misc/send-button
          {:disabled (or loading? (boolean (seq errors)))
           :on-touch-tap #(dispatch [:contract.invoice/add data])}]]))))

(defn job-create-page []
  [misc/employer-only-page
   [center-layout
    [paper
     [:h2 "New Job"]
     [add-job-form]]]])
