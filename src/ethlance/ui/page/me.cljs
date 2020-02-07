(ns ethlance.ui.page.me
  (:require
   [taoensso.timbre :as log]
   [district.ui.component.page :refer [page]]
   [reagent.core :as r]

   [ethlance.shared.enumeration.currency-type :as enum.currency]
   [ethlance.shared.constants :as constants]

   ;; Ethlance Components
   [ethlance.ui.component.circle-button :refer [c-circle-icon-button]]
   [ethlance.ui.component.currency-input :refer [c-currency-input]]
   [ethlance.ui.component.inline-svg :refer [c-inline-svg]]
   [ethlance.ui.component.main-layout :refer [c-main-layout]]
   [ethlance.ui.component.mobile-sidebar :refer [c-mobile-sidebar]]
   [ethlance.ui.component.profile-image :refer [c-profile-image]]
   [ethlance.ui.component.radio-select :refer [c-radio-select c-radio-search-filter-element]]
   [ethlance.ui.component.rating :refer [c-rating]]
   [ethlance.ui.component.search-input :refer [c-chip-search-input]]
   [ethlance.ui.component.select-input :refer [c-select-input]]
   [ethlance.ui.component.table :refer [c-table]]
   [ethlance.ui.component.tabular-layout :refer [c-tabular-layout]]
   [ethlance.ui.component.tag :refer [c-tag c-tag-label]]))


;;
;; Employer
;;


(defn c-default-listing []
  [:<>
   [c-table
    {:headers ["Title" "Candidate" "Rate" "Total Spent"]}
    [[:span "Cryptoeconomics Research Intern"]
     [:span "Keegan Quigley"]
     [:span "$30/hr"]
     [:span "12.2 ETH"]]

    [[:span "Smart Contract Hacker"]
     [:span "Cyrus Karsen"]
     [:span "$25"]
     [:span "1000 SNT"]]

    [[:span "Interactive Developer"]
     [:span "Ari Kaplan"]
     [:span "$75"]
     [:span "5.4 ETH"]]

    [[:span "Cryptoeconomics Research Intern"]
     [:span "Keegan Quigley"]
     [:span "$30/hr"]
     [:span "12.2 ETH"]]]
   [:div.button-listing
    [c-circle-icon-button {:name :ic-arrow-left2 :size :smaller :disabled? true}]
    [c-circle-icon-button {:name :ic-arrow-left :size :smaller :disabled? true}]
    [c-circle-icon-button {:name :ic-arrow-right :size :smaller :disabled? true}]
    [c-circle-icon-button {:name :ic-arrow-right2 :size :smaller :disabled? true}]]])


(defn c-my-employer-job-listing []
  [c-tabular-layout
   {:key "my-employer-job-tab-listing"
    :default-tab 0}

   {:label "Invitations"}
   [:div.listing.my-employer-job-listing
    [c-default-listing]]
    
   {:label "Pending Proposals"}
   [:div.listing
    [c-default-listing]]
   
   {:label "Active Contracts"}
   [:div.listing
    [c-default-listing]]

   {:label "Finished Contracts"}
   [:div.listing
    [c-default-listing]]

   {:label "Canceled Contracts"}
   [:div.listing
    [c-default-listing]]])


(defn c-my-employer-contract-listing [])
(defn c-my-employer-invoice-listing [])
(defn c-my-employer-dispute-listing [])

;;
;; Candidate
;;

(defn c-my-candidate-job-listing [])
(defn c-my-candidate-contract-listing [])
(defn c-my-candidate-invoice-listing [])
(defn c-my-candidate-dispute-listing [])

;;
;; Arbiter
;;

(defn c-my-arbiter-job-listing [])
(defn c-my-arbiter-contract-listing [])
(defn c-my-arbiter-dispute-listing [])


(defn c-sidebar [{:keys [*sidebar-choice]}]
  (fn []
    [:div.sidebar
     [:div.section
      [:div.label "Employer"]
      [:div.link.active "My Jobs"]
      [:div.link "My Contracts"]
      [:div.link "My Invoices"]
      [:div.link "My Disputes"]]
     
     [:div.section
      [:div.label "Candidate"]
      [:div.link "My Jobs"]
      [:div.link "My Contracts"]
      [:div.link "My Invoices"]
      [:div.link "My Disputes"]]

     [:div.section
      [:div.label "Arbiter"]
      [:div.link "My Jobs"]
      [:div.link "My Contracts"]
      [:div.link "My Disputes"]]]))


(defn c-mobile-navigation [{:keys [*sidebar-choice]}]
  (fn []
    [c-mobile-sidebar
     [:div.sidebar
      [:div.section
       [:div.label "Employer"]
       [:div.link.active "My Jobs"]
       [:div.link "My Contracts"]
       [:div.link "My Invoices"]
       [:div.link "My Disputes"]]
      
      [:div.section
       [:div.label "Candidate"]
       [:div.link "My Jobs"]
       [:div.link "My Contracts"]
       [:div.link "My Invoices"]
       [:div.link "My Disputes"]]

      [:div.section
       [:div.label "Arbiter"]
       [:div.link "My Jobs"]
       [:div.link "My Contracts"]
       [:div.link "My Disputes"]]]]))


(defn c-listing [{:keys [*sidebar-choice]}]
  [:div.listing
   (case @*sidebar-choice
    :my-employer-job-listing [c-my-employer-job-listing]
    (throw (ex-info "Unable to determine sidebar choice" {:sidebar-choice @*sidebar-choice})))])


(defmethod page :route.me/index []
  (let [*sidebar-choice (r/atom :my-employer-job-listing)]
    (fn []
      [c-main-layout {:container-opts {:class :my-contracts-main-container}}
       [c-sidebar {:*sidebar-choice *sidebar-choice}]
       [c-mobile-navigation {:*sidebar-choice *sidebar-choice}]
       [c-listing {:*sidebar-choice *sidebar-choice}]])))
