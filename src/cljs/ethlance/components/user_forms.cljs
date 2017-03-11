(ns ethlance.components.user-forms
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.chip-input :refer [chip-input]]
    [ethlance.components.country-select-field :refer [country-select-field]]
    [ethlance.components.currency-select-field :refer [currency-select-field]]
    [ethlance.components.icons :as icons]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a center-layout]]
    [ethlance.components.radio-group :refer [radio-group]]
    [ethlance.components.skills-chip-input :refer [skills-chip-input]]
    [ethlance.components.state-select-field :refer [state-select-field]]
    [ethlance.components.validated-chip-input :refer [validated-chip-input]]
    [ethlance.constants :as constants]
    [ethlance.ethlance-db :as ethlance-db]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [goog.string :as gstring]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn not-open-content [{:keys [:button-label :text :form-key]}]
  [row
   {:center "xs"
    :style styles/margin-top-gutter}
   [col {:xs 12 :style styles/margin-bottom-gutter-less} text]
   [ui/raised-button
    {:label button-label
     :primary true
     :on-touch-tap #(dispatch [:form/set-open? form-key true])}]])

(defn github-text-field [{:keys [:form-key]} {:keys [:user/github]}]
  (if-not constants/mist?
    [misc/connect-button-layout
     {:connected? (seq github)
      :connect-button-props
      {:icon (icons/github)
       :on-touch-tap
       (partial u/get-my-github-username #(dispatch [:form/set-value form-key :user/github %1]))}
      :clear-button-props {:on-touch-tap #(dispatch [:form/set-value form-key :user/github ""])}}
     [misc/form-text-field
      {:floating-label-text "Your Github profile"
       :form-key form-key
       :disabled true
       :field-key :user/github
       :floating-label-fixed true
       :value github}]]
    [misc/form-text-field
     {:floating-label-text "Your Github username"
      :form-key form-key
      :field-key :user/github
      :value github}]))

(defn linkedin-text-field []
  (let [invalid-url? (r/atom false)]
    (fn [{:keys [:form-key]} {:keys [:user/linkedin]}]
      (if-not constants/mist?
        [misc/connect-button-layout
         {:connected? (seq linkedin)
          :connect-button-props
          {:icon (icons/linkedin)
           :on-touch-tap
           (partial u/get-my-linkedin-profile-url #(dispatch [:form/set-value form-key :user/linkedin %1]))}
          :clear-button-props {:on-touch-tap #(dispatch [:form/set-value form-key :user/linkedin ""])}}
         [misc/form-text-field
          {:floating-label-text "Your LinkedIn profile"
           :form-key form-key
           :disabled true
           :field-key :user/linkedin
           :floating-label-fixed true
           :value linkedin}]]
        [misc/text-field-base
         {:floating-label-text "Your LinkedIn profile"
          :floating-label-fixed true
          :hint-text (when (empty? linkedin) "Paste LinkedIn profile URL here")
          :value linkedin
          :error-text (when @invalid-url? "Oops, it doesn't look like LinkedIn URL")
          :on-change (fn [_ val]
                       (if (or (empty? val) (u/linkedin-url? val))
                         (do
                           (reset! invalid-url? false)
                           (dispatch [:form/set-value form-key :user/linkedin (u/parse-linkedin-url-id val)]))
                         (reset! invalid-url? true)))}]))))

(defn user-form []
  (let [xs-width? (subscribe [:window/xs-width?])]
    (fn [{:keys [:user :form-key :show-save-button? :errors :loading?] :as props}]
      (let [{:keys [:user/name :user/gravatar :user/languages :user/country :user/state :user/email
                    :user/github :user/linkedin]} user]
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
         [row-plain
          {:bottom "xs"}
          [misc/text-field-base
           {:floating-label-text "Your Email"
            :value email
            :style (if @xs-width? {:width 220} {})
            :on-change #(dispatch [:form.user/set-email form-key %2 u/empty-or-valid-email?])
            :error-text (when-not (u/empty-or-valid-email? email)
                          "Invalid email address")}]
          [ui/icon-button
           {:tooltip "Gravatar is an avatar calculated from your email address. Click for more"
            :href "http://gravatar.com/"
            :target :_blank}
           (icons/help-circle-outline {:color styles/fade-color})]]
         [row-plain
          {:style styles/margin-top-gutter}
          [ui/avatar
           {:size 100
            :src (u/gravatar-url gravatar)}]]
         [:div
          [country-select-field
           {:value country
            :on-change #(dispatch [:form/set-value form-key :user/country %3 pos?])}]]
         (when (u/united-states? country)
           [state-select-field
            {:value state
             :on-change #(dispatch [:form/set-value form-key :user/state %3])}])
         #_ [github-text-field props user]
         #_ [linkedin-text-field props user]
         [validated-chip-input
          {:all-items constants/languages
           :value languages
           :floating-label-text "Languages you speak"
           :form-key form-key
           :field-key :user/languages
           :min-length-key :min-user-languages
           :max-length-key :max-user-languages
           :chip-backgroud-color styles/languages-chip-color
           :menu-props {:max-height 200}
           :max-search-results 10}]
         (when show-save-button?
           [misc/send-button
            {:label "Save User"
             :disabled (or loading? (boolean (seq errors)))
             :on-touch-tap #(dispatch [:contract.user/set-user user])}])]))))

(defn employer-form [{:keys [:user :form-key :open? :show-save-button? :errors :loading?]}]
  (let [{:keys [:employer/description]} user]
    (if open?
      [:div
       [misc/textarea
        {:floating-label-text "Overview"
         :hint-text "Introduce youself as an employer, your previous experiences, contact information"
         :form-key form-key
         :field-key :employer/description
         :max-length-key :max-user-description
         :value (:employer/description user)}]
       (when show-save-button?
         [misc/send-button
          {:label "Save Employer"
           :disabled (or loading? (boolean (seq errors)))
           :on-touch-tap #(dispatch [:contract.user/set-employer user])}])]
      [not-open-content
       {:form-key form-key
        :text "You are not yet registered as an employer"
        :button-label "Become Employer"}])))

(defn freelancer-form [{:keys [:user :form-key :open? :show-save-button? :errors :loading? :show-add-more-skills?]}]
  (let [{:keys [:freelancer/description :freelancer/job-title :freelancer/hourly-rate :freelancer/hourly-rate-currency
                :freelancer/categories :freelancer/skills :freelancer/available?]} user]
    (if open?
      [:div
       [misc/text-field
        {:floating-label-text "Job Title"
         :form-key form-key
         :field-key :freelancer/job-title
         :max-length-key :max-freelancer-job-title
         :min-length-key :min-freelancer-job-title
         :full-width true
         :value job-title}]
       [misc/ether-field-with-currency-select-field
        {:ether-field-props
         {:floating-label-text "Hourly rate"
          :form-key form-key
          :field-key :freelancer/hourly-rate
          :value hourly-rate}
         :currency-select-field-props
         {:value hourly-rate-currency
          :on-change #(dispatch [:form/set-value form-key :freelancer/hourly-rate-currency %3])}}]
       [validated-chip-input
        {:all-items (rest (vals constants/categories))
         :value categories
         :floating-label-text "Categories you are interested in"
         :open-on-focus true
         :max-search-results (count constants/categories)
         :form-key form-key
         :field-key :freelancer/categories
         :min-length-key :min-freelancer-categories
         :max-length-key :max-freelancer-categories
         :chip-backgroud-color styles/categories-chip-color
         :menu-props styles/chip-input-menu-props}]
       [skills-chip-input
        {:value skills
         :hint-text "Type skills you have"
         :validated? true
         :form-key form-key
         :field-key :freelancer/skills
         :min-length-key :min-freelancer-skills
         :max-length-key :max-freelancer-skills}]
       #_(when show-add-more-skills?
           [row-plain {:end "xs"}
            [misc/add-more-skills-button]])
       [misc/textarea
        {:floating-label-text "Overview"
         :hint-text "Introduce youself as a freelancer, your working experiences, portfolio, contact information"
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
           :disabled (or loading? (boolean (seq errors)))
           :on-touch-tap #(dispatch [:contract.user/set-freelancer user])}])]
      [not-open-content
       {:form-key form-key
        :text "You are not yet registered as a freelancer"
        :button-label "Become Freelancer"}])))