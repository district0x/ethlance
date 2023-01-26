(ns ethlance.ui.page.sign-up
  (:require
    [cuerdas.core :as str]
    [district.ui.component.page :refer [page]]
    [district.ui.router.events :as router-events]
    [district.ui.notification.subs :as noti-subs]
    [district.ui.router.subs :as router.subs]
    [ethlance.shared.constants :as constants]
    [ethlance.shared.spec :refer [validate-keys]]
    [ethlance.ui.component.button :refer [c-button c-button-icon-label]]
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
    [ethlance.ui.page.sign-up.subscriptions]
    [ethlance.ui.subscriptions :as subs]
    [ethlance.ui.util.component :refer [<sub >evt]]
    [ethlance.ui.util.navigation :as navigation-utils]
    [re-frame.core :as re]
    [reagent.core :as r]
    [taoensso.timbre :as log]
    [clojure.spec.alpha :as s]))


(defn- c-upload-image []
  (let [form-data (r/atom {})]
    (fn []
      [:div.upload-image
       [c-file-drag-input {:form-data form-data
                           :id :file-info
                           :label "Upload file"
                           :file-accept-pred (fn [{:keys [name type size]}]
                                               (log/debug "Veryfing acceptance of file" {:name name :type type :size size})
                                               (and (#{"image/png" "image/gif" "image/jpeg" "image/svg+xml"} type)
                                                    (< size 1500000)))
                           :on-file-accepted (fn [{:keys [name type size]}]
                                               (swap! form-data update-in [:file-info] dissoc :error)
                                               (log/info "Accepted file" {:name name :type type :size size} ::file-accepted)
                                               (re/dispatch [:page.sign-up/upload-user-image @form-data]))
                           :on-file-rejected (fn [{:keys [name type size]}]
                                               (swap! form-data assoc :file-info {:error "Non .png .jpeg .gif or .svg file selected with size less than 1.5 Mb"})
                                               (log/warn "Rejected file" {:name name :type type :size size} ::file-rejected))}]])))


(defn- c-user-name-input [{:keys [:form-values :form-validation]}]
  [:div.form-name
   [c-text-input
    {:placeholder "Name"
     :value (:user/name form-values)
     :error? (not (:user/name form-validation))
     :on-change #(>evt [:page.sign-up/set-user-name %])}]])


(defn- c-user-email-input [{:keys [:form-values :form-validation]}]
  [:div.form-email
   [c-email-input
    {:placeholder "Email"
     :value (:user/email form-values)
     :error? (not (:user/email form-validation))
     :on-change #(>evt [:page.sign-up/set-user-email %])}]])


(defn- c-user-country-input [{:keys [:form-values]}]
  [:div.form-country
   [c-select-input
    {:label "Select Country"
     :selections constants/countries
     :selection (:user/country form-values)
     :on-select #(>evt [:page.sign-up/set-user-country-code %])
     :search-bar? true
     :default-search-text "Search Countries"}]])


(defn- c-user-github-input [{:keys [:form-values :gh-client-id :root-url]}]
  [:div.form-connect-github
   [c-button
    {:size :large
     :disabled? (not (nil? (:user/github-username form-values)))
     :href (str "https://github.com/login/oauth/authorize?"
                "client_id=" gh-client-id
                "&scope=user"
                "&redirect_uri="
                (navigation-utils/url-encode (str root-url "/me/sign-up?tab=candidate&social=github")))}
    [c-button-icon-label {:icon-name :github :label-text "Connect Github" :inline? false}]]])


(defn- c-user-linkedin-input [{:keys [:form-values :linkedin-client-id :root-url]}]
  [:div.form-connect-linkedin
   [c-button
    {:size :large
     :disabled? (not (nil? (:user/linkedin-username form-values)))
     :href (str "https://www.linkedin.com/oauth/v2/authorization?"
                "client_id=" linkedin-client-id
                "&scope=r_liteprofile%20r_emailaddress"
                "&response_type=code"
                "&redirect_uri="
                (navigation-utils/url-encode (str root-url "/me/sign-up?tab=candidate&social=linkedin")))}
    [c-button-icon-label {:icon-name :linkedin :label-text "Connect LinkedIn" :inline? false}]]])


(defn- c-user-languages-input [{:keys [:form-values]}]
  [:<>
   [:div.label [:h2 "Languages You Speak"]]
   [c-chip-search-input
    {:search-icon? false
     :placeholder ""
     :auto-suggestion-listing constants/languages
     :allow-custom-chips? false
     :chip-listing (:user/languages form-values)
     :on-chip-listing-change #(>evt [:page.sign-up/set-user-languages %])}]])


(defn- c-bio [{:keys [:on-change :value]}]
  [:<>
   [:div.label [:h2 "Your Biography"]]
   [c-textarea-input
    {:placeholder ""
     :value value
     :on-change on-change}]])


(defn- c-submit-button [{:keys [:on-submit :disabled?]}]
  (let [in-progress @(re/subscribe [::subs/api-request-in-progress])
        disabled? (or disabled? in-progress)]
    [:div.form-submit
     {:class (when disabled? "disabled")
      :on-click (fn [] (when-not disabled? (>evt on-submit)))}
     [:span "Save"]
     [c-icon {:name :ic-arrow-right :size :smaller}]]))

(defn c-candidate-sign-up []
  (let [{:keys [root-url github linkedin]} (<sub [::subs/config])
        active-user (re/subscribe [::subs/active-user])
        active-candidate (re/subscribe [::subs/active-candidate])
        sign-up-form (re/subscribe [:page.sign-up/form])
        gh-client-id (:client-id github)
        linkedin-client-id (:client-id linkedin)
        {:keys [query]} (<sub [::router.subs/active-page])]
    (r/create-class
      {:display-name "c-candidate-sign-up"
       :component-did-mount (fn []
                              (when-let [code (:code query)]
                                (case (-> query :social keyword)
                                  :github
                                  (>evt [:page.sign-up/send-github-verification-code code])
                                  :linkedin
                                  (>evt [:page.sign-up/send-linkedin-verification-code code (str root-url "/me/sign-up?tab=candidate&social=linkedin")])
                                  nil)))
       :reagent-render
       (fn []
         (let [form-values (merge @active-user @active-candidate @sign-up-form)
               form-validation (validate-keys form-values)]
           [:div.candidate-sign-up
            [:div.form-container
             [:div.label "Sign Up"]
             [:div.first-forms
              [:div.form-image
               [c-upload-image]]
              [c-user-name-input
               {:form-values form-values
                :form-validation form-validation}]
              [c-user-email-input
               {:form-values form-values
                :form-validation form-validation}]
              [:div.form-professional-title
               [c-text-input
                {:placeholder "Professional Title"
                 :value (:candidate/professional-title form-values)
                 :error? (not (:candidate/professional-title form-validation))
                 :on-change #(>evt [:page.sign-up/set-candidate-professional-title %])}]]
              [:div.form-hourly-rate
               [c-currency-input
                {:placeholder "Hourly Rate"
                 :color :primary
                 :min 0
                 :value (:candidate/rate form-values)
                 :error? (not (:candidate/rate form-validation))
                 :on-change #(>evt [:page.sign-up/set-candidate-rate %])}]]
              [c-user-country-input
               {:form-values form-values}]
              [c-user-github-input
               {:form-values form-values
                :gh-client-id gh-client-id
                :root-url root-url}]
              [c-user-linkedin-input
               {:form-values form-values
                :linkedin-client-id linkedin-client-id
                :root-url root-url}]]
             [:div.second-forms
              [c-user-languages-input
               {:form-values form-values}]
              [:div.label [:h2 "Categories You Are Interested In"]]
              [c-chip-search-input
               {:search-icon? false
                :placeholder ""
                :auto-suggestion-listing constants/categories
                :allow-custom-chips? false
                :chip-listing (:candidate/categories form-values)
                :on-chip-listing-change #(>evt [:page.sign-up/set-candidate-categories %])
                :display-listing-on-focus? true}]
              [:div.label [:h2 "Your Skills "] [:i "(Choose at least one skill)"]]
              [c-chip-search-input
               {:search-icon? false
                :placeholder ""
                :allow-custom-chips? false
                :auto-suggestion-listing constants/skills
                :chip-listing (:candidate/skills form-values)
                :on-chip-listing-change #(>evt [:page.sign-up/set-candidate-skills %])}]
              [c-bio
               {:value (:candidate/bio form-values)
                :on-change #(>evt [:page.sign-up/set-candidate-bio %])
                :error? (not (:candidate/bio form-validation))}]]]
            [c-submit-button
             {:on-submit [:page.sign-up/update-candidate]
              :disabled? (not (s/valid? :page.sign-up/update-candidate form-values))}]]))})))


(defn c-employer-sign-up []
  (let [{:keys [root-url github linkedin]} (<sub [::subs/config])
        active-user (re/subscribe [::subs/active-user])
        active-employer (re/subscribe [::subs/active-employer])
        sign-up-form (re/subscribe [:page.sign-up/form])
        gh-client-id (-> github :client-id)
        linkedin-client-id (:client-id linkedin)
        {:keys [query]} (<sub [::router.subs/active-page])]
    (r/create-class
      {:display-name "c-employer-sign-up"
       :component-did-mount (fn []
                              (when-let [code (-> query :code)]
                                (case (-> query :social keyword)
                                  :github
                                  (>evt [:page.sign-up/send-github-verification-code code])
                                  :linkedin
                                  (>evt [:page.sign-up/send-linkedin-verification-code code (str root-url "/me/sign-up?tab=employer&social=linkedin")])
                                  nil)))
       :reagent-render
       (fn []
         (let [form-values (merge @active-user @active-employer @sign-up-form)
               form-validation (validate-keys form-values)]
           [:div.employer-sign-up
            [:div.form-container
             [:div.label "Sign Up"]
             [:div.first-forms
              [:div.form-image
               [c-upload-image]]
              [c-user-name-input
               {:form-values form-values
                :form-validation form-validation}]
              [c-user-email-input
               {:form-values form-values
                :form-validation form-validation}]
              [:div.form-professional-title
               [c-text-input
                {:placeholder "Professional Title"
                 :value (:employer/professional-title form-values)
                 :on-change #(>evt [:page.sign-up/set-employer-professional-title %])}]]
              [c-user-country-input
               {:form-values form-values}]
              [c-user-github-input
               {:form-values form-values
                :gh-client-id gh-client-id
                :root-url root-url}]
              [c-user-linkedin-input
               {:form-values form-values
                :linkedin-client-id linkedin-client-id
                :root-url root-url}]]
             [:div.second-forms
              [c-user-languages-input
               {:form-values form-values}]
              [c-bio
               {:value (:employer/bio form-values)
                :on-change #(>evt [:page.sign-up/set-employer-bio %])
                :error? (not (:employer/bio form-validation))}]]]
            [c-submit-button
             {:on-submit [:page.sign-up/update-employer]
              :disabled? (not (s/valid? :page.sign-up/update-employer form-values))}]]))})))

(defn c-arbiter-sign-up []
  (let [{:keys [root-url github linkedin]} (<sub [::subs/config])
        active-user (re/subscribe [::subs/active-user])
        active-arbiter (re/subscribe [::subs/active-arbiter])
        sign-up-form (re/subscribe [:page.sign-up/form])
        gh-client-id (-> github :client-id)
        linkedin-client-id (:client-id linkedin)
        {:keys [query]} (<sub [::router.subs/active-page])]
    (r/create-class
      {:display-name "c-arbiter-sign-up"
       :component-did-mount (fn []
                              (when-let [code (:code query)]
                                (case (-> query :social keyword)
                                  :github
                                  (>evt [:page.sign-up/send-github-verification-code code])
                                  :linkedin
                                  (>evt [:page.sign-up/send-linkedin-verification-code code (str root-url "/me/sign-up?tab=arbiter&social=linkedin")])
                                  nil)))
       :reagent-render
       (fn []
         (let [form-values (merge @active-user @active-arbiter @sign-up-form)
               form-validation (validate-keys form-values)]
           [:div.arbiter-sign-up
            [:div.form-container
             [:div.label "Sign Up"]
             [:div.first-forms
              [:div.form-image
               [c-upload-image]]
              [c-user-name-input
               {:form-values form-values
                :form-validation form-validation}]
              [c-user-email-input
               {:form-values form-values
                :form-validation form-validation}]
              [:div.form-professional-title
               [c-text-input
                {:placeholder "Professional Title"
                 :value (:arbiter/professional-title form-values)
                 :on-change #(>evt [:page.sign-up/set-arbiter-professional-title %])}]]
              [c-user-country-input
               {:form-values form-values}]
              [:div.form-hourly-rate
               [c-currency-input
                {:placeholder "Fixed Rate Per A Dispute" :color :primary
                 :value (:arbiter/fee form-values)
                 :on-change #(>evt [:page.sign-up/set-arbiter-fee %])}]]
              [c-user-github-input
               {:form-values form-values
                :gh-client-id gh-client-id
                :root-url root-url}]
              [c-user-linkedin-input
               {:form-values form-values
                :linkedin-client-id linkedin-client-id
                :root-url root-url}]]
             [:div.second-forms
              [c-user-languages-input
               {:form-values form-values}]
              [c-bio
               {:value (:arbiter/bio form-values)
                :on-change #(>evt [:page.sign-up/set-arbiter-bio %])
                :error? (not (:arbiter/bio form-validation))}]]]
            [c-submit-button
             {:on-submit [:page.sign-up/update-arbiter]
              :disabled? (not (s/valid? :page.sign-up/update-arbiter form-values))}]]))})))

(defn c-api-error-notification [message open?]
  [:div {:class ["notification-box" (when (not open?) "hidden")]}
    [:div {:class ["ui negative message"]}
     [:div {:class "header"} "Error"]
     [:p message]]])

(defmethod page :route.me/sign-up []
  (let [active-page (re/subscribe [::router.subs/active-page])]
    (fn []
      (let [{:keys [name params query]} @active-page
            active-tab-index (case (-> query :tab str/lower str/keyword)
                               :candidate 0
                               :employer 1
                               :arbiter 2
                               0)
            {:keys [:open? :message]} @(re/subscribe [::noti-subs/notification])]
        [c-main-layout {:container-opts {:class :sign-up-main-container}}
         [c-api-error-notification message open?]
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
