(ns ethlance.ui.page.profile
  (:require
   [taoensso.timbre :as log]
   [district.ui.component.page :refer [page]]

   [ethlance.shared.enumeration.currency-type :as enum.currency]

   ;; Ethlance Components
   [ethlance.ui.component.button :refer [c-button c-button-icon-label]]
   [ethlance.ui.component.carousel :refer [c-carousel c-feedback-slide]]
   [ethlance.ui.component.circle-button :refer [c-circle-icon-button]]
   [ethlance.ui.component.currency-input :refer [c-currency-input]]
   [ethlance.ui.component.inline-svg :refer [c-inline-svg]]
   [ethlance.ui.component.main-layout :refer [c-main-layout]]
   [ethlance.ui.component.profile-image :refer [c-profile-image]]
   [ethlance.ui.component.radio-select :refer [c-radio-select c-radio-search-filter-element]]
   [ethlance.ui.component.rating :refer [c-rating]]
   [ethlance.ui.component.scrollable :refer [c-scrollable]]
   [ethlance.ui.component.search-input :refer [c-chip-search-input]]
   [ethlance.ui.component.table :refer [c-table]]
   [ethlance.ui.component.tabular-layout :refer [c-tabular-layout]]
   [ethlance.ui.component.tag :refer [c-tag c-tag-label]]))


(defn c-candidate-profile []
  [:<>
   [:div.candidate-profile
    [:div.title
     [:div.profile-image
      [c-profile-image {}]]
     [:div.name "Clement Lesaenge"]
     [:div.detail "Graphic Design | Joined 8 Days Ago"]]
    [:div.biography
     "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do
     eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut
     enim ad minim veniam, quis nostrud exercitation ullamco laboris
     nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in
     reprehenderit in voluptate velit esse cillum dolore eu fugiat
     nulla pariatur. Excepteur sint occaecat cupidatat non proident,
     sunt in culpa qui officia deserunt mollit anim id est
     laborum."]
    [:div.rating
     [c-rating {:rating 3 :color :primary}]
     [:span "(8)"]]
    [:div.location "United States, New York"]
    [:div.detail-listing
     [:div.languages
      [:span "Languages"]
      [c-tag {} [c-tag-label "English"]]
      [c-tag {} [c-tag-label "German"]]]
     [:div.skills
      [:span "Skills"]
      [c-tag {} [c-tag-label "Javascript Programming"]]
      [c-tag {} [c-tag-label "HTML / CSS"]]]]
    
    [:div.button-listing
     [c-button
      {:size :normal}
      [c-button-icon-label {:icon-name :github :label-text "Github"}]]
     [c-button
      {:size :normal}
      [c-button-icon-label {:icon-name :linkedin :label-text "LinkedIn"}]]]]

   [:div.job-listing
    [:div.title "Job Activity"]
    [c-scrollable
     {:forceVisible true :autoHide false}
     [c-table
      {:headers ["Title" "Hired" "Created" "Status"]}
      [[:span "Cryptoeconomics Research Intern"]
       [:span "1"]
       [:span "5 Days Ago"]
       [:span "Hiring"]]

      [[:span "Cryptoeconomics Research Intern"]
       [:span "1"]
       [:span "5 Days Ago"]
       [:span "Hiring"]]

      [[:span "Cryptoeconomics Research Intern"]
       [:span "1"]
       [:span "5 Days Ago"]
       [:span "Hiring"]]

      [[:span "Cryptoeconomics Research Intern"]
       [:span "1"]
       [:span "5 Days Ago"]
       [:span "Hiring"]]]]

    [:div.button-listing
     [c-circle-icon-button {:name :ic-arrow-left2 :size :small}]
     [c-circle-icon-button {:name :ic-arrow-left :size :small}]
     [c-circle-icon-button {:name :ic-arrow-right :size :small}]
     [c-circle-icon-button {:name :ic-arrow-right2 :size :small}]]]

   [:div.feedback-listing
    [:div.title "Feedback"]
    [:div.sub-title "Smart Contract Hacker"]
    [c-carousel {}
     [c-feedback-slide {:rating 1}]
     [c-feedback-slide {:rating 2}]
     [c-feedback-slide {:rating 3}]
     [c-feedback-slide {:rating 4}]
     [c-feedback-slide {:rating 5}]]]])
  


(defn c-employer-profile []
  [:<>
   [:div.employer-profile
    [:div.title
     [:div.profile-image
      [c-profile-image {}]]
     [:div.name "Clement Lesaenge"]
     [:div.detail "Graphic Design | Joined 8 Days Ago"]]
    [:div.biography
     "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do
     eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut
     enim ad minim veniam, quis nostrud exercitation ullamco laboris
     nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in
     reprehenderit in voluptate velit esse cillum dolore eu fugiat
     nulla pariatur. Excepteur sint occaecat cupidatat non proident,
     sunt in culpa qui officia deserunt mollit anim id est
     laborum."]
    [:div.rating
     [c-rating {:rating 3 :color :primary}]
     [:span "(8)"]]
    [:div.location "United States, New York"]
    [:div.detail-listing
     [:div.languages
      [:span "Languages"]
      [c-tag {} [c-tag-label "English"]]
      [c-tag {} [c-tag-label "German"]]]
     [:div.skills
      [:span "Skills"]
      [c-tag {} [c-tag-label "Javascript Programming"]]
      [c-tag {} [c-tag-label "HTML / CSS"]]]]
    
    [:div.button-listing
     [c-button
      {:size :normal}
      [c-button-icon-label {:icon-name :github :label-text "Github"}]]
     [c-button
      {:size :normal}
      [c-button-icon-label {:icon-name :linkedin :label-text "LinkedIn"}]]]]

   [:div.job-listing
    [:div.title "Job Activity"]
    [c-scrollable
     {:forceVisible true :autoHide false}
     [c-table
      {:headers ["Title" "Hired" "Created" "Status"]}
      [[:span "Cryptoeconomics Research Intern"]
       [:span "1"]
       [:span "5 Days Ago"]
       [:span "Hiring"]]

      [[:span "Cryptoeconomics Research Intern"]
       [:span "1"]
       [:span "5 Days Ago"]
       [:span "Hiring"]]

      [[:span "Cryptoeconomics Research Intern"]
       [:span "1"]
       [:span "5 Days Ago"]
       [:span "Hiring"]]

      [[:span "Cryptoeconomics Research Intern"]
       [:span "1"]
       [:span "5 Days Ago"]
       [:span "Hiring"]]]]

    [:div.button-listing
     [c-circle-icon-button {:name :ic-arrow-left2 :size :small}]
     [c-circle-icon-button {:name :ic-arrow-left :size :small}]
     [c-circle-icon-button {:name :ic-arrow-right :size :small}]
     [c-circle-icon-button {:name :ic-arrow-right2 :size :small}]]]

   [:div.feedback-listing
    [:div.title "Feedback"]
    [:div.sub-title "Smart Contract Hacker"]
    [c-carousel {}
     [c-feedback-slide {:rating 1}]
     [c-feedback-slide {:rating 2}]
     [c-feedback-slide {:rating 3}]
     [c-feedback-slide {:rating 4}]
     [c-feedback-slide {:rating 5}]]]])


(defn c-arbiter-profile []
  [:<>
   [:div.arbiter-profile
    [:div.title
     [:div.profile-image
      [c-profile-image {}]]
     [:div.name "Clement Lesaenge"]
     [:div.detail "Graphic Design | Joined 8 Days Ago"]]
    [:div.biography
     "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do
     eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut
     enim ad minim veniam, quis nostrud exercitation ullamco laboris
     nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in
     reprehenderit in voluptate velit esse cillum dolore eu fugiat
     nulla pariatur. Excepteur sint occaecat cupidatat non proident,
     sunt in culpa qui officia deserunt mollit anim id est
     laborum."]
    [:div.rating
     [c-rating {:rating 3 :color :primary}]
     [:span "(8)"]]
    [:div.location "United States, New York"]
    [:div.detail-listing
     [:div.languages
      [:span "Languages"]
      [c-tag {} [c-tag-label "English"]]
      [c-tag {} [c-tag-label "German"]]]
     [:div.skills
      [:span "Skills"]
      [c-tag {} [c-tag-label "Javascript Programming"]]
      [c-tag {} [c-tag-label "HTML / CSS"]]]]
    
    [:div.button-listing
     [c-button
      {:size :normal}
      [c-button-icon-label {:icon-name :github :label-text "Github"}]]
     [c-button
      {:size :normal}
      [c-button-icon-label {:icon-name :linkedin :label-text "LinkedIn"}]]]]

   [:div.job-listing
    [:div.title "Job Activity"]
    [c-scrollable
     {:forceVisible true :autoHide false}
     [c-table
      {:headers ["Title" "Hired" "Created" "Status"]}
      [[:span "Cryptoeconomics Research Intern"]
       [:span "1"]
       [:span "5 Days Ago"]
       [:span "Hiring"]]

      [[:span "Cryptoeconomics Research Intern"]
       [:span "1"]
       [:span "5 Days Ago"]
       [:span "Hiring"]]

      [[:span "Cryptoeconomics Research Intern"]
       [:span "1"]
       [:span "5 Days Ago"]
       [:span "Hiring"]]

      [[:span "Cryptoeconomics Research Intern"]
       [:span "1"]
       [:span "5 Days Ago"]
       [:span "Hiring"]]]]

    [:div.button-listing
     [c-circle-icon-button {:name :ic-arrow-left2 :size :small}]
     [c-circle-icon-button {:name :ic-arrow-left :size :small}]
     [c-circle-icon-button {:name :ic-arrow-right :size :small}]
     [c-circle-icon-button {:name :ic-arrow-right2 :size :small}]]]

   [:div.feedback-listing
    [:div.title "Feedback"]
    [:div.sub-title "Smart Contract Hacker"]
    [c-carousel {}
     [c-feedback-slide {:rating 1}]
     [c-feedback-slide {:rating 2}]
     [c-feedback-slide {:rating 3}]
     [c-feedback-slide {:rating 4}]
     [c-feedback-slide {:rating 5}]]]])


(defmethod page :route.user/profile []
  (let []
    (fn []
      [c-main-layout {:container-opts {:class :profile-main-container}}
       [c-tabular-layout
        {:key "profile-tabular-layout"
         :default-tab 0}

        {:label "Candidate Profile"}
        [c-candidate-profile]

        {:label "Employer Profile"}
        [c-employer-profile]

        {:label "Arbiter Profile"}
        [c-arbiter-profile]]])))
