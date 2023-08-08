(ns ethlance.ui.page.new-job
  (:require [district.ui.component.page :refer [page]]
            [ethlance.shared.constants :as constants]
            [ethlance.ui.component.button :refer [c-button c-button-label]]
            [ethlance.ui.component.icon :refer [c-icon]]
            [ethlance.ui.component.main-layout :refer [c-main-layout]]
            [ethlance.ui.component.profile-image :refer [c-profile-image]]
            [district.ui.graphql.subs :as gql]
            [district.ui.web3-tx.events :as tx-events]
            [district.ui.smart-contracts.queries :as contract-queries]
            [ethlance.ui.page.new-job.events :as new-job.events]
            [ethlance.ui.component.radio-select
             :refer
             [c-radio-secondary-element c-radio-select]]
            [ethlance.ui.component.rating :refer [c-rating]]
            [ethlance.ui.component.search-input :refer [c-chip-search-input]]
            [ethlance.ui.component.select-input :refer [c-select-input]]
            [ethlance.ui.component.text-input :refer [c-text-input]]
            [ethlance.ui.component.textarea-input :refer [c-textarea-input]]
            [ethlance.ui.util.component :refer [<sub >evt]]
            [ethlance.ui.subscriptions :as subs]
            [ethlance.ui.util.navigation :as navigation]
            [ethlance.ui.util.job :as util.job]
            [re-frame.core :as re]
            [clojure.spec.alpha :as s]))

(defn c-arbiter-for-hire
  [arbiter]
  (let [invited-arbiter (re/subscribe [:page.new-job/invited-arbiter])]
    [:div.arbiter-for-hire
     [c-profile-image {:src (-> arbiter :user :user/profile-image)}]
     [:div.name (-> arbiter :user :user/name)]
     [c-rating {:rating (-> arbiter :arbiter/rating) :default-rating nil}]
     [:div.price "$" (-> arbiter :arbiter/fee)]
     (if-not (= @invited-arbiter (:user/id arbiter))
       [c-button
        {:size :small
         :on-click #(re/dispatch [:page.new-job/invite-arbiter (:user/id arbiter)])}
        [c-button-label "Invite"]]

       [c-button
        {:size :small
         :color :warning
         :on-click #(re/dispatch [:page.new-job/invite-arbiter nil])}
        [c-button-label "Uninvite"]])]))

(defn- c-submit-button [{:keys [:on-submit :disabled?]}]
  [:div.form-submit
   {:class (when disabled? "disabled")
    :on-click (fn [] (when-not disabled? (>evt on-submit)))}
   [:span "Create"]
   [c-icon {:name :ic-arrow-right :size :smaller}]])

(defn radio-options-from-vector [options]
  (map (fn [[kw desc]] [kw [c-radio-secondary-element desc]]) options))

(defn c-job-creation-form []
  (let [arbiters-query [:arbiter-search {:limit 1000}
                        [[:items [:user/id
                                  [:user [:user/id
                                          :user/name
                                          :user/profile-image]]
                                   :arbiter/bio
                                   :arbiter/professional-title
                                   :arbiter/rating
                                   :arbiter/fee
                                   :arbiter/fee-currency-id]]]]
        arbiters-result (re/subscribe [::gql/query {:queries [arbiters-query]}])
        *bid-option (re/subscribe [:page.new-job/bid-option])
        *category (re/subscribe [:page.new-job/category])
        *description (re/subscribe [:page.new-job/description])
        *estimated-project-length (re/subscribe [:page.new-job/estimated-project-length])
        *name (re/subscribe [:page.new-job/title])
        *required-availability (re/subscribe [:page.new-job/required-availability])
        *required-experience-level (re/subscribe [:page.new-job/required-experience-level])
        *required-skills (re/subscribe [:page.new-job/required-skills])
        *with-arbiter? (re/subscribe [:page.new-job/with-arbiter?])
        form-values (re/subscribe [:page.new-job/form])

        *token-type (re/subscribe [:page.new-job/token-type])
        *token-amount (re/subscribe [:page.new-job/token-amount])
        *token-address (re/subscribe [:page.new-job/token-address])
        *token-id (re/subscribe [:page.new-job/token-id])]
    (fn []
      (let [arbiters (get-in @arbiters-result [:arbiter-search :items])
            with-token? (#{:erc20 :erc721 :erc1155} @*token-type)
            with-nft? (#{:erc721} @*token-type)
            token-with-amount? (#{:erc20 :erc1155 :eth} @*token-type)
            token-with-id? (#{:erc721 :erc1155} @*token-type)]
        [c-main-layout {:container-opts {:class :new-job-main-container}}
         [:div.forms-left
          [:div.title "New job"]
          [:div.name-input
           [c-text-input
            {:placeholder "Name"
             :value @*name
             :on-change #(re/dispatch [:page.new-job/set-title %])}]]
          [:div.category-input
           [c-select-input
            {:label "Category"
             :selections (sort constants/categories)
             :selection @*category
             :on-select #(re/dispatch [:page.new-job/set-category %])}]]
          [:div.bid-for-radio-input.radio
           [:div.label "Candidates Should Bid For"]
           (into [c-radio-select
                  {:selection @*bid-option
                   :on-selection #(re/dispatch [:page.new-job/set-bid-option %])}]
                 (radio-options-from-vector util.job/bid-option))]

          [:div.experience-radio-input.radio
           [:div.label "Required Experience Level"]
           (into [c-radio-select
                  {:selection @*required-experience-level
                   :on-selection #(re/dispatch [:page.new-job/set-required-experience-level %])}]
                 (radio-options-from-vector util.job/experience-level))]

          [:div.project-length-radio-input.radio
           [:div.label "Estimated Project Length"]
           (into [c-radio-select
                  {:selection @*estimated-project-length
                   :on-selection #(re/dispatch [:page.new-job/set-estimated-project-length %])}]
                 (radio-options-from-vector util.job/estimated-durations))]

          [:div.availability-radio-input.radio
           [:div.label "Required Availability"]
           (into [c-radio-select
                  {:selection @*required-availability
                   :on-selection #(re/dispatch [:page.new-job/set-required-availability %])}]
                 (radio-options-from-vector util.job/required-availability))]

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
             :allow-custom-chips? true}]]

          [:div.description-text.chip
           [:div.label "Description"]
           [c-textarea-input
            {:placeholder "Enter Description"
             :value @*description
             :on-change #(re/dispatch [:page.new-job/set-description %])}]]

          [:div.forms-of-payment.chip
           [:div.label "Forms of Payment"]
           [c-radio-select
            {:selection @*token-type
             :on-selection #(re/dispatch [:page.new-job/set-token-type %])}
            [:eth [c-radio-secondary-element "Ether"]]
            [:erc20 [c-radio-secondary-element "Token ERC-20"]]
            [:erc721 [c-radio-secondary-element "NFT Token (ERC-721)"]]
            [:erc1155 [c-radio-secondary-element "Multi-Token (ERC-1155)"]]]
           (when token-with-amount?
             [:div.token-address-input
              [c-text-input
               {:value @*token-amount
                :on-change #(re/dispatch [:page.new-job/set-token-amount %])
                :placeholder "Token Amount"}]
              [:div.token-label "(amount)"]])
           (when with-token?
             [:div.token-address-input
              [:div.input
               [c-text-input
                {:value @*token-address
                 :on-change #(re/dispatch [:page.new-job/set-token-address %])
                 :placeholder "Token Address"}]
               [:div.token-label "(address)"]]
              ;; TODO: retrieve token logo
              [:div.token-logo]])
           (when token-with-id?
             [:div.token-address-input
              [:div.input
               [c-text-input
                {:value @*token-id
                 :type :number
                 :on-change #(re/dispatch [:page.new-job/set-token-id %])
                 :placeholder "Token ID"}]
               [:div.token-label "(token ID)"]]])]]

         (when @*with-arbiter?
           [:div.arbiters
            (for [arbiter arbiters]
              ^{:key (str "arbiter-" (:user/id arbiter))}
              [c-arbiter-for-hire arbiter])])

         [c-button
          {:on-click (fn [] (>evt [:page.new-job/create]))
           :disabled? (not (s/valid? :page.new-job/create @form-values))}
          [:div.label "Create"]
          [c-icon {:name :ic-arrow-right :size :small}]]]))))

(defn c-invite-to-create-employer-profile [user-id]
  [c-main-layout {:container-opts {:class :new-job-main-container}}
   [:div "Set up your employer profile to be able to create new jobs"]
   [:div.button
    {:on-click (navigation/create-handler {:route :route.me/sign-up :query {:tab "employer"}})}
    "Go to employer profile page"]])

(defmethod page :route.job/new []
  (let [active-user (:user/id @(re/subscribe [:ethlance.ui.subscriptions/active-session]))
        query [:employer {:user/id active-user}
               [:employer/bio]]
        result @(re/subscribe [::gql/query {:queries [query]}])
        has-employer-profile? (not (nil? (get-in result [:employer :employer/bio])))]
    (if has-employer-profile?
      [c-job-creation-form]
      [c-invite-to-create-employer-profile])))
