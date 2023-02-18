(ns ethlance.ui.page.job-detail
  (:require [district.ui.component.page :refer [page]]
            [ethlance.ui.component.button :refer [c-button c-button-label]]
            [ethlance.ui.component.carousel
             :refer
             [c-carousel c-carousel-old c-feedback-slide]]
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
            [ethlance.shared.utils :as shared-utils]
            [re-frame.core :as re]))

;; FIXME: description needs to be broken up into paragraphs. <p>
(def ^{:private true} fake-description
  "We are looking for help building a blockchain game called E.T.H. (Extreme Time Heroes). This is a turn-style fighting game that will be run on a custom hybrid plasma state-channels implementation. Players can collect heroes, battle for wagers or hero pink slips, and earn points for winning battles that let you mine new heroes. In collaboration with district0x, we are building a district to manage the hero/item marketplace. We also have plans to extend these NFT-wagered fights to other systems like Decentraland Land.

We are currently most in need of web developers but are open to those that would like to work on scalability research and development or blockchain/distributed game development.

Please contact us if this sounds interesting.")

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

          _ (println ">>> GQL job query RESULTS" @query-results)
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

          _ (println ">>> result keys" (keys results))
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

          *job-token-type (get-in results [:job/token-type])
          raw-token-amount (get-in results [:job/token-amount])
          *job-token-amount (if (= (str *job-token-type) "eth")
                              (shared-utils/wei->eth raw-token-amount)
                              raw-token-amount)]

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
        [:div #_c-scrollable
         {:forceVisible true :autoHide false}
         [c-table
          {:headers ["Candidate" "Rate" "Created" "Status"]}
          [[:span "Cyrus Keegan"]
           [:span "$25"]
           [:span "5 Days Ago"]
           [:span "Pending"]]]]
        [:div.button-listing
         [c-circle-icon-button {:name :ic-arrow-left2 :size :small}]
         [c-circle-icon-button {:name :ic-arrow-left :size :small}]
         [c-circle-icon-button {:name :ic-arrow-right :size :small}]
         [c-circle-icon-button {:name :ic-arrow-right2 :size :small}]]
        [:div.proposal-form
         [:div.label "Send Proposal"]
         [:div.amount-input
          [c-text-input
           {:placeholder "0"}]
          [c-select-input
           {:label "Token"
            :selections #{"ETH" "SNT" "DAI"}
            :default-selection "ETH"}]]
         [:div.description-input
          [c-textarea-input
           {:placeholder "Proposal Description"}]]
         [c-button {:size :small} [c-button-label "Send"]]]]

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
