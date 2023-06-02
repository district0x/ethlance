(ns ethlance.ui.page.job-contract
  (:require [district.parsers :refer [parse-int]]
            [district.ui.component.page :refer [page]]
            [district.ui.router.subs :as router.subs]
            [ethlance.ui.util.tokens :as tokens]
            [district.format :as format]
            [ethlance.shared.utils :refer [ilike=]]
            [district.ui.graphql.subs :as gql]
            [ethlance.ui.component.button :refer [c-button c-button-label]]
            [ethlance.ui.component.chat :refer [c-chat-log]]
            [ethlance.ui.component.main-layout :refer [c-main-layout]]
            [ethlance.ui.component.radio-select :refer [c-radio-secondary-element c-radio-select]]
            [ethlance.ui.component.rating :refer [c-rating]]
            [ethlance.ui.component.tabular-layout :refer [c-tabular-layout]]
            [ethlance.ui.component.textarea-input :refer [c-textarea-input]]
            [ethlance.ui.component.text-input :refer [c-text-input]]
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

(defn c-information [text]
  [:div.feedback-input-container {:style {:opacity "50%"}}
   [:div {:style {:height "10em" :display "flex" :align-items "center" :justify-content "center"}}
    text]])

(defn common-chat-fields [current-user job-story field-fn details]
  (let [direction (fn [viewer creator]
                    (if (ilike= viewer creator)
                      :sent :received))
        message (field-fn job-story)]
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
                                 (let [amount (tokens/human-amount
                                                (-> job-story :job-story/proposal-rate)
                                                (-> job-story :job :job/token-type))
                                       token-name (-> job-story :job :token-details :token-detail/name)
                                       token-symbol (-> job-story :job :token-details :token-detail/symbol)]
                                   (str amount " " token-symbol " (" token-name ")")))
        common-fields (partial common-chat-fields current-user job-story)

        invitation (common-fields :invitation-message ["Invited to a job"])
        invitation-accepted (common-fields :invitation-accepted-message ["Accepted invitation to a job"])
        proposal (-> (common-fields :proposal-message ["Sent a job proposal"])
                      (add-to-details ,,, (format-proposal-amount job-story)))
        proposal-accepted (common-fields :proposal-accepted-message ["Accepted proposal for a job"])
        arbiter-feedback (map #(common-chat-fields current-user % :message ["Feedback for arbiter"])
                              (:job-story/arbiter-feedback job-story))
        employer-feedback (map #(common-chat-fields current-user % :message ["Feedback for employer"])
                              (:job-story/employer-feedback job-story))
        employer-feedback (map #(common-chat-fields current-user % :message ["Feedback for candidate"])
                              (:job-story/candidate-feedback job-story))
        direct-messages (map #(common-chat-fields current-user % identity ["Direct message"])
                              (:direct-messages job-story))
        invoice-messages (map #(common-chat-fields current-user % identity ["Invoice created"])
                              (map :creation-message (get-in job-story [:job-story/invoices :items])))
        dispute-creation (map #(common-chat-fields current-user % identity ["Dispute was created"])
                              (map :dispute-raised-message (get-in job-story [:job-story/invoices :items])))
        dispute-resolution (map #(common-chat-fields current-user % identity ["Dispute was resolved"])
                                (map :dispute-resolved-message (get-in job-story [:job-story/invoices :items])))]
    (->> [dispute-creation dispute-resolution
          invitation invitation-accepted
          proposal proposal-accepted
          arbiter-feedback employer-feedback
          direct-messages invoice-messages]
         (remove nil? ,,,)
         (flatten ,,,)
         (remove nil?)
         (sort-by :timestamp)
         ; (reverse ,,,) ; Uncomment to see more recent messages at the top
         )))

(defn c-chat [job-story-id]
  (let [active-user (:user/id @(re/subscribe [:ethlance.ui.subscriptions/active-session]))
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
                         [:proposal-message message-fields]
                         [:proposal-accepted-message message-fields]
                         [:invitation-message message-fields]
                         [:invitation-accepted-message message-fields]

                         [:job-story/arbiter-feedback [:message/id
                                                       [:message message-fields]]]
                         [:job-story/employer-feedback [:message/id
                                                       [:message message-fields]]]
                         [:job-story/candidate-feedback [:message/id
                                                        [:message message-fields]]]
                         [:direct-messages (into message-fields [:message/creator :direct-message/recipient])]
                         [:job-story/invoices [[:items [:id
                                                       [:creation-message message-fields]
                                                       [:dispute-raised-message message-fields]
                                                       [:dispute-resolved-message message-fields]]]] ]]]

        messages-result (re/subscribe [::gql/query {:queries [messages-query]}
                                       {:refetch-on #{:page.job-contract/refetch-messages}}])]
    (fn [job-story-id]
      (let [chat-messages (extract-chat-messages (:job-story @messages-result) active-user)]
        [c-chat-log chat-messages]))))

(defn c-feedback-panel [feedback-recipients]
  (let [job-story-id (re/subscribe [:page.job-contract/job-story-id])
        feedback-text (re/subscribe [:page.job-contract/feedback-text])
        feedback-rating (re/subscribe [:page.job-contract/feedback-rating])
        feedback-recipient (re/subscribe [:page.job-contract/feedback-recipient])
        feedback-done? (= 0 (count (keys feedback-recipients)))]
    (if feedback-done?
      [c-information "No more feedback to leave. You have already left feedback for all participants"]

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
                                           {:job-story/id @job-story-id
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

(defn c-accept-proposal-message [message-params]
  (let [text (re/subscribe [:page.job-contract/accept-proposal-message-text])
        proposal-data (assoc (select-keys message-params [:job/id :job-story/id :candidate :employer])
                             :text @text)
        query [:job-story {:job-story/id (:job-story/id message-params)}
               [:job-story/id
                [:proposal-message [:message/id]]
                :job-story/status]]
        result (re/subscribe [::gql/query {:queries [query]}
                              {:refetch-on #{:page.job-contract/refetch-messages}}])

        can-accept? (= :proposal (:job-story/status message-params))]
    (if can-accept?
      [:div.message-input-container
       [:div.label "Message"]
       [c-textarea-input {:placeholder ""
                          :value @text
                          :on-change #(re/dispatch [:page.job-contract/set-accept-proposal-message-text %])}]
       [c-button {:color :primary
                  :on-click #(re/dispatch [:page.job-contract/accept-proposal proposal-data])}
        [c-button-label "Accept Proposal"]]]

      [:div.message-input-container
        [c-information "No proposals to accept"]])))

(defn c-employer-options [message-params]
  [c-tabular-layout
   {:key "employer-tabular-layout"
    :default-tab 1}

   {:label "Send Message"}
   [c-direct-message (select-keys message-params [:candidate :arbiter])]

   {:label "Accept Proposal"}
   [c-accept-proposal-message message-params]

   {:label "Leave Feedback"}
   [c-feedback-panel (select-keys message-params [:candidate :arbiter])]])

(defn c-candidate-options [{job-story-status :job-story/status ; TODO: take into account for limiting actions (feedback, disputes)
                           job-id :job/id
                           employer :employer
                           arbiter :arbiter
                           candidate :candidate
                           current-user-role :current-user-role
                           :as message-params}]
  (let [active-user (:user/id @(re/subscribe [:ethlance.ui.subscriptions/active-session]))
        *active-page-params (re/subscribe [::router.subs/active-page-params])
        job-story-id (-> @*active-page-params :job-story-id parse-int)

        invoice-query [:job-story {:job-story/id job-story-id}
                       [:job/id
                        :job-story/id
                        [:job
                         [:job/token-type
                          :job/token-address
                          :job/token-id
                          [:token-details [:token-detail/name :token-detail/symbol]]]]
                        [:job-story/invoices [:total-count
                                              [:items [:id
                                                       :job/id
                                                       :job-story/id
                                                       :invoice/status
                                                       :invoice/id
                                                       :invoice/date-paid
                                                       :invoice/amount-requested
                                                       :invoice/amount-paid
                                                       [:creation-message [:message/date-created]]
                                                       [:dispute-raised-message [:message/id :message/text]]
                                                       [:dispute-resolved-message [:message/id :message/text]]]]]]]]
        invoice-result (re/subscribe [::gql/query {:queries [invoice-query]}])
        invoices (get-in @invoice-result [:job-story :job-story/invoices :items])
        latest-unpaid-invoice (->> invoices
                                   (filter #(= "created" (:invoice/status %)) ,,,)
                                   (sort-by #(get-in % [:creation-message :message/date-created]) > ,,,)
                                   first)
        token-symbol (get-in @invoice-result [:job-story :job :token-details :token-detail/symbol])
        token-type (keyword (get-in @invoice-result [:job-story :job :job/token-type]))
        candidate-invoiced-amount (get-in latest-unpaid-invoice [:invoice/amount-requested])
        human-amount (tokens/human-amount candidate-invoiced-amount token-type)

        has-invoice? (not (nil? latest-unpaid-invoice))
        can-dispute? (and has-invoice?
                          (nil? (get-in latest-unpaid-invoice [:dispute-raised-message])))
        dispute-available? (and has-invoice? can-dispute?)
        dispute-unavailable-message (if has-invoice?
                                      "You have already raised a dispute on your latest invoice. One invoice can only be disputed once"
                                      "Raising dispute becomes available after creating an invoice")
        dispute-text (re/subscribe [:page.job-contract/dispute-text])]
    [c-tabular-layout
     {:key "candidate-tabular-layout"
      :default-tab 0}

     {:label "Send Message"}
     [c-direct-message (select-keys message-params [:employer :arbiter])]

     {:label "Raise Dispute"}
     (if can-dispute?
       [:div.dispute-input-container
        [:div.label "Dispute"]
        [:p "This is about the latest created, unpaid, non-disputed invoice"
         " (ref.id " (:invoice/id latest-unpaid-invoice) ")"
         " for " human-amount " (" token-symbol ")"
         " created " (format/time-ago (new js/Date (get-in latest-unpaid-invoice [:creation-message :message/date-created])))
         " (" (.toString (new js/Date (get-in latest-unpaid-invoice [:creation-message :message/date-created] 0))) ")"]
        [c-textarea-input {:placeholder "Please explain the reason of the dispute"
                           :value @dispute-text
                           :on-change #(re/dispatch [:page.job-contract/set-dispute-text %])}]
        [c-button {:color :primary
                   :on-click #(re/dispatch [:page.job-contract/raise-dispute
                                            {:job/id job-id
                                             :job-story/id job-story-id
                                             :invoice/id (:invoice/id latest-unpaid-invoice)}])}
         [c-button-label "Raise Dispute"]]]

       ; else: can't dispute
       [c-information dispute-unavailable-message])

     {:label "Leave Feedback"}
     [c-feedback-panel (select-keys message-params [:employer :arbiter])]]))

(defn c-arbiter-options [message-params]
  (let [*active-page-params (re/subscribe [::router.subs/active-page-params])
        job-story-id (-> @*active-page-params :job-story-id parse-int)

        invoice-query [:job-story {:job-story/id job-story-id}
                       [:job/id
                        :job-story/id
                        :job-story/status
                        [:job
                         [:job/token-type
                          :job/token-address
                          :job/token-id
                          [:token-details [:token-detail/name :token-detail/symbol]]]]
                        [:job-story/employer-feedback [:message/id]]
                        [:job-story/candidate-feedback [:message/id]]
                        [:job-story/invoices [:total-count
                                              [:items [:id
                                                       :job/id
                                                       :job-story/id
                                                       :invoice/status
                                                       :invoice/id
                                                       :invoice/date-paid
                                                       :invoice/amount-requested
                                                       :invoice/amount-paid
                                                       [:creation-message [:message/date-created]]
                                                       [:dispute-raised-message [:message/id]]
                                                       [:dispute-resolved-message [:message/id]]]]]]]]
        invoice-result (re/subscribe [::gql/query {:queries [invoice-query]}])
        job-id (get-in @invoice-result [:job-story :job/id])
        job-story-id (get-in @invoice-result [:job-story :job-story/id])
        token-symbol (get-in @invoice-result [:job-story :job :token-details :token-detail/symbol])
        token-type (keyword (get-in @invoice-result [:job-story :job :job/token-type]))
        token-address (get-in @invoice-result [:job-story :job :job/token-address])
        token-id (get-in @invoice-result [:job-story :job :job/token-id])
        invoices (get-in @invoice-result [:job-story :job-story/invoices :items])
        dispute-open? (fn [invoice] (not (nil? (:dispute-raised-message invoice))))
        latest-disputed-invoice (->> invoices
                                   ; (filter #(= "dispute-raised" (:invoice/status %)) ,,,)
                                   (filter dispute-open? ,,,)
                                   (sort-by #(get-in % [:creation-message :message/date-created]) > ,,,)
                                   first)
        dispute-to-resolve? (nil? (:dispute-resolved-message latest-disputed-invoice))
        invoice-id (:invoice/id latest-disputed-invoice)
        dispute-candidate-percentage (re/subscribe [:page.job-contract/dispute-candidate-percentage])
        dispute-text (re/subscribe [:page.job-contract/dispute-text])
        candidate-invoiced-amount (get-in latest-disputed-invoice [:invoice/amount-requested])
        human-amount (tokens/human-amount candidate-invoiced-amount token-type)
        resolution-percentage (/ @dispute-candidate-percentage 100)
        resolved-amount (.toFixed (* human-amount resolution-percentage) 3)
        resolution-contract-amount (* candidate-invoiced-amount resolution-percentage)
        feedback-available-for-arbiter? (or
                                          (not-empty (get-in @invoice-result [:job-story :job-story/employer-feedback]))
                                          (not-empty (get-in @invoice-result [:job-story :job-story/candidate-feedback])))]
    [c-tabular-layout
     {:key "arbiter-tabular-layout"
      :default-tab 0}

     {:label "Send Message"}
     [c-direct-message (select-keys message-params [:candidate :employer])]

     {:label "Resolve Dispute" :active? true} ;; TODO: conditionally show
     (if dispute-to-resolve?
       [:div.dispute-input-container
        [:div {:style {:gap "2em" :display "flex"}}
         [:label "Amount (%) of the invoiced amount for the candidate. Invoice ref.id: " invoice-id]
         [:div
          [c-text-input
           {:value @dispute-candidate-percentage
            :on-change #(re/dispatch [:page.job-contract/set-dispute-candidate-percentage %])
            :placeholder 100
            :min 0
            :max 100
            :list "percentage-markers"
            :type :range
            :style {:width "200px"}}]
          [:datalist {:id "percentage-markers"
                      :style {:display "flex"
                              :flex-direction "column"
                              :justify-content "space-between"
                              :writing-mode "vertical-lr"
                              :width "200px"}}
           (into [:<>]
                 (map #(vector :option {:value % :label (str %) :style {:padding "0"}})
                      (range 0 101 25)))]]
         [:label
          "Candidate gets: " @dispute-candidate-percentage "%"
          " or " resolved-amount " " token-symbol " of the requested " human-amount " " token-symbol]]
        [:div.label "Explanation:"]
        [c-textarea-input {:placeholder "Please explain the reasoning behind the resolution"
                           :value @dispute-text
                           :on-change #(re/dispatch [:page.job-contract/set-dispute-text %])}]
        [c-button {:color :primary :on-click #(re/dispatch [:page.job-contract/resolve-dispute
                                                            {:job/id job-id
                                                             :job-story/id job-story-id
                                                             :invoice/id invoice-id
                                                             :token-type token-type
                                                             :token-amount resolution-contract-amount
                                                             :token-address token-address
                                                             :token-id token-id}])}
         [c-button-label "Resolve Dispute"]]]

       ; Else
       [c-information (str "The latest invoice ref.id " invoice-id " doesn't have open dispute to resolve")])

     {:label "Leave Feedback"}
     (if feedback-available-for-arbiter?
       [c-feedback-panel (select-keys message-params [:candidate :employer])]
       [c-information "Leaving feedback becomes available after employer or candidate have given their feedback (and thus terminated the job contract)"])]))

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

                              [:proposal-message [:message/id :message/text]]

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
            result @(re/subscribe [::gql/query {:queries [job-story-query]} {:refetch-on #{:job-contract-refresh}}])
            _ (cljs.pprint/pprint result)
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
                               (assoc ,,, :job/id (:job/id job-story))
                               (assoc ,,, :job-story/status (:job-story/status job-story))
                               (assoc ,,, :job-story/id job-story-id)
                               (assoc ,,, :current-user-role current-user-role))

            token-type (keyword (get-in job-story [:job :job/token-type]))
            raw-amount (get-in job-story [:job :job/token-amount])
            human-amount (tokens/human-amount raw-amount token-type)
            profile {:title (get-in job-story [:job :job/title])
                     :status (get-in job-story [:job-story/status])
                     :funds {:amount human-amount
                             :name (-> job-story :job :token-details :token-detail/name)
                             :symbol (-> job-story :job :token-details :token-detail/symbol)}
                     :employer {:name (get-in job-story [:job :job/employer :user :user/name])
                                :address employer-id}
                     :candidate {:name (get-in job-story [:candidate :user :user/name])
                                 :address candidate-id}
                     :arbiter {:name (get-in job-story [:job :job/arbiter :user :user/name])
                               :address arbiter-id}}]
        [c-main-layout {:container-opts {:class :job-contract-main-container :random (rand)}}
         [:div.header-container
          [c-header-profile profile]
          [c-chat job-story-id]]

         [:div.options-container
          (case current-user-role
            :employer [c-employer-options message-params]
            :candidate [c-candidate-options message-params]
            :arbiter [c-arbiter-options message-params]
            [c-guest-options])]]))))
