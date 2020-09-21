(ns ethlance.ui.page.sign-up
  (:require [cuerdas.core :as str]
            [district.ui.component.page :refer [page]]
            [ethlance.ui.subscriptions :as subs]
            [district.ui.router.subs :as router.subs]
            [district.ui.router.events :as router-events]
            [district.ui.web3-accounts.subs :as accounts-subs]
            [ethlance.shared.constants :as constants]
            [ethlance.ui.component.button :refer [c-button c-button-icon-label]]
            [ethlance.ui.component.checkbox :refer [c-labeled-checkbox]]
            [ethlance.ui.component.currency-input :refer [c-currency-input]]
            [ethlance.ui.component.email-input :refer [c-email-input]]
            [ethlance.ui.component.file-drag-input :refer [c-file-drag-input]]
            [ethlance.ui.component.icon :refer [c-icon]]
            [ethlance.ui.component.main-layout :refer [c-main-layout]]
            [ethlance.ui.component.search-input :refer [c-chip-search-input]]
            [ethlance.ui.component.select-input :refer [c-select-input]]
            [ethlance.ui.component.tabular-layout :refer [c-tabular-layout]]
            [ethlance.ui.component.text-input :refer [c-text-input]]
            [ethlance.ui.component.textarea-input :refer [c-textarea-input]]
            [ethlance.ui.event.sign-up :as event.sign-up]
            [ethlance.ui.util.component :refer [>evt <sub]]
            [re-frame.core :as re]
            [reagent.core :as r]
            [taoensso.timbre :as log]))

(defn- c-upload-image []
  (let [form-data (r/atom {})]
    (fn []
      [:div.upload-image
       [c-file-drag-input {:form-data form-data
                           :id :file-info
                           :label "Upload file"
                           :file-accept-pred (fn [{:keys [name type size] :as props}]
                                               (log/debug "Veryfing acceptance of file" {:name name :type type :size size})
                                               (and (#{"image/png" "image/gif" "image/jpeg" "image/svg+xml" "video/mp4"} type)
                                                    (< size 1500000)))
                           :on-file-accepted (fn [{:keys [name type size array-buffer] :as props}]
                                               (swap! form-data update-in [:file-info] dissoc :error)
                                               (log/info "Accepted file" {:name name :type type :size size} ::file-accepted)
                                               (re/dispatch [::event.sign-up/upload-user-image @form-data]))
                           :on-file-rejected (fn [{:keys [name type size] :as props}]
                                               (swap! form-data assoc :file-info {:error "Non .png .jpeg .gif .svg or .mp4 file selected with size less than 1.5 Mb"})
                                               (log/warn "Rejected file" {:name name :type type :size size} ::file-rejected))}]])))

(defn c-candidate-sign-up []
  (let [config (<sub [::subs/config])
        user (re/subscribe [::subs/user])
        candidate (re/subscribe [::subs/candidate])
        gh-client-id (-> config :github :client-id)
        active-page (<sub [::router.subs/active-page])]
    (r/create-class
     {:display-name "c-candidate-sign-up"
      :component-did-mount (fn []
                             (when-let [code (-> active-page :query :code)]
                               (>evt [:page.sign-up/send-github-verification-code code])))
      :reagent-render
      (fn []
        (let [{:user/keys [user-name github-username email country-code languages is-registered-candidate]} @user
              {:candidate/keys [rate professional-title categories skills bio for-hire?]} @candidate]
          [:div.candidate-sign-up
           [:div.form-container
            [:div.label "Sign Up"]
            [:div.first-forms
             [:div.form-image
              [c-upload-image]]
             [:div.form-name
              [c-text-input
               {:placeholder "Name"
                :value (or user-name github-username)
                :on-change #(>evt [:page.sign-up/set-user-name %])}]]
             [:div.form-email
              [c-email-input
               {:placeholder "Email"
                :value email
                :on-change #(>evt [:page.sign-up/set-user-email %])}]]
             [:div.form-professional-title
              [c-text-input
               {:placeholder "Professional Title"
                :value professional-title
                :on-change #(>evt [:page.sign-up/set-candidate-professional-title %])}]]
             [:div.form-hourly-rate
              [c-currency-input
               {:placeholder "Hourly Rate"
                :color :primary
                :min 0
                :value rate
                :on-change #(>evt [:page.sign-up/set-candidate-hourly-rate %])}]]
             [:div.form-country
              [c-select-input
               {:label "Select Country"
                :selections constants/countries
                :selection country-code
                :on-select #(>evt [:page.sign-up/set-user-country-code %])
                :search-bar? true
                :default-search-text "Search Countries"}]]
             [:div.form-connect-github
              [c-button
               {:size :large
                :disabled? (not (nil? github-username))
                :href (str "https://github.com/login/oauth/authorize?client_id=" gh-client-id "&scope=user"
                           ;; "&redirect_uri=" root-url
                           active-page)}
               [c-button-icon-label {:icon-name :github :label-text "Connect Github" :inline? false}]]]
             [:div.form-connect-linkedin
              [c-button
               {:size :large}
               [c-button-icon-label {:icon-name :linkedin :label-text "Connect LinkedIn" :inline? false}]]]]
            [:div.second-forms
             [:div.label [:h2 "Languages You Speak"]]
             [c-chip-search-input
              {:search-icon? false
               :placeholder ""
               :auto-suggestion-listing constants/languages
               :allow-custom-chips? false
               :chip-listing languages
               :on-chip-listing-change #(>evt [:page.sign-up/set-user-languages %])}]

             [:div.label [:h2 "Categories You Are Interested In"]]
             [c-chip-search-input
              {:search-icon? false
               :placeholder ""
               :auto-suggestion-listing (sort constants/categories)
               :allow-custom-chips? false
               :chip-listing categories
               :on-chip-listing-change #(>evt [:page.sign-up/set-candidate-categories %])
               :display-listing-on-focus? true}]

             [:div.label [:h2 "Your Skills "] [:i "(Choose at least one skill)"]]
             [c-chip-search-input
              {:search-icon? false
               :placeholder ""
               :allow-custom-chips? false
               :auto-suggestion-listing constants/skills
               :chip-listing skills
               :on-chip-listing-change #(>evt [:page.sign-up/set-candidate-skills %])}]

             [:div.label [:h2 "Your Biography"]]
             [c-textarea-input
              {:placeholder ""
               :value bio
               :on-change #(>evt [:page.sign-up/set-candidate-bio %])}]
             [c-labeled-checkbox
              {:id "form-for-hire"
               :label "I'm available for hire"
               :checked? for-hire?
               :on-change #(>evt [:page.sign-up/set-candidate-for-hire? %])}]]]
           ;; TODO : active / inactive (based on required fields)
           [:div.form-submit {:on-click #(>evt [:page.sign-up/update-candidate])}
            [:span "Create"]
            [c-icon {:name :ic-arrow-right :size :smaller}]]]))})))

;; TODO
(defn c-employer-sign-up []
  (let [config (<sub [::subs/config])
        user (re/subscribe [::subs/user])
        employer (re/subscribe [::subs/employer])
        gh-client-id (-> config :github :client-id)
        active-page (<sub [::router.subs/active-page])

        ]
    (r/create-class
     {:display-name "c-employer-sign-up"
      :component-did-mount (fn []
                             (when-let [code (-> active-page :query :code)]
                               (>evt [:page.sign-up/send-github-verification-code code])))
      :reagent-render
      (fn []
        ;; TODO
        (let [{:user/keys [user-name github-username email country-code languages is-registered-candidate]} @user
              {:employer/keys [bio professional-title]} @employer
              ]
          [:div.employer-sign-up
           [:div.form-container
            [:div.label "Sign Up"]
            [:div.first-forms
             [:div.form-image
              [c-upload-image]]
             [:div.form-name
              [c-text-input
               {:placeholder "Name"
                :value (or user-name github-username)

                :on-change #(>evt [:page.sign-up/set-user-name %])

                }]]
             [:div.form-email
              [c-email-input
               {:placeholder "Email"
                :value email
                :on-change #(>evt [:page.sign-up/set-user-email %])}]]

             [:div.form-professional-title
              [c-text-input
               {:placeholder "Professional Title"
                :value professional-title
                :on-change #(>evt [:page.sign-up/set-employer-professional-title %])}]]

             [:div.form-country
              [c-select-input
               {:label "Select Country"
                :selections constants/countries
                :selection country-code
                :on-select #(>evt [:page.sign-up/set-user-country-code %])
                :search-bar? true
                :default-search-text "Search Countries"}]]

             [:div.form-connect-github
              [c-button
               {:size :large}
               [c-button-icon-label {:icon-name :github :label-text "Connect Github" :inline? false}]]]

             [:div.form-connect-linkedin
              [c-button
               {:size :large
                :disabled? (not (nil? github-username))
                :href (str "https://github.com/login/oauth/authorize?client_id=" gh-client-id "&scope=user"
                           ;; "&redirect_uri=" root-url
                           active-page)
                }
               [c-button-icon-label {:icon-name :linkedin :label-text "Connect LinkedIn" :inline? false}]]]]

            [:div.second-forms
             [:div.label [:h2 "Languages You Speak"]]
             [c-chip-search-input
              {:search-icon? false
               :placeholder ""
               :auto-suggestion-listing constants/languages
               :allow-custom-chips? false
               :chip-listing languages
               :on-chip-listing-change #(>evt [:page.sign-up/set-user-languages %])}]

             [:div.label [:h2 "Your Biography"]]
             [c-textarea-input
              {:placeholder ""
               :value bio
               :on-change #(>evt [:page.sign-up/set-employer-bio %])}]]]

           [:div.form-submit
            [:span "Create"]
            [c-icon {:name :ic-arrow-right :size :smaller}]]]))})))


(defn c-arbiter-sign-up []
  (let [*full-name (re/subscribe [:page.sign-up/arbiter-full-name])
        *professional-title (re/subscribe [:page.sign-up/arbiter-professional-title])
        *fixed-rate-per-dispute (re/subscribe [:page.sign-up/arbiter-fixed-rate-per-dispute])
        *email (re/subscribe [:page.sign-up/arbiter-email])
        *github-key (re/subscribe [:page.sign-up/arbiter-github-key])
        *linkedin-key (re/subscribe [:page.sign-up/arbiter-linkedin-key])
        *languages (re/subscribe [:page.sign-up/arbiter-languages])
        *biography (re/subscribe [:page.sign-up/arbiter-biography])
        *country (re/subscribe [:page.sign-up/arbiter-country])]
    (fn []
      [:div.arbiter-sign-up
       [:div.form-container
        [:div.label "Sign Up"]
        [:div.first-forms
         [:div.form-image
          [c-upload-image]]
         [:div.form-name
          [c-text-input
           {:placeholder "Name"
            :value @*full-name
            :on-change #(re/dispatch [:page.sign-up/set-arbiter-full-name %])}]]
         [:div.form-email
          [c-email-input
           {:placeholder "Email"
            :value @*email
            :on-change #(re/dispatch [:page.sign-up/set-arbiter-email %])}]]
         [:div.form-professional-title
          [c-text-input
           {:placeholder "Professional Title"
            :value @*professional-title
            :on-change #(re/dispatch [:page.sign-up/set-arbiter-professional-title %])}]]
         [:div.form-country
          [c-select-input
           {:label "Select Country"
            :selections constants/countries
            :selection @*country
            :on-select #(re/dispatch [:page.sign-up/set-arbiter-country %])
            :search-bar? true
            :default-search-text "Search Countries"}]]
         [:div.form-hourly-rate
          [c-currency-input
           {:placeholder "Fixed Rate Per A Dispute" :color :primary
            :value @*fixed-rate-per-dispute
            :on-change #(re/dispatch [:page.sign-up/set-arbiter-fixed-rate-per-dispute %])}]]
         [:div.form-connect-github
          [c-button
           {:size :large}
           [c-button-icon-label {:icon-name :github :label-text "Connect Github" :inline? false}]]]
         [:div.form-connect-linkedin
          [c-button
           {:size :large}
           [c-button-icon-label {:icon-name :linkedin :label-text "Connect LinkedIn" :inline? false}]]]]

        [:div.second-forms
         [:div.label [:h2 "Languages You Speak"]]
         [c-chip-search-input
          {:search-icon? false
           :placeholder ""
           :auto-suggestion-listing constants/languages
           :allow-custom-chips? false
           :on-chip-listing-change (fn [languages] (log/info "Languages: " languages))}]

         [:div.label [:h2 "Your Biography"]]
         [c-textarea-input {:placeholder ""}]]]

       [:div.form-submit
        [:span "Create"]
        [c-icon {:name :ic-arrow-right :size :smaller}]]])))

(defmethod page :route.me/sign-up []
  (let [active-page (re/subscribe [::router.subs/active-page])]
    (fn []
      (let [{:keys [name params query] :as q} @active-page
            active-tab-index (case (-> query :tab str/lower str/keyword)
                               :candidate 0
                               :employer 1
                               :arbiter 2
                               0)]
        [c-main-layout {:container-opts {:class :sign-up-main-container}}
         [c-tabular-layout
          {:key "sign-up-tabular-layout"
           :default-tab active-tab-index}

          {:label "Candidate"
           :on-click (fn []
                       (when name
                         (re/dispatch [::router-events/navigate name params (merge query {:tab :candidate})])))}
          [c-candidate-sign-up]

          {:label "Employer"
           :on-click (fn []
                       (when name
                         (re/dispatch [::router-events/navigate name params (merge query {:tab :employer})])))}
          [c-employer-sign-up]

          {:label "Arbiter"
           :on-click (fn []
                       (when name
                         (re/dispatch [::router-events/navigate name params (merge query {:tab :arbiter})])))}
          [c-arbiter-sign-up]]]))))
