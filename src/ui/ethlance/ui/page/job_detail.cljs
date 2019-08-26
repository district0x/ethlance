(ns ethlance.ui.page.job-detail
  (:require
   [taoensso.timbre :as log]
   [district.ui.component.page :refer [page]]

   ;; Ethlance Components
   [ethlance.ui.component.button :refer [c-button c-button-icon-label]]
   [ethlance.ui.component.circle-button :refer [c-circle-icon-button]]
   [ethlance.ui.component.currency-input :refer [c-currency-input]]
   [ethlance.ui.component.inline-svg :refer [c-inline-svg]]
   [ethlance.ui.component.main-layout :refer [c-main-layout]]
   [ethlance.ui.component.radio-select :refer [c-radio-select c-radio-search-filter-element]]
   [ethlance.ui.component.rating :refer [c-rating]]
   [ethlance.ui.component.search-input :refer [c-chip-search-input]]
   [ethlance.ui.component.table :refer [c-table]]
   [ethlance.ui.component.tabular-layout :refer [c-tabular-layout]]
   [ethlance.ui.component.tag :refer [c-tag c-tag-label]]
   [ethlance.ui.component.profile-image :refer [c-profile-image]]
   [ethlance.ui.component.carousel :refer [c-carousel]]))


(def ^{:private true} fake-description
  "We are looking for help building a blockchain game called E.T.H. (Extreme Time Heroes). This is a turn-style fighting game that will be run on a custom hybrid plasma state-channels implementation. Players can collect heroes, battle for wagers or hero pink slips, and earn points for winning battles that let you mine new heroes. In collaboration with district0x, we are building a district to manage the hero/item marketplace. We also have plans to extend these NFT-wagered fights to other systems like Decentraland Land.

We are currently most in need of web developers but are open to those that would like to work on scalability research and development or blockchain/distributed game development.

Please contact us if this sounds interesting.")


(defmethod page :route.job/detail []
  (let []
    (fn []
      [c-main-layout {:container-opts {:class :job-detail-main-container}}
       [:div.header
        [:div.main
         [:div.title "Finality Labs Full-Stack dApp Dev"]
         [:div.sub-title "Web, Mobile, and Software Development"]
         [:div.description fake-description]
         [:div.title "Required Skills"]
         [:div.skill-listing
          [c-tag {} [c-tag-label "System Administration"]]
          [c-tag {} [c-tag-label "Game Design"]]
          [c-tag {} [c-tag-label "Game Development"]]
          [c-tag {} [c-tag-label "Web Programming"]]]
         [:div.ticket
          [:div.label "Available Funds"]
          [:div.amount "14,000 SNT"]]
         [:div.profiles
          [:div.employer-detail]
          [:div.arbiter-detail]]]
        [:div.side
         [:div.title "Posted 7 Days Ago"]
         [c-tag {} [c-tag-label "Hiring"]]
         [c-tag {} [c-tag-label "Hourly Rate"]]
         [c-tag {} [c-tag-label "For Months"]]
         [c-tag {} [c-tag-label "For Expert ($$$)"]]
         [c-tag {} [c-tag-label "Full Time"]]
         [c-tag {} [c-tag-label "Needs 3 Freelancers"]]]]


       [:div.proposal-listing
        [:div.proposal-table
         [c-table
          {:headers ["Candidate" "Rate" "Created" "Status"]}
          [[:span "Cyrus Keegan"]
           [:span "$25"]
           [:span "5 Days Ago"]
           [:span "Pending"]]]]
        [:div.button-listing
         [c-circle-icon-button {:name :ic-arrow-left2}]
         [c-circle-icon-button {:name :ic-arrow-left}]
         [c-circle-icon-button {:name :ic-arrow-right}]
         [c-circle-icon-button {:name :ic-arrow-right2}]]
        [:div.proposal-form
         "Proposal Form"]]

       [:div.invoice-listing
        [c-table
         {:headers ["Candidate" "Amount" "Created" "Status"]}
         [[:span "Giacomo Guilizzoni"]
          [:span "120 SNT"]
          [:span "5 Days Ago"]
          [:span "Full Payment"]]]
        [:div.button-listing
         [c-circle-icon-button {:name :ic-arrow-left2}]
         [c-circle-icon-button {:name :ic-arrow-left}]
         [c-circle-icon-button {:name :ic-arrow-right}]
         [c-circle-icon-button {:name :ic-arrow-right2}]]]

       [:div.feedback-listing
        [c-carousel {}]]])))
         
