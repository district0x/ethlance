(ns ethlance.ui.page.new-job
  (:require [district.ui.component.page :refer [page]]
            [ethlance.shared.constants :as constants]
            [ethlance.ui.component.button :refer [c-button c-button-label]]
            [ethlance.ui.component.icon :refer [c-icon]]
            [ethlance.ui.component.main-layout :refer [c-main-layout]]
            [ethlance.ui.component.profile-image :refer [c-profile-image]]
            [ethlance.ui.component.radio-select
             :refer
             [c-radio-secondary-element c-radio-select]]
            [ethlance.ui.component.rating :refer [c-rating]]
            [ethlance.ui.component.search-input :refer [c-chip-search-input]]
            [ethlance.ui.component.select-input :refer [c-select-input]]
            [ethlance.ui.component.text-input :refer [c-text-input]]
            [ethlance.ui.component.textarea-input :refer [c-textarea-input]]
            [re-frame.core :as re]))

(defn c-arbiter-for-hire
  []
  [:div.arbiter-for-hire
   [c-profile-image {}]
   [:div.name "Brian Curran"]
   [c-rating {:default-rating 3}]
   [:div.price "$10"]
   [c-button
    {:size :small}
    [c-button-label "Invite"]]])

(defmethod page :route.job/new []
  (let [*type (re/subscribe [:page.new-job/type])
        *name (re/subscribe [:page.new-job/name])
        *category (re/subscribe [:page.new-job/category])
        *bid-option (re/subscribe [:page.new-job/bid-option])
        *required-experience-level (re/subscribe [:page.new-job/required-experience-level])
        *estimated-project-length (re/subscribe [:page.new-job/estimated-project-length])
        *required-availability (re/subscribe [:page.new-job/required-availability])
        *required-skills (re/subscribe [:page.new-job/required-skills])
        *description (re/subscribe [:page.new-job/description])
        *form-of-payment (re/subscribe [:page.new-job/form-of-payment])
        *token-address (re/subscribe [:page.new-job/token-address])
        *with-arbiter? (re/subscribe [:page.new-job/with-arbiter?])]
    (fn []
      (let [is-bounty? (= @*type :bounty)
            with-token? (= @*form-of-payment :erc20)]
        [c-main-layout {:container-opts {:class :new-job-main-container}}
         [:div.forms-left
          [:div.title (if is-bounty? "New Bounty" "New Job")]
          [:div.job-type-input
           [c-radio-select
            {:selection @*type
             :on-selection #(re/dispatch [:page.new-job/set-type %])
             :flex? true}
            [:job [c-radio-secondary-element "Job"]]
            [:bounty [c-radio-secondary-element "Bounty"]]]]
          [:div.name-input
           [c-text-input
            {:placeholder "Name"
             :value @*name
             :on-change #(re/dispatch [:page.new-job/set-name %])}]]
          [:div.category-input
           [c-select-input
            {:label "Category"
             :selections (sort constants/categories)
             :selection @*category
             :on-select #(re/dispatch [:page.new-job/set-category %])}]]
          (when-not is-bounty?
            [:div.bid-for-radio-input.radio
             [:div.label "Candidates Should Bid For"]
             [c-radio-select
              {:selection @*bid-option
               :on-selection #(re/dispatch [:page.new-job/set-bid-option %])}
              [:hourly-rate [c-radio-secondary-element "Hourly Rate"]]
              [:fixed-price [c-radio-secondary-element "Fixed Price"]]]])

          (when-not is-bounty?
            [:div.experience-radio-input.radio
             [:div.label "Required Experience Level"]
             [c-radio-select
              {:selection @*required-experience-level
               :on-selection #(re/dispatch [:page.new-job/set-required-experience-level %])}
              [:beginner [c-radio-secondary-element "Beginner ($)"]]
              [:intermediate [c-radio-secondary-element "Intermediate ($$)"]]
              [:expert [c-radio-secondary-element "Expert ($$$)"]]]])

          (when-not is-bounty?
            [:div.project-length-radio-input.radio
             [:div.label "Estimated Project Length"]
             [c-radio-select
              {:selection @*estimated-project-length
               :on-selection #(re/dispatch [:page.new-job/set-estimated-project-length %])}
              [:day [c-radio-secondary-element "Hours or Days"]]
              [:week [c-radio-secondary-element "Weeks"]]
              [:month [c-radio-secondary-element "Months"]]
              [:year [c-radio-secondary-element ">6 Months"]]]])

          (when-not is-bounty?
            [:div.availability-radio-input.radio
             [:div.label "Required Availability"]
             [c-radio-select
              {:selection @*required-availability
               :on-selection #(re/dispatch [:page.new-job/set-required-availability %])}
              [:full-time [c-radio-secondary-element "Full-Time"]]
              [:part-time [c-radio-secondary-element "Part-Time"]]]])

          [:div.with-arbiter-radio-input.radio
           [:div.label "Arbiter"]
           [c-radio-select
            {:selection (if @*with-arbiter? :with-arbiter :no-arbiter)
             :on-selection #(re/dispatch [:page.new-job/set-with-arbiter? (= :with-arbiter %)])}
            [:with-arbiter [c-radio-secondary-element "With Arbiter"]]
            [:no-arbiter [c-radio-secondary-element "Without Arbiter"]]]]]

         [:div.forms-right
          [:div.required-skills-chip.chip
           [:div.label "Required Skills"]
           [c-chip-search-input
            {:search-icon? false
             :auto-suggestion-listing constants/skills
             :chip-listing @*required-skills
             :on-chip-listing-change #(re/dispatch [:page.new-job/set-required-skills %])
             :placeholder "Search Skills"
             :allow-custom-chips? false}]]

          [:div.description-text.chip
           [:div.label "Description"]
           [c-textarea-input
            {:placeholder "Enter Description"
             :value @*description
             :on-change #(re/dispatch [:page.new-job/set-description %])}]]

          [:div.forms-of-payment.chip
           [:div.label "Forms of Payment"]
           [c-radio-select
            {:selection @*form-of-payment
             :on-selection #(re/dispatch [:page.new-job/set-form-of-payment %])}
            [:ethereum [c-radio-secondary-element "Ether"]]
            [:erc20 [c-radio-secondary-element "Token (ERC-20)"]]]

           (when with-token?
             [:div.token-address-input
              [:div.input
               [c-text-input
                {:value @*token-address
                 :on-change #(re/dispatch [:page.new-job/set-token-address %])
                 :placeholder "Token Address"}]
               [:div.token-label "SNT"]]
              ;; TODO: retrieve token logo
              [:div.token-logo]])]]

         (when @*with-arbiter?
           [:div.arbiters
            [c-arbiter-for-hire]
            [c-arbiter-for-hire]
            [c-arbiter-for-hire]])

         [:div.button
          {:on-click (fn [])}
          [:div.label "Create"]
          [c-icon {:name :ic-arrow-right :size :small}]]]))))
