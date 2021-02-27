(ns ethlance.ui.page.profile
  (:require [district.ui.component.page :refer [page]]
            [ethlance.ui.component.button :refer [c-button c-button-icon-label]]
            [ethlance.ui.component.carousel :refer [c-carousel c-feedback-slide]]
            [ethlance.ui.component.circle-button :refer [c-circle-icon-button]]
            [ethlance.ui.component.main-layout :refer [c-main-layout]]
            [ethlance.ui.component.profile-image :refer [c-profile-image]]
            [ethlance.ui.component.rating :refer [c-rating]]
            [ethlance.ui.component.scrollable :refer [c-scrollable]]
            [ethlance.ui.component.table :refer [c-table]]
            [ethlance.ui.component.tabular-layout :refer [c-tabular-layout]]
            [ethlance.ui.component.tag :refer [c-tag c-tag-label]]
            [ethlance.ui.subscriptions :as subs]
            [ethlance.ui.page.profile.subscriptions :as page-subs]
            [district.format :as format]
            [cljsjs.graphql]
            [clojure.string :as string]
            [district.graphql-utils :as utils]
            [re-frame.core :as re]
            [clojure.string :as string]
            ))

(defn c-tag-list [name tags]
  (let [container [:div {:class (string/lower-case name)} [:span name]]]
    (into container (map #(vector c-tag {} [c-tag-label %]) tags))))

(defn- format-date-looking-column [column value]
  (if (string/ends-with? (name column) "-date")
    (format/format-datetime (utils/gql-date->date value))
    value))

(defn c-job-activity-row [job column-names]
  (map #(conj [] :span (format-date-looking-column % (get job %))) column-names))

(defn c-job-activity [jobs keys-headers]
  (let [headers (map last keys-headers)
        column-names (map first keys-headers)]
    [:div.job-listing
      [:div.title "Job Activity"]
      [c-scrollable
       {:forceVisible true :autoHide false}
       (into [c-table {:headers headers}] (map #(c-job-activity-row % column-names) jobs))]

      [:div.button-listing
       [c-circle-icon-button {:name :ic-arrow-left2 :size :small}]
       [c-circle-icon-button {:name :ic-arrow-left :size :small}]
       [c-circle-icon-button {:name :ic-arrow-right :size :small}]
       [c-circle-icon-button {:name :ic-arrow-right2 :size :small}]]]))

(defn c-candidate-profile []
  (let [user @(re/subscribe [::subs/active-user])
        candidate @(re/subscribe [::subs/active-candidate])
        name (:user/name user)
        email (:user/email user)
        location (:user/country user)
        professional-title (:candidate/professional-title candidate)
        biography (:candidate/bio candidate)
        languages (:user/languages user)
        skills (:candidate/skills candidate)
        jobs @(re/subscribe [::page-subs/job-roles "0xc238fa6ccc9d226e2c49644b36914611319fc3ff" "CANDIDATE"])]
   [:<>
     [:div.candidate-profile
      [:div.title
       [:div.profile-image
        [c-profile-image {}]]
       [:div.name name]
       [:div.detail professional-title]]
      [:div.biography biography]
      [:div.rating
       [c-rating {:default-rating 3 :color :primary}] ; TODO
       [:span "(8)"]] ; TODO
      [:div.location location]
      [:div.detail-listing
       [c-tag-list "Languages" languages]
       [c-tag-list "Skills" skills]]

      [:div.button-listing
       [c-button
        {:size :normal}
        [c-button-icon-label {:icon-name :github :label-text "Github"}]]
       [c-button
        {:size :normal}
        [c-button-icon-label {:icon-name :linkedin :label-text "LinkedIn"}]]]]

     (c-job-activity jobs {:title "Title" :start-date "Created"})

     [:div.feedback-listing
      [:div.title "Feedback"]
      [:div.sub-title "Smart Contract Hacker"]
      [c-carousel {}
       [c-feedback-slide {:rating 1}]
       [c-feedback-slide {:rating 2}]
       [c-feedback-slide {:rating 3}]
       [c-feedback-slide {:rating 4}]
       [c-feedback-slide {:rating 5}]]
      ]]))

(defn c-employer-profile []
  (let [user @(re/subscribe [::subs/active-user])
        employer @(re/subscribe [::subs/active-employer])
        name (:user/name user)
        email (:user/email user)
        location (:user/country user)
        professional-title (:candidate/professional-title employer)
        biography (:candidate/bio employer)
        languages (:user/languages user)
        jobs @(re/subscribe [::page-subs/job-roles "0xc238fa6ccc9d226e2c49644b36914611319fc3ff" "EMPLOYER"])]
    [:<>
     [:div.employer-profile
      [:div.title
       [:div.profile-image
        [c-profile-image {}]]
       [:div.name name]
       [:div.detail professional-title]]
      [:div.biography biography]
      [:div.rating
       [c-rating {:rating 3 :color :primary}] ; TODO
       [:span "(8)"]] ; TODO
      [:div.location location]
      [:div.detail-listing
       [c-tag-list "Languages" languages]]
      [:div.button-listing
       [c-button
        {:size :normal}
        [c-button-icon-label {:icon-name :github :label-text "Github"}]]
       [c-button
        {:size :normal}
        [c-button-icon-label {:icon-name :linkedin :label-text "LinkedIn"}]]]]

     (c-job-activity jobs {:title "Title" :start-date "Created" :status "Status"})

     [:div.feedback-listing
      [:div.title "Feedback"]
      [:div.sub-title "Smart Contract Hacker"]
      [c-carousel {}
       [c-feedback-slide {:rating 1}]
       [c-feedback-slide {:rating 2}]
       [c-feedback-slide {:rating 3}]
       [c-feedback-slide {:rating 4}]
       [c-feedback-slide {:rating 5}]]
    ]]))

(defn c-arbiter-profile []
  (let [user @(re/subscribe [::subs/active-user])
        arbiter @(re/subscribe [::subs/active-arbiter])
        name (:user/name user)
        email (:user/email user)
        location (:user/country user)
        professional-title (:candidate/professional-title arbiter)
        biography (:candidate/bio arbiter)
        languages (:user/languages user)
        jobs @(re/subscribe [::page-subs/job-roles "0xc238fa6ccc9d226e2c49644b36914611319fc3ff" "ARBITER"])
        ]
    [:<>
     [:div.arbiter-profile
      [:div.title
       [:div.profile-image
        [c-profile-image {}]]
       [:div.name name]
       [:div.detail professional-title]]
      [:div.biography biography]
      [:div.rating
       [c-rating {:rating 3 :color :primary}] ; TODO
       [:span "(8)"]] ; TODO
      [:div.location location]
      [:div.detail-listing
       [c-tag-list "Languages" languages]]
      [:div.button-listing
       [c-button
        {:size :normal}
        [c-button-icon-label {:icon-name :github :label-text "Github"}]]
       [c-button
        {:size :normal}
        [c-button-icon-label {:icon-name :linkedin :label-text "LinkedIn"}]]]]

     (c-job-activity jobs {:title "Title" :start-date "Created"})

     [:div.feedback-listing
      [:div.title "Feedback"]
      [:div.sub-title "Smart Contract Hacker"]
      [c-carousel {}
       [c-feedback-slide {:rating 1}]
       [c-feedback-slide {:rating 2}]
       [c-feedback-slide {:rating 3}]
       [c-feedback-slide {:rating 4}]
       [c-feedback-slide {:rating 5}]]
    ]]))

(defmethod page :route.user/profile []
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
      [c-arbiter-profile]]]))
