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
            [ethlance.ui.component.textarea-input :refer [c-textarea-input]]))

;; FIXME: description needs to be broken up into paragraphs. <p>
(def ^{:private true} fake-description
  "We are looking for help building a blockchain game called E.T.H. (Extreme Time Heroes). This is a turn-style fighting game that will be run on a custom hybrid plasma state-channels implementation. Players can collect heroes, battle for wagers or hero pink slips, and earn points for winning battles that let you mine new heroes. In collaboration with district0x, we are building a district to manage the hero/item marketplace. We also have plans to extend these NFT-wagered fights to other systems like Decentraland Land.

We are currently most in need of web developers but are open to those that would like to work on scalability research and development or blockchain/distributed game development.

Please contact us if this sounds interesting.")

(defmethod page :route.job/detail []
  (fn []
    [c-main-layout {:container-opts {:class :job-detail-main-container}}
     [:div.header
      [:div.main
       [:div.title "Finality Labs Full-Stack dApp Dev"]
       [:div.sub-title "Web, Mobile, and Software Development"]
       [:div.description fake-description]
       [:div.label "Required Skills"]
       [:div.skill-listing
        [c-tag {} [c-tag-label "System Administration"]]
        [c-tag {} [c-tag-label "Game Design"]]
        [c-tag {} [c-tag-label "Game Development"]]
        [c-tag {} [c-tag-label "Web Programming"]]]
       [:div.ticket-listing
        [:div.ticket
         [:div.label "Available Funds"]
         [:div.amount "14,000 SNT"]]]
       [:div.profiles
        [:div.employer-detail
         [:div.header "Employer"]
         [:div.profile-image [c-profile-image {}]]
         [:div.name "Brian Curran"]
         [:div.rating [c-rating {:default-rating 3}]]
         [:div.location "New York, United States"]
         [:div.fee ""]]
        [:div.arbiter-detail
         [:div.header "Arbiter"]
         [:div.profile-image [c-profile-image {}]]
         [:div.name "Brian Curran"]
         [:div.rating [c-rating {:default-rating 3}]]
         [:div.location "New York, United States"]
         [:div.fee "Fee: 0.12 ETH"]]]]
      [:div.side
       [:div.label "Posted 7 Days Ago"]
       [c-tag {} [c-tag-label "Hiring"]]
       [c-tag {} [c-tag-label "Hourly Rate"]]
       [c-tag {} [c-tag-label "For Months"]]
       [c-tag {} [c-tag-label "For Expert ($$$)"]]
       [c-tag {} [c-tag-label "Full Time"]]
       [c-tag {} [c-tag-label "Needs 3 Freelancers"]]]]

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
       ]]]))
