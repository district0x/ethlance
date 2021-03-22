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
            [district.ui.router.subs :as router-subs]
            [district.ui.router.events :as router-events]
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

(defn c-rating-box [rating]
  [:div.rating
   [c-rating {:rating (:average rating) :color :primary}]
   [:span (str "(" (:count rating) ")")]])

(defn c-feedback-listing [feedback-list]
  [:div.feedback-listing
      [:div.title "Feedback"]
      [:div.sub-title "Smart Contract Hacker"]
      (into [c-carousel {}] (map #(c-feedback-slide %) feedback-list))])

(defn c-candidate-profile []
  (let [page-params @(re/subscribe [::router-subs/active-page-params])
        address (:address page-params)
        user @(re/subscribe [::subs/user address])
        candidate @(re/subscribe [::subs/candidate address])
        name (:user/name user)
        email (:user/email user)
        location (:user/country user)
        professional-title (:candidate/professional-title candidate)
        biography (:candidate/bio candidate)
        languages (:user/languages user)
        skills (:candidate/skills candidate)
        jobs @(re/subscribe [::page-subs/job-roles address "CANDIDATE"])
        rating @(re/subscribe [::page-subs/candidate-ratings address ])
        feedback-list @(re/subscribe [::page-subs/candidate-feedback-cards address])
        ]
   [:<>
     [:div.candidate-profile
      [:div.title
       [:div.profile-image
        [c-profile-image {}]]
       [:div.name name]
       [:div.detail professional-title]]
      [:div.biography biography]
      [c-rating-box rating]
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
     (c-feedback-listing feedback-list)
     ]))

(defn c-employer-profile []
  (let [user @(re/subscribe [::subs/active-user])
        employer @(re/subscribe [::subs/active-employer])
        name (:user/name user)
        email (:user/email user)
        location (:user/country user)
        professional-title (:candidate/professional-title employer)
        biography (:candidate/bio employer)
        languages (:user/languages user)
        jobs @(re/subscribe [::page-subs/job-roles "0xc238fa6ccc9d226e2c49644b36914611319fc3ff" "EMPLOYER"])
        feedback-list @(re/subscribe [::page-subs/candidate-feedback-cards "0xc238fa6ccc9d226e2c49644b36914611319fc3ff"]) ; FIXME: employer subscription
        ]
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
     (c-feedback-listing feedback-list)]))

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
        feedback-list @(re/subscribe [::page-subs/candidate-feedback-cards "0xc238fa6ccc9d226e2c49644b36914611319fc3ff"]) ; FIXME: replace with arbiter subscription
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
     (c-feedback-listing feedback-list)]))

(defmethod page :route.user/profile []
  (let [{:keys [name params query]} @(re/subscribe [::router-subs/active-page])
        tabs {"candidate" 0 "employer" 1 "arbiter" 2}
        default-tab (get tabs (:tab query) 0)
        navigate-to (fn [tab name params] (when name (re/dispatch [::router-events/navigate name params (merge query {:tab tab})])))
        navigate-to-candidate (partial navigate-to "candidate" name params)
        navigate-to-employer (partial navigate-to "employer" name params)
        navigate-to-arbiter (partial navigate-to "arbiter" name params)]
    (fn []
      [c-main-layout {:container-opts {:class :profile-main-container}}
       [c-tabular-layout
        {:key "profile-tabular-layout"
         :default-tab default-tab}

        {:label "Candidate Profile" :on-click navigate-to-candidate}
        [c-candidate-profile]

        {:label "Employer Profile" :on-click navigate-to-employer}
        [c-employer-profile]

        {:label "Arbiter Profile" :on-click navigate-to-arbiter}
        [c-arbiter-profile]
        ]])))
