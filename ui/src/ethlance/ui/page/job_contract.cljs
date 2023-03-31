(ns ethlance.ui.page.job-contract
  (:require [district.parsers :refer [parse-int]]
            [district.ui.component.page :refer [page]]
            [district.ui.router.subs :as router.subs]
            [ethlance.shared.utils :refer [ilike=]]
            [district.ui.graphql.subs :as gql]
            [ethlance.ui.component.button :refer [c-button c-button-label]]
            [ethlance.ui.component.chat :refer [c-chat-log]]
            [ethlance.ui.component.main-layout :refer [c-main-layout]]
            [ethlance.ui.component.radio-select :refer [c-radio-secondary-element c-radio-select]]
            [ethlance.ui.component.rating :refer [c-rating]]
            [ethlance.ui.component.tabular-layout :refer [c-tabular-layout]]
            [ethlance.ui.component.textarea-input :refer [c-textarea-input]]
            [ethlance.ui.util.navigation :as util.navigation]
            [re-frame.core :as re]))

(defn profile-link-handlers [user-type address]
  {:on-click (util.navigation/create-handler
               {:route :route.user/profile
                :params {:address address}
                :query {:tab user-type}})
   :href (util.navigation/resolve-route
           {:route :route.user/profile
            :params {:address address}
            :query {:tab user-type}})})

(defn c-job-detail-table
  [{:keys [status funds employer candidate arbiter]}]
  [:div.job-detail-table

   [:div.name "Status"]
   [:div.value status]

   [:div.name "Funds Available"]
   [:div.value (str (:amount funds) " " (:symbol funds))]

   [:div.name "Employer"]
   [:a.value (profile-link-handlers :employer (:address employer)) (:name employer)]

   [:div.name "Candidate"]
   [:a.value (profile-link-handlers :candidate (:address candidate)) (:name candidate)]

   [:div.name "Arbiter"]
   [:a.value (profile-link-handlers :arbiter (:address arbiter)) (:name arbiter)]])

(defn c-header-profile
  [{:keys [title] :as details}]
  [:div.header-profile
   [:div.title "Job Contract"]
   [:div.job-name title]
   [:div.job-details
    [c-job-detail-table details]]])

(defn common-chat-fields [current-user job-story field-fn details]
  (let [direction (fn [viewer creator]
                    (println ">>> common-chat-fields comparing" {:viewer viewer :creator creator :details details})
                    (if (ilike= viewer creator)
                      :sent :received))
        message (-> job-story field-fn)]
    (when message
      {:id (str "dispute-creation-msg-" (-> message :message/id))
       :direction (direction current-user (-> message :creator :user/id))
       :text (-> message :message/text)
       :full-name (-> message :creator :user/name)
       :timestamp (-> message :message/date-created)
       :image-url (-> message :creator :user/profile-image)
       :details details})))

(defn extract-chat-messages [job-story current-user]
  (let [job-story-id (-> job-story :job-story :job-story/id)
        add-to-details (fn [message additional-detail]
                         (when message
                           (assoc message :details (conj (:details message) additional-detail))))
        format-proposal-amount (fn [job-story]
                                 (let [amount (-> job-story :job-story/proposal-rate)
                                       token-name (-> job-story :job :token-details :token-detail/name)
                                       token-symbol (-> job-story :job :token-details :token-detail/symbol)]
                                   (str amount " " token-symbol " (" token-name ")")))
        common-fields (partial common-chat-fields current-user job-story)
        dispute-creation (-> (common-fields :dispute-creation-message ["Dispute was created"]))
        dispute-resolution (common-fields :dispute-resolution-message ["Dispute was resolved"])
        invitation (common-fields :invitation-message ["Invited to job"])
        proposal (-> (common-fields :proposal-message ["Sent a job proposal"])
                      (add-to-details ,,, (format-proposal-amount job-story)))
        arbiter-feedback (map #(common-chat-fields current-user % :message ["Feedback for arbiter"])
                              (:job-story/arbiter-feedback job-story))
        employer-feedback (map #(common-chat-fields current-user % :message ["Feedback for employer"])
                              (:job-story/employer-feedback job-story))
        employer-feedback (map #(common-chat-fields current-user % :message ["Feedback for candidate"])
                              (:job-story/candidate-feedback job-story))
        direct-messages (map #(common-chat-fields current-user % identity ["Direct message"])
                              (:direct-messages job-story))]
    (->> [dispute-creation dispute-resolution invitation proposal arbiter-feedback employer-feedback
          direct-messages]
         (remove nil? ,,,)
         (flatten ,,,)
         (sort-by :timestamp)
         (reverse ,,,))))

(defn c-chat [job-story-id]
  (let [active-user (:user/id @(re/subscribe [:ethlance.ui.subscriptions/active-session]))
        ; jobStory_invoices {items {message invoice_datePaid}}
        message-fields [:message/id
                        :message/text
                        [:creator [:user/id :user/name :user/profile-image]]
                        :message/date-created]
        messages-query [:job-story {:job-story/id job-story-id}
                        [:job-story/proposal-rate
                         [:job [:job/token-type
                                :job/token-address
                                [:token-details [:token-detail/id
                                                 :token-detail/name
                                                 :token-detail/symbol]]]]
                         [:dispute-creation-message message-fields]
                         [:dispute-resolution-message message-fields]
                         [:proposal-message message-fields]
                         [:invitation-message message-fields]

                         [:job-story/arbiter-feedback [:message/id
                                                       [:message message-fields]]]
                         [:job-story/employer-feedback [:message/id
                                                       [:message message-fields]]]
                         [:job-story/candidate-feedback [:message/id
                                                        [:message message-fields]]]
                         [:direct-messages (into message-fields [:message/creator :direct-message/recipient])]]]

        messages-result @(re/subscribe [::gql/query {:queries [messages-query]}])
        chat-messages (extract-chat-messages (:job-story messages-result) active-user)]
    [c-chat-log chat-messages]))

(defn c-feedback-panel [feedback-recipients]
  (let [job-story-id (re/subscribe [:page.job-contract/job-story-id])
        feedback-text (re/subscribe [:page.job-contract/feedback-text])
        feedback-rating (re/subscribe [:page.job-contract/feedback-rating])
        feedback-recipient (re/subscribe [:page.job-contract/feedback-recipient])
        feedback-done? (= 0 (count (keys feedback-recipients)))]
    (if feedback-done?
      [:div.feedback-input-container {:style {:opacity "50%"}}
       [:div {:style {:height "10em" :display "flex" :align-items "center" :justify-content "center"}}
        "No more feedback to leave. You have already left feedback for all participants"]]

      [:div.feedback-input-container
       [:span.selection-label "Feedback for:"]
       (into [c-radio-select
              {:selection @feedback-recipient
               :on-selection #(re/dispatch [:page.job-contract/set-feedback-recipient %])}]
             (map (fn [[user-type address]]
                    [address [c-radio-secondary-element (clojure.string/capitalize (name user-type))]])
                  feedback-recipients))
       [:div.rating-input
        [c-rating {:rating @feedback-rating
                   :on-change #(re/dispatch [:page.job-contract/set-feedback-rating %])}]]
       [:div.label "Feedback:"]
       [c-textarea-input {:value @feedback-text
                          :placeholder "Feedback"
                          :on-change #(re/dispatch [:page.job-contract/set-feedback-text %])}]
       [:span.note "Note, by leaving feedback, you will end this contract, which means no more invoices can be sent."]
       [c-button {:color :primary
                  :on-click #(re/dispatch [:page.job-contract/send-feedback
                                           {:job-story/id job-story-id
                                            :text @feedback-text
                                            :rating @feedback-rating
                                            :to @feedback-recipient}])}
        [c-button-label "Send Feedback"]]])))

(defn c-direct-message [recipients]
  (let [text (re/subscribe [:page.job-contract/message-text])
        recipient (re/subscribe [:page.job-contract/message-recipient])
        job-story-id (re/subscribe [:page.job-contract/job-story-id])]
    [:div.message-input-container
     [:span.selection-label "Recipient:"]
     (into [c-radio-select
            {:selection @recipient
             :on-selection #(re/dispatch [:page.job-contract/set-message-recipient %])}]
           (map (fn [[user-type address]]
                  [address [c-radio-secondary-element (clojure.string/capitalize (name user-type))]])
                recipients))

     [:div.label "Message"]
     [c-textarea-input {:placeholder ""
                        :value @text
                        :on-change #(re/dispatch [:page.job-contract/set-message-text %])}]
     [c-button {:color :primary
                :on-click #(re/dispatch [:page.job-contract/send-message {:text @text
                                                                          :to @recipient
                                                                          :job-story/id @job-story-id}])}
      [c-button-label "Send Message"]]]))

(defn c-employer-options [{job-story-status :job-story/status
                           employer :employer
                           arbiter :arbiter
                           candidate :candidate
                           current-user-role :current-user-role
                           :as message-params}]
  (let [active-user (:user/id @(re/subscribe [:ethlance.ui.subscriptions/active-session]))
        *active-page-params (re/subscribe [::router.subs/active-page-params])
        job-story-id (-> @*active-page-params :job-story-id parse-int)]
    [c-tabular-layout
     {:key "employer-tabular-layout"
      :default-tab 1}

     {:label "Send Message"}
     [c-direct-message (select-keys message-params [:candidate :arbiter])]

     {:label "Leave Feedback"}
     [c-feedback-panel (select-keys message-params [:candidate :arbiter])]]))

(defn c-candidate-options [message-params]
  [c-tabular-layout
   {:key "employer-tabular-layout"
    :default-tab 0}

   {:label "Send Message"}
   [c-direct-message (select-keys message-params [:employer :arbiter])]

   {:label "Raise Dispute"}
   [:div.dispute-input-container
    [:div.label "Dispute"]
    [c-textarea-input {:placeholder ""}]
    [c-button {:color :primary} [c-button-label "Raise Dispute"]]]

   {:label "Leave Feedback"}
   [c-feedback-panel (select-keys message-params [:employer :arbiter])]])

(defn c-arbiter-options [message-params]
  [c-tabular-layout
   {:key "employer-tabular-layout"
    :default-tab 0}

   {:label "Send Message"}
   [c-direct-message (select-keys message-params [:candidate :employer])]

   {:label "Resolve Dispute"
    :active? true} ;; TODO: conditionally show
   [:div.dispute-input-container
    [:div.label "Dispute"]
    [c-textarea-input {:placeholder ""}]
    [c-button {:color :primary} [c-button-label "Resolve Dispute"]]]

   {:label "Leave Feedback"}
   [c-feedback-panel (select-keys message-params [:candidate :employer])]])

(defn c-guest-options [])

(defmethod page :route.job/contract []
  (let [*active-page-params (re/subscribe [::router.subs/active-page-params])]
    (fn []
      (let [job-story-id (-> @*active-page-params :job-story-id parse-int)
            active-user (:user/id @(re/subscribe [:ethlance.ui.subscriptions/active-session]))
            job-story-query [:job-story {:job-story/id job-story-id}
                             [:job/id
                              :job-story/id
                              :job-story/status
                              [:candidate [:user/id
                                           [:user [:user/name]]]]
                              [:job [:job/title
                                     :job/token-type
                                     :job/token-amount
                                     :job/token-address
                                     :job/token-id
                                     [:job/employer
                                      [:user/id
                                       [:user [:user/name]]]]
                                     [:job/arbiter
                                      [:user/id
                                       [:user [:user/name]]]]
                                     [:token-details [:token-detail/symbol :token-detail/name]]]]]]
            result @(re/subscribe [::gql/query {:queries [job-story-query]}])
            job-story (:job-story result)

            candidate-id (get-in job-story [:candidate :user/id])
            employer-id (get-in job-story [:job :job/employer :user/id])
            arbiter-id (get-in job-story [:job :job/arbiter :user/id])

            involved-users {:candidate candidate-id
                            :employer employer-id
                            :arbiter arbiter-id}
            current-user-role (reduce (fn [acc [user-type address]]
                                        (if (and (nil? acc) (ilike= address active-user)) user-type acc))
                                      nil ; initial value
                                      involved-users)
            message-params (-> involved-users
                               (assoc ,,, :job-story/status (:job-story/status job-story))
                               (assoc ,,, :current-user-role current-user-role))

            profile {:title (get-in job-story [:job :job/title])
                     :status (get-in job-story [:job-story/status])
                     :funds {:amount (get-in job-story [:job :job/token-amount])
                             :name (-> job-story :job :token-details :token-detail/name)
                             :symbol (-> job-story :job :token-details :token-detail/symbol)}
                     :employer {:name (get-in job-story [:job :job/employer :user :user/name])
                                :address employer-id}
                     :candidate {:name (get-in job-story [:candidate :user :user/name])
                                 :address candidate-id}
                     :arbiter {:name (get-in job-story [:job :job/arbiter :user :user/name])
                               :address arbiter-id}}]
        [c-main-layout {:container-opts {:class :job-contract-main-container}}
         [:div.header-container
          [c-header-profile profile]
          [c-chat job-story-id]]

         [:div.options-container
          (case current-user-role
            :employer [c-employer-options message-params]
            :candidate [c-candidate-options message-params]
            :arbiter [c-arbiter-options message-params]
            [c-guest-options])]]))))
