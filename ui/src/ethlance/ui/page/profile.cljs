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
            [district.ui.router.subs :as router-subs]
            [district.ui.router.events :as router-events]
            [district.format :as format]
            [cljsjs.graphql]
            [clojure.string :as string]
            [district.graphql-utils :as utils]
            [district.ui.graphql.subs :as gql]
            [re-frame.core :as re]))

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

(def log (.-log js/console))

(defn prepare-ratings [rating]
  {:rating (:feedback/rating rating)
   :from (get-in rating [:feedback/from-user :user/name])
   :text (:feedback/text rating)})

(defn prepare-feedback-cards [item]
  {:rating (:feedback/rating item)
   :text (:feedback/text item)
   :author (get-in item [:feedback/from-user :user/name])})

(defn prepare-jobs [story]
  {:title (get-in story [:job :job/title])
   :start-date (get-in story [:job-story/date-candidate-accepted])
   :status (get-in story [:job :job/status])})

(defn prepare-employer-jobs [story]
  {:title (get-in story [:job :job/title])
   :start-date (get-in story [:job-story/date-created])
   :status (get-in story [:job :job/status])})

(defn prepare-arbitrations [arbitration]
  {:title (get-in arbitration [:job :job/title])
   :start-date (get-in arbitration [:arbitration/date-arbiter-accepted])
   :status (get-in arbitration [:arbitration/status])})

(defn mock-feedback-list []
  [{:rating 1 :text "First job good" :author "Siimar Sapikas" :image-url "https://i.pravatar.cc/300?img=1"}
   {:rating 2 :text "Second job better" :author "John Doe" :image-url "https://i.pravatar.cc/300?img=2"}
   {:rating 3 :text "Third job best" :author "Mary Jane" :image-url "https://i.pravatar.cc/300?img=3"}
   {:rating 4 :text "Fourth job last" :author "Siimar Sapikas" :image-url "https://i.pravatar.cc/300?img=4"}])

(defn c-candidate-profile []
  (let [page-params (re/subscribe [::router-subs/active-page-params])
        query "query ($id: ID!) {
                 user(user_id: $id) {user_name user_profileImage user_country user_languages}
                 candidate(user_id: $id) {
                   candidate_professionalTitle
                   candidate_skills
                   candidate_bio
                   candidate_rating
                   candidate_feedback {
                     items {
                       message_id
                       feedback_text
                       feedback_rating
                       feedback_fromUser {user_name}
                       }
                     }
                   candidate_jobStories {
                     items {
                       job {job_title job_status}
                       jobStory_dateCandidateAccepted
                     }
                   }
                 }
               }"
        results (re/subscribe [::gql/query query {:variables {:id (:address @page-params)}}])]
    (fn []
      (let [name (get-in @results [:user :user/name])
            location (get-in @results [:user :user/country])
            professional-title (get-in @results [:candidate :candidate/professional-title])
            biography (get-in @results [:candidate :candidate/bio])
            image-url (get-in @results [:user :user/profile-image])
            languages (get-in @results [:user :user/languages])
            skills (get-in @results [:user :user/skills])
            job-activity-column-headers {:title "Title" :start-date "Created"}
            jobs (map prepare-jobs (get-in @results [:candidate :candidate/job-stories :items]))
            ; feedback-list (map prepare-feedback-cards (get-in @results [:candidate :candidate/feedback :items]))
            feedback-list (mock-feedback-list)
            rating {:average (get-in @results [:candidate :candidate/rating]) :count (count feedback-list)}]
        [:<>
         [:div.candidate-profile
          [:div.title
           [:div.profile-image
            [c-profile-image {:src image-url}]]
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
         (c-job-activity jobs job-activity-column-headers)
         (c-feedback-listing feedback-list)]))))

(defn c-employer-profile []
  (let [page-params (re/subscribe [::router-subs/active-page-params])
        query "query ($id: ID!) {
                 user(user_id: $id) { user_name user_profileImage user_country user_languages }
                 employer(user_id: $id) {
                   employer_professionalTitle
                   employer_bio
                   employer_rating
                   employer_feedback {
                     items {
                       message_id
                       feedback_text
                       feedback_rating
                       feedback_fromUser {user_name user_profileImage}
                     }
                   }
                   employer_jobStories {
                     items {
                       job {
                         job_title
                         job_status
                       }
                       jobStory_dateCreated}}}}"
        results (re/subscribe [::gql/query query {:variables {:id (:address @page-params)}} ])]
  (fn []
    (let [name (get-in @results [:user :user/name])
          location (get-in @results [:user :user/country])
          professional-title (get-in @results [:employer :employer/professional-title])
          biography (get-in @results [:employer :employer/bio])
          image-url (get-in @results [:user :user/profile-image])
          languages (get-in @results [:user :user/languages])
          job-activity-column-headers {:title "Title" :start-date "Created" :status "Status"}
          jobs (map prepare-employer-jobs (get-in @results [:employer :employer/job-stories :items]))
          ; feedback-list (map prepare-feedback-cards (get-in @results [:employer :employer/feedback :items]))
          feedback-list (mock-feedback-list)
          rating {:average (get-in @results [:employer :employer/rating]) :count (count feedback-list)}]
      [:<>
       [:div.employer-profile
        [:div.title
         [:div.profile-image
          [c-profile-image {:src image-url}]]
         [:div.name name]
         [:div.detail professional-title]]
        [:div.biography biography]
        [c-rating-box rating]
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

       (c-job-activity jobs job-activity-column-headers)
       (c-feedback-listing feedback-list)]))))

(defn c-arbiter-profile []
  (let [page-params (re/subscribe [::router-subs/active-page-params])
        ; query "query ($id: ID!) {
        ;          user(user_id: $id) { user_name user_profileImage user_country user_languages }
        ;          arbiter(user_id: $id) {
        ;            arbiter_professionalTitle
        ;            arbiter_bio
        ;            arbiter_rating
        ;            arbiter_fee
        ;            arbiter_feeCurrencyId
        ;            arbiter_feedback { items { message_id feedback_text feedback_rating feedback_fromUser { user_name } } }
        ;            arbiter_jobStories { items { job { job_title job_status }
        ;                                       jobStory_dateArbiterAccepted } } } }"
        query "query ($id: ID!) {
                 user(user_id: $id) { user_name user_profileImage user_country user_languages }
                 arbiter(user_id: $id) {
                   arbiter_professionalTitle
                   arbiter_bio
                   arbiter_rating
                   arbiter_fee
                   arbiter_feeCurrencyId
                   arbiter_feedback { items { message_id feedback_text feedback_rating feedback_fromUser { user_name } } }
                   arbitrations {
                     items {
                       arbitration_dateArbiterAccepted
                       arbitration_arbiterFee
                       arbitration_arbiterFeeCurrencyId
                       job {
                         job_title
                       }
                     }
                   }
                   arbiter_jobs { items { job_title job_status job_arbiter
                                              jobStory_dateArbiterAccepted } } } }"
        results (re/subscribe [::gql/query query {:variables {:id (:address @page-params)}}])
        ; query {:queries [
        ;                  [:user {:user/id (:address @page-params)}
        ;                 [:user/name
        ;                  :user/profile-image
        ;                  :user/country
        ;                  :user/languages]] ; Up until here the query works
        ;                [:arbiter {:user/id (:address @page-params)}
        ;                 [:arbiter/professional-title
        ;                  :arbiter/bio
        ;                  :arbiter/rating
        ;                  [:arbiter/feedback [:items [:message/id
        ;                                              :feedback/text
        ;                                              :feedback/rating
        ;                                              [:feedback/from-user [:user/name :user/profile-image]]]]

        ;                   ]

        ;                                           ; :arbiter/job-stories [:items [
        ;                  ;                               :job [:job/title
        ;                  ;                                     :job/status]
        ;                  ;                               :job-story/date-candidate-accepted
        ;                  ;                               ]]
        ;                  ]]
        ;                ]}
        ; results (re/subscribe [::gql/query query])
        ]
    (fn []
      (let [name (get-in @results [:user :user/name])
            location (get-in @results [:user :user/country])
            professional-title (get-in @results [:arbiter :arbiter/professional-title])
            biography (get-in @results [:arbiter :arbiter/bio])
            image-url (get-in @results [:user :user/profile-image])
            languages (get-in @results [:user :user/languages])
            arbitration-column-headers {:title "Title" :start-date "Hired" :fee "Fee"}
            arbitrations (map prepare-arbitrations (get-in @results [:arbiter :arbitrations :items]))
            ; feedback-list (map prepare-feedback-cards (get-in @results [:arbiter :arbiter/feedback :items]))
            feedback-list (mock-feedback-list)
            rating {:average (get-in @results [:arbiter :arbiter/rating]) :count (count feedback-list)}]
    [:<>
     [:div.arbiter-profile
      [:div.title
       [:div.profile-image
        [c-profile-image {:src image-url}]]
       [:div.name name]
       [:div.detail professional-title]]
      [:div.biography biography]
      [c-rating-box rating]
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

     (c-job-activity arbitrations arbitration-column-headers)
     (c-feedback-listing feedback-list)]))))

(defmethod page :route.user/profile []
  (let [{:keys [name params query]} @(re/subscribe [::router-subs/active-page])
        user-address (:address params)
        tabs {"candidate" 0 "employer" 1 "arbiter" 2}
        default-tab (get tabs (:tab query) 0)
        navigate-to (fn [tab name params] (when name (re/dispatch [::router-events/navigate name params (merge query {:tab tab})])))
        navigate-to-candidate (partial navigate-to "candidate" name params)
        navigate-to-employer (partial navigate-to "employer" name params)
        navigate-to-arbiter (partial navigate-to "arbiter" name params)
        ]
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
