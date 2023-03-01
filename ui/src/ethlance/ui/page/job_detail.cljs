(ns ethlance.ui.page.job-detail
  (:require [district.ui.component.page :refer [page]]
            [ethlance.ui.component.button :refer [c-button c-button-label]]
            [ethlance.ui.component.carousel
             :refer
             [c-carousel c-carousel-old c-feedback-slide]]
            [ethlance.ui.component.scrollable :refer [c-scrollable]]
            [ethlance.ui.component.circle-button :refer [c-circle-icon-button]]
            [ethlance.ui.component.main-layout :refer [c-main-layout]]
            [ethlance.ui.component.profile-image :refer [c-profile-image]]
            [ethlance.ui.component.rating :refer [c-rating]]
            [ethlance.ui.component.select-input :refer [c-select-input]]
            [ethlance.ui.component.table :refer [c-table]]
            [ethlance.ui.component.tag :refer [c-tag c-tag-label]]
            [ethlance.ui.component.text-input :refer [c-text-input]]
            [ethlance.ui.component.textarea-input :refer [c-textarea-input]]
            [district.ui.graphql.subs :as gql]
            [ethlance.ui.util.component :refer [<sub >evt]]
            [ethlance.shared.utils :refer [millis->relative-time]]
            [ethlance.shared.utils :as shared-utils]
            [re-frame.core :as re]))

(defn c-token-values [opts]
  (fn [{:keys [token-type token-amount token-address token-id disabled?]}]
    (let [token-type (keyword token-type)
          step (if (= token-type :eth) 0.001 1)]
      (cond
        (= :erc721 token-type)
        [:div "The payment will be NFT (ERC721)"]

       (#{:eth :erc1155 :erc20} token-type)
        [:div.amount-input
         [c-text-input
          {:placeholder "Token amount"
           :step step
           :type :number
           :default-value nil
           :disabled disabled?
           :value @token-amount
           :on-change #(re/dispatch [:page.job-detail/set-proposal-token-amount (js/parseFloat %)])}]]))))

(defmethod page :route.job/detail []
  (fn []
    (let [page-params (re/subscribe [:district.ui.router.subs/active-page-params])
          contract-address (:contract @page-params)
          job-query "query ($contract: ID!) {
                   job(contract: $contract) {
                     job_id
                     job_contract
                     job_title
                     job_description
                     job_requiredSkills
                     job_category
                     job_status
                     job_requiredExperienceLevel
                     job_requiredAvailability
                     job_bidOption
                     job_estimatedProjectLength

                     job_tokenType
                     job_tokenAmount
                     job_tokenAddress
                     job_tokenId

                     job_employer(contract: $contract) {
                       employer_rating
                       user_address
                       user {user_country user_name user_profileImage}
                     }
                     job_arbiter(contract: $contract) {
                       arbiter_rating
                       arbiter_fee
                       arbiter_feeCurrencyId
                       user_address
                       user {user_country user_name user_profileImage}
                     }
                  }
                }
                "
          query-results (re/subscribe [::gql/query job-query {:variables {:contract contract-address}}])
          results (:job @query-results)

          *title (:job/title results)
          *description (:job/description results)
          *sub-title (:job/category results)
          *experience (:job/required-experience-level results)
          *posted-time "Posted 7 Days Ago"
          *job-info-tags (remove nil? [(:job/estimated-project-length results)
                           (:job/status results)
                           (:job/required-experience-level results)
                           (:job/bid-option results)])
          *required-skills (:job/required-skills results)

          *employer-name (get-in results [:job/employer :user :user/name])
          *employer-rating (get-in results [:job/employer :employer/rating])
          *employer-country (get-in results [:job/employer :user :user/country])
          *employer-profile-image (get-in results [:job/employer :user :user/profile-image])

          *arbiter-name (get-in results [:job/arbiter :user :user/name])
          *arbiter-rating (get-in results [:job/arbiter :arbiter/rating])
          *arbiter-country (get-in results [:job/arbiter :user :user/country])
          *arbiter-profile-image (get-in results [:job/arbiter :user :user/profile-image])
          *arbiter-fee (get-in results [:job/arbiter :arbiter/fee])
          *arbiter-fee-currency (-> (get-in results [:job/arbiter :arbiter/fee-currency-id] "")
                                    name
                                    clojure.string/upper-case)

          raw-token-amount (get-in results [:job/token-amount])
          *job-token-type (get-in results [:job/token-type])
          *job-token-id (get-in results [:job/token-id])
          *job-token-address (get-in results [:job/token-address])
          *job-token-amount (if (= (str *job-token-type) "eth")
                              (shared-utils/wei->eth raw-token-amount)
                              raw-token-amount)

          *proposal-token-amount (re/subscribe [:page.job-detail/proposal-token-amount])
          *proposal-text (re/subscribe [:page.job-detail/proposal-text])

          proposals (re/subscribe [:page.job-detail/active-proposals])
          my-proposal (re/subscribe [:page.job-detail/my-proposal])
          my-job-story-id (:job-story/id @my-proposal)
          my-proposal? (not (nil? @my-proposal))
          my-proposal-withdrawable? (and @my-proposal (= "proposed" (:status @my-proposal)))]
      [c-main-layout {:container-opts {:class :job-detail-main-container}}
       [:div.header
        [:div.main
         [:div.title *title]
         [:div.sub-title *sub-title] ; TODO: where this comes from
         [:div.description *description]
         [:div.label "Required Skills"]
         [:div.skill-listing
          (for [skill *required-skills] [c-tag {:key skill} [c-tag-label skill]])]
         [:div.ticket-listing
          [:div.ticket
           [:div.label "Available Funds"]
           [:div.amount (str *job-token-amount " " *job-token-type)]]]
         [:div.profiles
          [:div.employer-detail
           [:div.header "Employer"]
           [:div.profile-image [c-profile-image {:src *employer-profile-image}]]
           [:div.name *employer-name]
           [:div.rating [c-rating {:rating *employer-rating}]]
           [:div.location *employer-country]
           [:div.fee ""]]
          [:div.arbiter-detail
           [:div.header "Arbiter"]
           [:div.profile-image [c-profile-image {:src *arbiter-profile-image}]]
           [:div.name *arbiter-name]
           [:div.rating [c-rating {:rating *arbiter-rating}]]
           [:div.location *arbiter-country]
           [:div.fee (str *arbiter-fee " " *arbiter-fee-currency)]]]]
        [:div.side
         [:div.label *posted-time]
         (for [tag-text *job-info-tags] [c-tag {:key tag-text} [c-tag-label tag-text]])]]
       [:div.proposal-listing
        [:div.label "Proposals"]
        [c-scrollable
         {:forceVisible true :autoHide false}
          (into [c-table {:headers ["" "Candidate" "Rate" "Created" "Status"]}]
                (map (fn [proposal]
                       [[:span (if (:current-user? proposal) "⭐" "")]
                        [:span (:candidate-name proposal)]
                        [:span (:rate proposal)]
                        [:span (millis->relative-time (:created-at proposal))]
                        [:span (:status proposal)]])
                     @proposals))]
        [:div.button-listing
         [c-circle-icon-button {:name :ic-arrow-left2 :size :small}]
         [c-circle-icon-button {:name :ic-arrow-left :size :small}]
         [c-circle-icon-button {:name :ic-arrow-right :size :small}]
         [c-circle-icon-button {:name :ic-arrow-right2 :size :small}]]
        [:div.proposal-form
         [:div.label "Send Proposal"]
         [c-token-values {:disabled? (not (nil? @my-proposal)) ; my-proposal?
                          :token-type *job-token-type
                          :token-amount *proposal-token-amount
                          :token-id *job-token-id
                          :token-address *job-token-address}]
         [:div.description-input
          [c-textarea-input
           {:disabled my-proposal?
            :placeholder "Proposal Description"
            :value (if my-proposal? (:message @my-proposal) @*proposal-text)
            :on-change #(re/dispatch [:page.job-detail/set-proposal-text %])}]]

         (if my-proposal-withdrawable?
           [c-button {:color :warning :on-click (fn [] (>evt [:page.job-proposal/remove my-job-story-id]))
                      :size :small}
            [c-button-label "Remove"]]
           )
         (if (not my-proposal?)
           [c-button {:on-click (fn [] (>evt [:page.job-proposal/send contract-address]))
                      :size :small}
            [c-button-label "Send"]])
         ]]

       [:div.invoice-listing
        [:div.label "Invoices"]
        [:div #_c-scrollable
         {:forceVisible true :autoHide false}
         [c-table
          {:headers ["Candidate" "Amount" "Created" "Status"]}
          [[:span "Giacomo Guilizzoni"]
           [:span "120 SNT"]
           [:span "5 Days Ago"]
           [:span "Full Payment"]]]]
        [:div.button-listing
         [c-circle-icon-button {:name :ic-arrow-left2 :size :small}]
         [c-circle-icon-button {:name :ic-arrow-left :size :small}]
         [c-circle-icon-button {:name :ic-arrow-right :size :small}]
         [c-circle-icon-button {:name :ic-arrow-right2 :size :small}]]]

       [:div.feedback-listing
        [:div.label "Feedback"]

        [c-carousel-old {}
         [c-feedback-slide {:rating 1}]
         [c-feedback-slide {:rating 2}]
         [c-feedback-slide {:rating 3}]
         [c-feedback-slide {:rating 4}]
         [c-feedback-slide {:rating 5}]]

        [c-carousel {}
         [c-feedback-slide {:rating 1}]
         [c-feedback-slide {:rating 2}]
         [c-feedback-slide {:rating 3}]
         [c-feedback-slide {:rating 4}]
         [c-feedback-slide {:rating 5}]
         ]]])))
