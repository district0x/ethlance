(ns ethlance.ui.page.me
  (:require [district.ui.component.page :refer [page]]
            [district.ui.router.subs :as router.subs]
            [ethlance.ui.component.circle-button :refer [c-circle-icon-button]]
            [ethlance.ui.component.main-layout :refer [c-main-layout]]
            [ethlance.ui.component.mobile-sidebar :refer [c-mobile-sidebar]]
            [ethlance.ui.component.table :refer [c-table]]
            [ethlance.ui.component.tabular-layout :refer [c-tabular-layout]]
            [ethlance.ui.util.navigation :as util.navigation]
            [re-frame.core :as re]))

(defn c-nav-sidebar-element [label location]
  (let [*current-sidebar-choice (re/subscribe [:page.me/current-sidebar-choice])
        *active-page (re/subscribe [::router.subs/active-page])]
    (fn []
      (let [{active-page :name
             active-params :param
             active-query :query} @*active-page
            updated-query (assoc (or active-query {}) :sidebar (name location))]
        [:div.nav-element
         [:a.link
          {:title (str "Navigate to '" label "'")
           :class [(when (= @*current-sidebar-choice location) "active")]
           :href (util.navigation/resolve-route {:route active-page :params active-params :query updated-query})
           :on-click (fn [e]
                       (re/dispatch [:page.me/change-sidebar-choice location])
                       (.preventDefault e)
                       nil)}
          label]]))))

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

(defn c-my-employer-contract-listing []
  [:div.not-implemented "Not Implemented - Employer - My Contract"])

(defn c-my-employer-invoice-listing []
  [:div.not-implemented "Not Implemented - Employer - My Invoices"])

(defn c-my-employer-dispute-listing []
  [:div.not-implemented "Not Implemented - Employer - My Disputes"])

;;
;; Candidate Sections
;;

(defn c-my-candidate-job-listing []
  [:div.not-implemented "Not Implemented - Candidate - My Jobs"])

(defn c-my-candidate-contract-listing []
  [:div.not-implemented "Not Implemented - Candidate - My Contracts"])

(defn c-my-candidate-invoice-listing []
  [:div.not-implemented "Not Implemented - Candidate - My Invoices"])

(defn c-my-candidate-dispute-listing []
  [:div.not-implemented "Not Implemented - Candidate - My Disputes"])

;;
;; Arbiter Sections
;;

(defn c-my-arbiter-job-listing []
  [:div.not-implemented "Not Implemented - Arbiter - My Jobs"])

(defn c-my-arbiter-contract-listing []
  [:div.not-implemented "Not Implemented - Arbiter - My Contracts"])

(defn c-my-arbiter-dispute-listing []
  [:div.not-implemented "Not Implemented - Arbiter - My Disputes"])

(defn c-sidebar
  []
  (fn []
    [:div.sidebar
     [:div.section
      [:div.label "Employer"]
      [c-nav-sidebar-element "My Jobs" :my-employer-job-listing]
      [c-nav-sidebar-element "My Contracts" :my-employer-contract-listing]
      [c-nav-sidebar-element "My Invoices" :my-employer-invoice-listing]
      [c-nav-sidebar-element "My Disputes" :my-employer-dispute-listing]]

     [:div.section
      [:div.label "Candidate"]
      [c-nav-sidebar-element "My Jobs" :my-candidate-job-listing]
      [c-nav-sidebar-element "My Contracts" :my-candidate-contract-listing]
      [c-nav-sidebar-element "My Invoices" :my-candidate-invoice-listing]
      [c-nav-sidebar-element "My Disputes" :my-candidate-dispute-listing]]

     [:div.section
      [:div.label "Arbiter"]
      [c-nav-sidebar-element "My Jobs" :my-arbiter-job-listing]
      [c-nav-sidebar-element "My Contracts" :my-arbiter-contract-listing]
      [c-nav-sidebar-element "My Invoices" :my-arbiter-invoice-listing]
      [c-nav-sidebar-element "My Disputes" :my-arbiter-dispute-listing]]]))

(defn c-mobile-navigation
  []
  (fn []
    [c-mobile-sidebar
     [c-sidebar]]))

(defn c-listing []
  (let [*current-sidebar-choice (re/subscribe [:page.me/current-sidebar-choice])]
    (fn []
      [:div.listing
       (case @*current-sidebar-choice
         ;; Employer
         :my-employer-job-listing [c-my-employer-job-listing]
         :my-employer-contract-listing [c-my-employer-contract-listing]
         :my-employer-invoice-listing [c-my-employer-invoice-listing]
         :my-employer-dispute-listing [c-my-employer-dispute-listing]

         ;; Candidate
         :my-candidate-job-listing [c-my-candidate-job-listing]
         :my-candidate-contract-listing [c-my-candidate-contract-listing]
         :my-candidate-invoice-listing [c-my-candidate-invoice-listing]
         :my-candidate-dispute-listing [c-my-candidate-dispute-listing]

         ;; Arbiter
         :my-arbiter-job-listing [c-my-arbiter-job-listing]
         :my-arbiter-contract-listing [c-my-arbiter-contract-listing]
         :my-arbiter-dispute-listing [c-my-arbiter-dispute-listing]

         (throw (ex-info "Unable to determine sidebar choice" @*current-sidebar-choice)))])))

(defmethod page :route.me/index []
  (fn []
    [c-main-layout {:container-opts {:class :my-contracts-main-container}}
     [c-sidebar]
     [c-mobile-navigation]
     [c-listing]]))
