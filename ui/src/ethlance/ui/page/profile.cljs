(ns ethlance.ui.page.profile
  (:require [district.ui.component.page :refer [page]]
            [ethlance.ui.component.button :refer [c-button c-button-label c-button-icon-label]]
            [ethlance.ui.component.carousel :refer [c-carousel c-feedback-slide]]
            [ethlance.ui.component.circle-button :refer [c-circle-icon-button]]
            [ethlance.ui.component.main-layout :refer [c-main-layout]]
            [ethlance.ui.component.profile-image :refer [c-profile-image]]
            [ethlance.ui.component.rating :refer [c-rating]]
            [ethlance.ui.component.scrollable :refer [c-scrollable]]
            [ethlance.ui.component.table :refer [c-table]]
            [ethlance.ui.component.tabular-layout :refer [c-tabular-layout]]
            [ethlance.ui.component.select-input :refer [c-select-input]]
            [ethlance.ui.component.tag :refer [c-tag c-tag-label]]
            [ethlance.ui.component.textarea-input :refer [c-textarea-input]]
            [district.ui.router.subs :as router-subs]
            [district.ui.router.events :as router-events]
            [ethlance.shared.utils :refer [ilike=]]
            [district.format :as format]
            [cljsjs.graphql]
            [clojure.string :as string]
            [ethlance.ui.util.dates :as util.dates]
            [district.graphql-utils :as utils]
            [district.ui.graphql.subs :as gql]
            [re-frame.core :as re]))

(defn c-tag-list [name tags]
  (let [container [:div {:class (string/lower-case name)} [:span name]]]
    (into container (map #(vector c-tag {} [c-tag-label %]) tags))))

(defn- format-date-looking-column [column value]
  (if (string/ends-with? (name column) "-date")
    (util.dates/formatted-date value)
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

(defn c-invite-to-jobs []
  (let [{:keys [_ params _]} @(re/subscribe [::router-subs/active-page])
        candidate-address (:address params)

        active-user (:user/id @(re/subscribe [:ethlance.ui.subscriptions/active-session]))
        jobs-query [:job-search {:search-params {:creator active-user} :order-by :dateCreated}
                    [[:items [:job/id
                              :job/title
                              :job/date-created

                              [:job-stories [[:items [:job-story/id
                                                      [:invitation-message [:message/id]]
                                                      [:proposal-message [:message/id]]
                                                      [:candidate [:user/id]]]]]]]]]]
        result @(re/subscribe [::gql/query
                               {:queries [jobs-query]}
                               {:id :JobsWithStoriesForInvitationDropdown
                                :refetch-on [:ethlance.ui.page.profile.events/send-invitation-tx-success]}])
        all-jobs (get-in (first result) [:job-search :items] [])
        existing-relation (fn [job]
                            (cond
                              (some #(get-in % [:invitation-message :message/id]) (-> job :job-stories :items))
                              " (you already invited this candidate)"
                              (some #(get-in % [:proposal-message :message/id]) (-> job :job-stories :items))
                              " (candidate already proposed)"))
        jobs (sort-by :job/date-created #(compare %2 %1)
                      (reduce (fn [acc job]
                        (if (some #(ilike= candidate-address (-> % :candidate :user/id)) (-> job :job-stories :items))
                          (conj acc (merge job {:comment (existing-relation job)
                                                :job-story-exists? true}))
                          (conj acc job)))
                      []
                      all-jobs))
        job-for-invitation (re/subscribe [:page.profile/job-for-invitation])
        invitation-text (re/subscribe [:page.profile/invitation-text])
        preselected-job (or @job-for-invitation (first jobs))
        job-story-exists? (:job-story-exists? preselected-job)]
    [:div.job-listing
      [:div.title "Invite to a job"]
      [c-select-input
       {:selections jobs
        :value-fn :job/id
        :label-fn #(str (:job/title %) (:comment %))
        :selection preselected-job
        :on-select #(re/dispatch [:page.profile/set-job-for-invitation %])}]
      [c-textarea-input {:value @invitation-text
                         :disabled job-story-exists?
                         :placeholder "Briefly describe to what and why you're inviting the candidate"
                         :on-change #(re/dispatch [:page.profile/set-invitation-text %])}]
      [c-button {:color :primary
                 :disabled? job-story-exists?
                 :on-click (fn []
                             (when-not job-story-exists?
                               (re/dispatch [:page.profile/send-invitation
                                            {:candidate candidate-address
                                             :text @invitation-text
                                             :job preselected-job
                                             :employer active-user}])))}
        [c-button-label "Invite"]]]))

(defn c-rating-box [rating]
  [:div.rating
   [c-rating {:rating (:average rating) :color :primary}]
   [:span (str "(" (:count rating) ")")]])

(defn c-feedback-listing [sub-title feedback-list]
  [:div.feedback-listing
      [:div.title "Feedback"]
      [:div.sub-title sub-title]
      (into [c-carousel {}] (map #(c-feedback-slide %) feedback-list))])

(def log (.-log js/console))

(defn prepare-ratings [rating]
  {:rating (:feedback/rating rating)
   :from (get-in rating [:feedback/from-user :user/name])
   :text (:feedback/text rating)})

(defn prepare-feedback-cards [item]
  {:rating (:feedback/rating item)
   :text (:feedback/text item)
   :image-url (-> item :feedback/from-user :user/profile-image)
   :author (get-in item [:feedback/from-user :user/name])})

(defn prepare-candidate-jobs [story]
  {:title (get-in story [:job :job/title])
   :start-date (get-in story [:job-story/date-contract-active])
   :status (get-in story [:job-story/status])})

(defn prepare-employer-jobs [story]
  {:title (get-in story [:job :job/title])
   :start-date (get-in story [:job-story/date-created])
   :status (get-in story [:job :job/status])})

(defn prepare-arbitrations [arbitration]
  {:title (get-in arbitration [:job :job/title])
   :start-date (get-in arbitration [:arbitration/date-arbiter-accepted]) ;
   :fee (str (get-in arbitration [:arbitration/fee]) " " (get-in arbitration [:arbitration/fee-currency-id]))
   :status (get-in arbitration [:arbitration/status])})

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
                       feedback_fromUser {user_name user_profileImage}
                       }
                     }
                   jobStories {
                     items {
                       job {job_title job_status}
                       jobStory_dateContractActive
                       jobStory_status
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
            skills (get-in @results [:candidate :candidate/skills])
            job-activity-column-headers {:title "Title" :start-date "Created" :status "Status"}
            jobs (map prepare-candidate-jobs (get-in @results [:candidate :job-stories :items]))
            feedback-list (map prepare-feedback-cards (get-in @results [:candidate :candidate/feedback :items]))
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
         [c-invite-to-jobs]
         (c-feedback-listing professional-title feedback-list)]))))

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
                   jobStories {
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
          jobs (map prepare-employer-jobs (get-in @results [:employer :job-stories :items]))
          feedback-list (map prepare-feedback-cards (get-in @results [:employer :employer/feedback :items]))
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
       (c-feedback-listing professional-title feedback-list)]))))

(defn c-arbiter-profile []
  (let [page-params (re/subscribe [::router-subs/active-page-params])
        query [:arbiter {:user/id (:address @page-params)}
               [:arbiter/professional-title
                :arbiter/bio
                :arbiter/rating
                :arbiter/fee
                :arbiter/fee-currency-id
                [:user
                 [:user/name
                  :user/profile-image
                  :user/country
                  :user/languages]]
                [:arbitrations
                 [:total-count
                  [:items
                   [:id
                    :arbitration/date-arbiter-accepted
                   :arbitration/fee
                   :arbitration/fee-currency-id
                   :arbitration/status
                   [:job
                    [:job/title]]]]]]
                [:arbiter/feedback
                 [:total-count
                  [:items
                  [:message/id
                   :feedback/text
                   :feedback/rating
                   [:feedback/from-user
                    [:user/name
                     :user/profile-image]]]]]]]]
        results (re/subscribe [::gql/query {:queries [query]}])]
    (fn []
      (let [name (get-in @results [:user :user/name])
            location (get-in @results [:user :user/country])
            professional-title (get-in @results [:arbiter :arbiter/professional-title])
            biography (get-in @results [:arbiter :arbiter/bio])
            image-url (get-in @results [:user :user/profile-image])
            languages (get-in @results [:user :user/languages])
            arbitration-column-headers {:title "Title" :start-date "Hired" :fee "Fee" :status "Status"}
            arbitrations (map prepare-arbitrations (get-in @results [:arbiter :arbitrations :items]))
            feedback-list (map prepare-feedback-cards (get-in @results [:arbiter :arbiter/feedback :items]))
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
     (c-feedback-listing professional-title feedback-list)]))))

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
