(ns ethlance.components.user-forms
  (:require
    [cljs-react-material-ui.icons :as icons]
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.chip-input :refer [chip-input]]
    [ethlance.components.country-select-field :refer [country-select-field]]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a center-layout]]
    [ethlance.components.radio-group :refer [radio-group]]
    [ethlance.components.skills-chip-input :refer [skills-chip-input]]
    [ethlance.constants :as constants]
    [ethlance.ethlance-db :as ethlance-db]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [goog.string :as gstring]
    [re-frame.core :refer [subscribe dispatch]]
    ))

(defn not-open-content [{:keys [:button-label :text :form-key]}]
  [row
   {:center "xs"
    :style styles/margin-top-gutter}
   [col {:xs 12 :style styles/margin-bottom-gutter-less} text]
   [ui/raised-button
    {:label button-label
     :primary true
     :on-touch-tap #(dispatch [:form/set-open? form-key true])}]])

(defn user-form [{:keys [:user :form-key :show-save-button? :errors :loading?] :as props}]
  (let [{:keys [:user/name :user/gravatar :user/languages :user/country]} user]
    [:div
     (dissoc props :user :form-key :show-save-button? :errors :loading? :open?)
     [:div
      [misc/text-field
       {:floating-label-text "Your Name"
        :form-key form-key
        :field-key :user/name
        :max-length-key :max-user-name
        :min-length-key :min-user-name
        :value name}]]
     [:div
      [misc/text-field
       {:floating-label-text "Gravatar"
        :form-key form-key
        :field-key :user/gravatar
        :value gravatar}]]
     [country-select-field
      {:value country
       :on-change #(dispatch [:form/set-value form-key :user/country %3 pos?])}]
     [chip-input
      {:value languages
       :all-items constants/languages
       :floating-label-text "Languages"
       :on-change #(dispatch [:form/set-value form-key :user/languages %1 seq])}]
     (when show-save-button?
       [misc/send-button
        {:label "Save User"
         :icon (icons/content-save)
         :disabled (or loading? (boolean (seq errors)))
         :on-touch-tap #(dispatch [:contract.user/set-user user])}])]))

(defn employer-form [{:keys [:user :form-key :open? :show-save-button? :errors :loading?]}]
  (let [{:keys [:employer/description]} user]
    (if open?
      [:div
       [misc/textarea
        {:floating-label-text "Overview"
         :hint-text "Describe youself as employer"
         :form-key form-key
         :field-key :employer/description
         :max-length-key :max-user-description
         :value (:employer/description user)}]
       (when show-save-button?
         [misc/send-button
          {:label "Save Employer"
           :icon (icons/content-save)
           :disabled (or loading? (boolean (seq errors)))
           :on-touch-tap #(dispatch [:contract.user/set-employer user])}])]
      [not-open-content
       {:form-key form-key
        :text "You are not yet registered as an employer"
        :button-label "Become Employer"}])))

(defn freelancer-form [{:keys [:user :form-key :open? :show-save-button? :errors :loading?]}]
  (let [{:keys [:freelancer/description :freelancer/job-title :freelancer/hourly-rate :freelancer/categories
                :freelancer/skills :freelancer/available?]} user]
    (if open?
      [:div
       [misc/text-field
        {:floating-label-text "Job Title"
         :form-key form-key
         :field-key :freelancer/job-title
         :max-length-key :max-freelancer-job-title
         :min-length-key :min-freelancer-job-title
         :value job-title}]
       [misc/ether-field
        {:floating-label-text "Hourly rate"
         :form-key form-key
         :field-key :freelancer/hourly-rate
         :value hourly-rate}]
       [chip-input
        {:value categories
         :all-items (rest (vals constants/categories))
         :floating-label-text "Categories"
         :on-change #(dispatch [:form/set-value form-key :freelancer/categories %1 seq])}]
       [skills-chip-input
        {:value skills
         :hint-text "Type skills you have"
         :on-change #(dispatch [:form/set-value form-key :freelancer/skills %1 seq])}]
       [misc/textarea
        {:floating-label-text "Overview"
         :hint-text "Describe youself as freelancer"
         :form-key form-key
         :field-key :freelancer/description
         :max-length-key :max-user-description
         :value (:freelancer/description user)}]
       [ui/checkbox
        {:style styles/margin-top-gutter-less
         :label "I'm available for hire"
         :checked available?
         :on-check #(dispatch [:form/set-value form-key :freelancer/available? %2])}]
       (when show-save-button?
         [misc/send-button
          {:label "Save Freelancer"
           :icon (icons/content-save)
           :disabled (or loading? (boolean (seq errors)))
           :on-touch-tap #(dispatch [:contract.user/set-freelancer user])}])]
      [not-open-content
       {:form-key form-key
        :text "You are not yet registered as a freelancer"
        :button-label "Become Freelancer"}])))