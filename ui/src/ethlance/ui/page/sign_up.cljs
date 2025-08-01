(ns ethlance.ui.page.sign-up
  (:require
    [clojure.spec.alpha :as s]
    [cuerdas.core :as str]
    [district.ui.component.page :refer [page]]
    [district.ui.graphql.subs :as gql]
    [district.ui.notification.subs :as noti-subs]
    [district.ui.router.events :as router-events]
    [district.ui.router.subs :as router.subs]
    [ethlance.shared.constants :as constants]
    [ethlance.shared.spec :refer [validate-keys]]
    [ethlance.ui.component.currency-input :refer [c-currency-input]]
    [ethlance.ui.component.email-input :refer [c-email-input]]
    [ethlance.ui.component.file-drag-input :refer [c-file-drag-input]]
    [ethlance.ui.component.icon :refer [c-icon]]
    [ethlance.ui.component.main-layout :refer [c-main-layout]]
    [ethlance.ui.component.modal.events]
    [ethlance.ui.component.search-input :refer [c-chip-search-input]]
    [ethlance.ui.component.select-input :refer [c-select-input]]
    [ethlance.ui.component.tabular-layout :refer [c-tabular-layout]]
    [ethlance.ui.component.text-input :refer [c-text-input]]
    [ethlance.ui.component.textarea-input :refer [c-textarea-input]]
    [ethlance.ui.page.sign-up.events :as sign-up.events]
    [ethlance.ui.page.sign-up.subscriptions]
    [ethlance.ui.util.component :refer [>evt]]
    [ethlance.ui.util.urls :as util.urls]
    [re-frame.core :as re]
    [reagent.core :as r]
    [taoensso.timbre :as log]))


(defn- c-upload-image
  []
  (let [form-data (r/atom {})]
    (fn [{:keys [:form-values]}]
      [:div.upload-image
       [c-file-drag-input
        {:form-data form-data
         :id :file-info
         :current-src (-> form-values :user/profile-image util.urls/ipfs-hash->gateway-url)
         :label "Upload file"
         :file-accept-pred (fn [{:keys [name type size]}]
                             (log/debug "Veryfing acceptance of file" {:name name :type type :size size})
                             (and (#{"image/png" "image/gif" "image/jpeg" "image/svg+xml" "image/webp"} type)
                                  (< size 1500000)))
         :on-file-accepted (fn [{:keys [name type size]}]
                             (swap! form-data update-in [:file-info] dissoc :error)
                             (log/info "Accepted file" {:name name :type type :size size} ::file-accepted)
                             (re/dispatch [:page.sign-up/upload-user-image @form-data]))
         :on-file-rejected (fn [{:keys [name type size]}]
                             (swap! form-data assoc :file-info {:error "Non .png .jpeg .gif or .svg file selected with size less than 1.5 Mb"})
                             (log/warn "Rejected file" {:name name :type type :size size} ::file-rejected))}]])))


(defn- c-user-name-input
  [{:keys [:form-values :form-validation]}]
  [:div.form-name
   [c-text-input
    {:placeholder "Name"
     :value (:user/name form-values)
     :error? (not (:user/name form-validation))
     :on-change #(>evt [:page.sign-up/set-user-name %])}]])


(defn- c-user-email-input
  [{:keys [:form-values :form-validation]}]
  [:div.form-email
   [c-email-input
    {:placeholder "Email"
     :value (:user/email form-values)
     :error? (not (:user/email form-validation))
     :on-change #(>evt [:page.sign-up/set-user-email %])}]])


(defn- c-user-country-input
  [{:keys [:form-values]}]
  [:div.form-country
   [c-select-input
    {:label "Select Country"
     :selections constants/countries
     :selection (:user/country form-values)
     :on-select #(>evt [:page.sign-up/set-user-country-code %])
     :search-bar? true
     :default-search-text "Search Countries"}]])


(defn- c-user-languages-input
  [{:keys [:form-values]}]
  [:<>
   [:div.label [:h2 "Languages You Speak"]]
   [c-chip-search-input
    {:search-icon? false
     :placeholder ""
     :auto-suggestion-listing constants/languages
     :allow-custom-chips? false
     :chip-listing (:user/languages form-values)
     :on-chip-listing-change #(>evt [:page.sign-up/set-user-languages %])}]])


(defn- c-bio
  [{:keys [:on-change :value]}]
  [:<>
   [:div.label [:h2 "Your Biography"]]
   [c-textarea-input
    {:placeholder ""
     :value value
     :on-change on-change}]])


(defn- c-submit-button
  [{:keys [:on-submit :disabled?]}]
  [:div.form-submit
   {:class (when disabled? "disabled")
    :on-click (fn [] (when-not disabled? (>evt on-submit)))}
   [:span "Save"]
   [c-icon {:name :ic-arrow-right :size :smaller}]])


(defn c-candidate-sign-up
  []
  (let [user-id (:user/id @(re/subscribe [:ethlance.ui.subscriptions/active-session]))
        candidate-query [:candidate {:user/id user-id} sign-up.events/candidate-fields]
        user-query [:user {:user/id user-id} sign-up.events/user-fields]
        results (re/subscribe [::gql/query {:queries [candidate-query user-query]}])
        sign-up-form (re/subscribe [:page.sign-up/form])
        form-values (merge (get @results :candidate)
                           (get @results :user)
                           @sign-up-form)
        form-validation (validate-keys form-values)]
    [:div.candidate-sign-up
     [:div.form-container
      [:div.label "Sign Up"]
      [:div.first-forms
       [:div.form-image
        [c-upload-image {:form-values form-values}]]
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
          :on-change #(>evt [:page.sign-up/set-candidate-rate (js/parseInt %)])}]]
       [c-user-country-input
        {:form-values (select-keys form-values [:user/country])}]]
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
      {:on-submit [:page.sign-up/update-candidate form-values]
       :disabled? (not (s/valid? :page.sign-up/update-candidate form-values))}]]))


(defn c-employer-sign-up
  []
  (let [user-id (:user/id @(re/subscribe [:ethlance.ui.subscriptions/active-session]))
        employer-query [:employer {:user/id user-id} sign-up.events/employer-fields]
        user-query [:user {:user/id user-id} sign-up.events/user-fields]
        results (re/subscribe [::gql/query {:queries [employer-query user-query]}])
        sign-up-form (re/subscribe [:page.sign-up/form])
        form-values (merge (get @results :employer)
                           (get @results :user)
                           @sign-up-form)
        form-validation (validate-keys form-values)]
    [:div.employer-sign-up
     [:div.form-container
      [:div.label "Sign Up"]
      [:div.first-forms
       [:div.form-image
        [c-upload-image {:form-values form-values}]]
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
        {:form-values form-values}]]
      [:div.second-forms
       [c-user-languages-input
        {:form-values form-values}]
       [c-bio
        {:value (:employer/bio form-values)
         :on-change #(>evt [:page.sign-up/set-employer-bio %])
         :error? (not (:employer/bio form-validation))}]]]
     [c-submit-button
      {:on-submit [:page.sign-up/update-employer form-values]
       :disabled? (not (s/valid? :page.sign-up/update-employer form-values))}]]))


(defn c-arbiter-sign-up
  []
  (let [user-id (:user/id @(re/subscribe [:ethlance.ui.subscriptions/active-session]))
        arbiter-query [:arbiter {:user/id user-id} sign-up.events/arbiter-fields]
        user-query [:user {:user/id user-id} sign-up.events/user-fields]
        results (re/subscribe [::gql/query {:queries [arbiter-query user-query]}])
        sign-up-form (re/subscribe [:page.sign-up/form])
        form-values (merge (get @results :arbiter)
                           (get @results :user)
                           @sign-up-form)
        form-validation (validate-keys form-values)]
    [:div.arbiter-sign-up
     [:div.form-container
      [:div.label "Sign Up"]
      [:div.first-forms
       [:div.form-image
        [c-upload-image {:form-values form-values}]]
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
          :on-change #(>evt [:page.sign-up/set-arbiter-fee (js/parseInt %)])}]]]
      [:div.second-forms
       [c-user-languages-input
        {:form-values form-values}]
       [c-bio
        {:value (:arbiter/bio form-values)
         :on-change #(>evt [:page.sign-up/set-arbiter-bio %])
         :error? (not (:arbiter/bio form-validation))}]]]
     [c-submit-button
      {:on-submit [:page.sign-up/update-arbiter form-values]
       :disabled? (not (s/valid? :page.sign-up/update-arbiter form-values))}]]))


(defn c-api-error-notification
  [message open?]
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
