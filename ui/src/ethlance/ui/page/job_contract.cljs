(ns ethlance.ui.page.job-contract
  (:require
    [clojure.string]
    [district.format :as format]
    [district.parsers :refer [parse-int]]
    [district.ui.component.page :refer [page]]
    [district.ui.graphql.subs :as gql]
    [district.ui.router.subs :as router.subs]
    [ethlance.shared.utils :refer [ilike=]]
    [ethlance.ui.component.button :refer [c-button c-button-label]]
    [ethlance.ui.component.chat :refer [c-chat-log]]
    [ethlance.ui.component.main-layout :refer [c-main-layout]]
    [ethlance.ui.component.radio-select :refer [c-radio-secondary-element c-radio-select]]
    [ethlance.ui.component.rating :refer [c-rating]]
    [ethlance.ui.component.tabular-layout :refer [c-tabular-layout]]
    [ethlance.ui.component.text-input :refer [c-text-input]]
    [ethlance.ui.component.textarea-input :refer [c-textarea-input]]
    [ethlance.ui.component.token-info :refer [c-token-info] :as token-info]
    [ethlance.ui.util.navigation :as util.navigation]
    [ethlance.ui.util.tokens :as tokens]
    [re-frame.core :as re]))


(defn profile-link-handlers
  [user-type address]
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
   [:div.value [c-token-info (:amount funds) (:token-details funds)]]

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
   [:a.job-name
    (util.navigation/link-params {:route :route.job/detail :params {:id (:job/id details)}})
    title]
   [:div.job-details
    [c-job-detail-table details]]])


(defn c-information
  [text]
  [:div.feedback-input-container {:style {:opacity "50%"}}
   [:div {:style {:height "10em" :display "flex" :align-items "center" :justify-content "center"}}
    text]])


(defn common-chat-fields
  [involved-users current-user entity field-fn details]
  (let [direction (fn [viewer creator]
                    (if (ilike= viewer creator)
                      :sent :received))
        message (field-fn entity)
        sender (-> message :creator :user/id)
        involved-reversed (reduce-kv #(assoc %1 %3 %2) {} involved-users)]
    (when message
      {:id (str "dispute-creation-msg-" (-> message :message/id))
       :direction (direction current-user (-> message :creator :user/id))
       :user-type (get involved-reversed sender)
       :text (-> message :message/text)
       :full-name (-> message :creator :user/name)
       :sender-user-id (-> message :creator :user/id)
       :timestamp (-> message :message/date-created)
       :image-url (-> message :creator :user/profile-image)
       :details (map (fn [detail-or-fn]
                       (if (fn? detail-or-fn)
                         (detail-or-fn entity)
                         detail-or-fn))
                     details)})))


(defn invoice-detail
  [job-story amount-field invoice]
  [c-token-info (amount-field invoice) (get-in job-story [:job :token-details])])


(defn extract-chat-messages
  [job-story current-user involved-users]
  (let [add-to-details (fn [message additional-detail]
                         (when message
                           (assoc message :details (conj (:details message) additional-detail))))
        format-proposal-amount (fn [job-story]
                                 (token-info/token-info-str (-> job-story :job-story/proposal-rate) (get-in job-story [:job :token-details])))
        common-fields (partial common-chat-fields involved-users current-user job-story)
        common-chat-fields (partial common-chat-fields involved-users)

        invitation (common-fields :invitation-message ["Sent job invitation"])
        invitation-accepted (common-fields :invitation-accepted-message ["Accepted invitation"])
        proposal (-> (common-fields :proposal-message ["Sent job proposal"])
                     (add-to-details ,,, (format-proposal-amount job-story)))
        proposal-accepted (common-fields :proposal-accepted-message ["Accepted proposal"])
        feedback-stars (fn [fb] [c-rating {:color :white :size :small :rating (:feedback/rating fb)}])
        arbiter-feedback (map #(common-chat-fields current-user % :message ["Feedback for arbiter"
                                                                            feedback-stars])
                              (:job-story/arbiter-feedback job-story))
        employer-feedback (map #(common-chat-fields current-user % :message ["Feedback for employer"
                                                                             feedback-stars])
                               (:job-story/employer-feedback job-story))
        candidate-feedback (map #(common-chat-fields current-user % :message ["Feedback for candidate"
                                                                              feedback-stars])
                                (:job-story/candidate-feedback job-story))
        direct-messages (map #(common-chat-fields current-user % identity ["Direct message"])
                             (:direct-messages job-story))
        invoice-link (fn [invoice]
                       [:a (util.navigation/link-params
                             {:route :route.invoice/index
                              :params {:job-id (:job/id invoice) :invoice-id (:invoice/id invoice)}})
                        "View invoice details"])
        invoice-messages (map #(common-chat-fields current-user % :creation-message
                                                   [(fn [invoice] (str "Invoice #" (:invoice/id invoice) " created"))
                                                    (partial invoice-detail job-story :invoice/amount-requested)
                                                    invoice-link])
                              (get-in job-story [:job-story/invoices :items]))
        payment-messages (map #(common-chat-fields current-user % :payment-message
                                                   [(fn [invoice]
                                                      (str "Invoice #" (:invoice/id invoice) " paid"))
                                                    (partial invoice-detail job-story :invoice/amount-paid)
                                                    invoice-link])
                              (get-in job-story [:job-story/invoices :items]))
        dispute-creation (map #(common-chat-fields current-user % identity ["Dispute was created"])
                              (map :dispute-raised-message (get-in job-story [:job-story/invoices :items])))
        dispute-resolution (map #(common-chat-fields current-user % identity ["Dispute was resolved"])
                                (map :dispute-resolved-message (get-in job-story [:job-story/invoices :items])))]
    (->> [dispute-creation dispute-resolution
          invitation invitation-accepted
          proposal proposal-accepted
          arbiter-feedback employer-feedback candidate-feedback
          direct-messages
          invoice-messages payment-messages]
         (remove nil? ,,,)
         (flatten ,,,)
         (remove nil?)
         (sort-by :timestamp)
         ;; (reverse ,,,) ; Uncomment to see more recent messages at the top
         )))


(defn c-chat
  [job-story-id involved-users]
  (let [active-user (:user/id @(re/subscribe [:ethlance.ui.subscriptions/active-session]))
        message-fields [:message/id
                        :message/text
                        [:creator [:user/id :user/name :user/profile-image]]
                        :message/date-created]
        messages-query [:job-story {:job-story/id job-story-id}
                        [:job-story/proposal-rate
                         [:job
                          [:job/token-type
                           :job/token-address
                           [:token-details
                            [:token-detail/id
                             :token-detail/type
                             :token-detail/name
                             :token-detail/symbol
                             :token-detail/decimals]]]]
                         [:proposal-message message-fields]
                         [:proposal-accepted-message message-fields]
                         [:invitation-message message-fields]
                         [:invitation-accepted-message message-fields]

                         [:job-story/arbiter-feedback [:message/id
                                                       :feedback/rating
                                                       [:message message-fields]]]
                         [:job-story/employer-feedback [:message/id
                                                        :feedback/rating
                                                        [:message message-fields]]]
                         [:job-story/candidate-feedback [:message/id
                                                         :feedback/rating
                                                         [:message message-fields]]]
                         [:direct-messages (into message-fields [:message/creator :direct-message/recipient])]
                         [:job-story/invoices
                          [[:items
                            [:id
                             :invoice/id
                             :job-story/id
                             :job/id
                             :invoice/amount-requested
                             :invoice/amount-paid
                             :invoice/status
                             [:creation-message message-fields]
                             [:payment-message message-fields]
                             [:dispute-raised-message message-fields]
                             [:dispute-resolved-message message-fields]]]]]]]

        messages-result (re/subscribe [::gql/query {:queries [messages-query]}
                                       {:refetch-on #{:page.job-contract/refetch-messages}}])
        graphql-data-ready? (and (not (:graphql/loading? @messages-result)) (not (:graphql/processing? @messages-result)))]
    (when graphql-data-ready?
      [c-chat-log (extract-chat-messages (:job-story @messages-result) active-user involved-users)])))


(defn c-feedback-panel
  [feedbacker]
  (let [job-story-id (re/subscribe [:page.job-contract/job-story-id])
        feedback-text (re/subscribe [:page.job-contract/feedback-text])
        feedback-rating (re/subscribe [:page.job-contract/feedback-rating])
        user-fields [:user [:user/id :user/name]]
        query [:job-story {:job-story/id @job-story-id}
               [:job-story/id
                [:candidate
                 [user-fields]]
                [:job
                 [:job/id
                  [:job/employer
                   [user-fields]]
                  [:job/arbiter
                   [user-fields]]]]
                [:job-story/invoices {:statuses [:dispute-raised :created]}
                 [[:items
                   [:id]]]]
                [:feedbacks
                 [:feedback/from-user-type
                  :feedback/to-user-type
                  [:feedback/from-user [:user/id :user/name]]
                  [:feedback/to-user [:user/id :user/name]]]]]]
        results @(re/subscribe [::gql/query {:queries [query]} {:refetch-on #{:page.job-contract/refetch-messages}}])
        feedbacks (get-in results [:job-story :feedbacks])
        open-invoices (get-in results [:job-story :job-story/invoices :items])
        participants {:employer (get-in results [:job-story :job :job/employer :user])
                      :candidate (get-in results [:job-story :candidate :user])
                      :arbiter (get-in results [:job-story :job :job/arbiter :user])}

        normalized-feedback-users (map (fn [fb]
                                         [(:feedback/from-user-type fb)
                                          (:feedback/to-user-type fb)])
                                       feedbacks)
        feedback-between? (fn [feedbacks from to]
                            (some #(= % [from to]) feedbacks))
        given-feedback? (partial feedback-between? normalized-feedback-users)
        feedback-receiver-role (case feedbacker
                                 :employer
                                 (cond
                                   (not (given-feedback? :employer :candidate))
                                   :candidate

                                   (and
                                     (given-feedback? :employer :candidate)
                                     (not (given-feedback? :employer :arbiter)))
                                   :arbiter)

                                 :candidate
                                 (cond
                                   (not (given-feedback? :candidate :employer))
                                   :employer

                                   (and
                                     (given-feedback? :candidate :employer)
                                     (not (given-feedback? :candidate :arbiter)))
                                   :arbiter)

                                 :arbiter
                                 (when
                                   (or
                                     (given-feedback? :employer :candidate feedbacks)
                                     (given-feedback? :candidate :employer feedbacks))
                                   (cond
                                     (and (not (given-feedback? :arbiter :employer feedbacks))
                                          (not (given-feedback? :arbiter :candidate feedbacks)))
                                     :candidate
                                     (and (not (given-feedback? :arbiter :employer feedbacks))
                                          (given-feedback? :arbiter :candidate feedbacks))
                                     :employer
                                     (and (not (given-feedback? :arbiter :candidate feedbacks))
                                          (given-feedback? :arbiter :employer feedbacks))
                                     :candidate)))

        open-invoices? (not (empty? open-invoices))
        can-give-feedback? (and
                             (not (nil? feedback-receiver-role))
                             (empty? open-invoices))
        all-feedbacks-done? (and (empty? open-invoices)
                                 (nil? feedback-receiver-role))

        next-feedback-receiver (get participants feedback-receiver-role)]
    [:div.feedback-input-container
     (when open-invoices?
       [c-information "There are still unpaid invoices. Feedback can be given after they have been paid"])

     (when all-feedbacks-done?
       [c-information "No more feedback to leave. You have already left feedback for all participants"])

     (when can-give-feedback?
       [:<>
        [:span.selection-label "Feedback for:"]
        [:span.note (str (:user/name next-feedback-receiver) " (" (name feedback-receiver-role) ")")]
        (when (and
                (not (= :arbiter feedbacker))
                (#{:candidate :employer} feedback-receiver-role))
          [:span.note "Note, by leaving feedback, you will end this contract, which means no more invoices can be sent."])

        [:div.rating-input
         [c-rating {:rating @feedback-rating
                    :on-change #(re/dispatch [:page.job-contract/set-feedback-rating %])}]]
        [:div.label "Feedback:"]
        [c-textarea-input {:value @feedback-text
                           :placeholder "Feedback"
                           :on-change #(re/dispatch [:page.job-contract/set-feedback-text %])}]

        [c-button {:color :primary
                   :on-click #(re/dispatch [:page.job-contract/send-feedback
                                            {:job-story/id @job-story-id
                                             :text @feedback-text
                                             :rating @feedback-rating
                                             :to (:user/id next-feedback-receiver)}])}
         [c-button-label "Send Feedback"]]])]))


(defn c-direct-message
  []
  (let [text (re/subscribe [:page.job-contract/message-text])
        job-story-id (re/subscribe [:page.job-contract/job-story-id])]
    [:div.message-input-container
     [:div.label "Message"]
     [c-textarea-input {:placeholder ""
                        :value @text
                        :on-change #(re/dispatch [:page.job-contract/set-message-text %])}]
     [c-button {:color :primary
                :on-click #(re/dispatch [:page.job-contract/send-message {:text @text
                                                                          :job-story/id @job-story-id}])}
      [c-button-label "Send Message"]]]))


(defn c-accept-proposal-message
  [message-params]
  (let [text (re/subscribe [:page.job-contract/accept-proposal-message-text])
        proposal-data (assoc (select-keys message-params [:job/id :job-story/id :candidate :employer])
                             :text @text)
        can-accept? (= :proposal (:job-story/status message-params))
        button-disabled? (re/subscribe [:page.job-contract/buttons-disabled?])]
    (if can-accept?
      [:div.message-input-container
       [:div.label "Message"]
       [c-textarea-input {:placeholder ""
                          :value @text
                          :on-change #(re/dispatch [:page.job-contract/set-accept-proposal-message-text %])}]
       [c-button {:color :primary
                  :disabled? @button-disabled?
                  :on-click #(re/dispatch [:page.job-contract/accept-proposal proposal-data])}
        [c-button-label "Accept Proposal"]]]

      [:div.message-input-container
       [c-information "No proposals to accept"]])))


(defn c-accept-invitation
  [message-params]
  (let [text (re/subscribe [:page.job-contract/accept-invitation-message-text])
        job-story-id (re/subscribe [:page.job-contract/job-story-id])]
    [:div.message-input-container
     [:div.label "Message"]
     [c-textarea-input {:placeholder ""
                        :value @text
                        :on-change #(re/dispatch [:page.job-contract/set-accept-invitation-message-text %])}]
     [c-button {:color :primary
                :on-click #(re/dispatch [:page.job-contract/accept-invitation
                                         {:text @text
                                          :to (:employer message-params)
                                          :job-story/id @job-story-id}])}
      [c-button-label "Accept Invitation"]]]))


(defn c-employer-options
  [message-params]
  [c-tabular-layout
   {:key "employer-tabular-layout"
    :default-tab 1}

   {:label "Send Message"}
   [c-direct-message]

   {:label "Accept Proposal"}
   [c-accept-proposal-message message-params]

   {:label "Leave Feedback"}
   [c-feedback-panel :employer]])


(defn c-candidate-options
  [{job-story-status :job-story/status ; TODO: take into account for limiting actions (feedback, disputes)
    job-id :job/id
    :as message-params}]
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
                          [:token-details
                           [:token-detail/name
                            :token-detail/symbol
                            :token-detail/decimals]]
                          [:job/arbiter
                           [:user/id
                            [:user
                             [:user/name]]]]]]
                        [:invitation-message [:message/id]]
                        [:invitation-accepted-message [:message/id]]
                        [:job-story/invoices
                         [:total-count
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
        invoice-result (re/subscribe [::gql/query {:queries [invoice-query]} {:refetch-on #{:page.job-contract/refetch-messages}}])

        invitation-message (get-in @invoice-result [:job-story :invitation-message])
        invitation-accepted-message (get-in @invoice-result [:job-story :invitation-accepted-message])
        invitation-to-accept? (and
                                (not (nil? invitation-message))
                                (nil? invitation-accepted-message))
        invoices (get-in @invoice-result [:job-story :job-story/invoices :items])
        latest-unpaid-invoice (->> invoices
                                   (filter #(= "created" (:invoice/status %)) ,,,)
                                   (sort-by #(get-in % [:creation-message :message/date-created]) > ,,,)
                                   first)
        token-symbol (get-in @invoice-result [:job-story :job :token-details :token-detail/symbol])
        token-type (keyword (get-in @invoice-result [:job-story :job :job/token-type]))
        decimals (get-in @invoice-result [:job-story :job :token-details :token-detail/decimals])
        candidate-invoiced-amount (get-in latest-unpaid-invoice [:invoice/amount-requested])
        human-amount (tokens/human-amount candidate-invoiced-amount token-type decimals)

        has-invoice? (not (nil? latest-unpaid-invoice))
        has-arbiter? (not (nil? (get-in @invoice-result [:job-story :job :job/arbiter])))
        can-dispute? (and has-invoice?
                          has-arbiter?
                          (nil? (get latest-unpaid-invoice :dispute-raised-message)))
        job-active? (= :active (get-in @invoice-result [:job-story :job-story/status]))
        dispute-unavailable-message (cond
                                      (not has-arbiter?) "This job doesn't yet have an arbiter so disputes can't be created."
                                      has-invoice? "You have already raised a dispute on your latest invoice. One invoice can only be disputed once."
                                      :else "Raising dispute becomes available after creating an invoice.")
        dispute-text (re/subscribe [:page.job-contract/dispute-text])
        button-disabled? (re/subscribe [:page.job-contract/buttons-disabled?])]
    [c-tabular-layout
     {:key "candidate-tabular-layout"
      :default-tab 0}

     (when invitation-to-accept? {:label "Accept invitation"})
     (when invitation-to-accept? [c-accept-invitation message-params])

     (when job-active? {:label "Create Invoice"})
     (when job-active?
       [:div.message-input-container
        [:div.info-message "Click here to create new invoice for this job"]
        [c-button {:color :primary
                   :on-click (util.navigation/create-handler {:route :route.invoice/new})}
         [c-button-label "Go to create invoice"]]])


     {:label "Send Message"}
     [c-direct-message]

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
                   :disabled? @button-disabled?
                   :on-click #(re/dispatch [:page.job-contract/raise-dispute
                                            {:job/id job-id
                                             :job-story/id job-story-id
                                             :invoice/id (:invoice/id latest-unpaid-invoice)}])}
         [c-button-label "Raise Dispute"]]]

       ;; else: can't dispute
       [c-information dispute-unavailable-message])

     {:label "Leave Feedback"}
     [c-feedback-panel :candidate]]))


(defn c-arbiter-options
  [message-params]
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
                          [:token-details
                           [:token-detail/name
                            :token-detail/symbol
                            :token-detail/decimals]]]]
                        [:job-story/employer-feedback [:message/id]]
                        [:job-story/candidate-feedback [:message/id]]
                        [:job-story/invoices
                         [:total-count
                          [:items
                           [:id
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
        invoice-result (re/subscribe [::gql/query {:queries [invoice-query]} {:refetch-on #{:page.job-contract/refetch-messages}}])
        job-id (get-in @invoice-result [:job-story :job/id])
        job-story-id (get-in @invoice-result [:job-story :job-story/id])
        token-symbol (get-in @invoice-result [:job-story :job :token-details :token-detail/symbol])
        decimals (get-in @invoice-result [:job-story :job :token-details :token-detail/decimals])
        token-type (keyword (get-in @invoice-result [:job-story :job :job/token-type]))
        token-address (get-in @invoice-result [:job-story :job :job/token-address])
        token-id (get-in @invoice-result [:job-story :job :job/token-id])
        invoices (get-in @invoice-result [:job-story :job-story/invoices :items])
        dispute-open? (fn [invoice]
                        (and
                          (not (nil? (:dispute-raised-message invoice)))
                          (nil? (:dispute-resolved-message invoice))))
        latest-disputed-invoice (->> invoices
                                     (filter dispute-open? ,,,)
                                     (sort-by #(get-in % [:creation-message :message/date-created]) > ,,,)
                                     first)
        dispute-to-resolve? (not (nil? latest-disputed-invoice))

        invoice-id (:invoice/id latest-disputed-invoice)
        dispute-candidate-percentage (re/subscribe [:page.job-contract/dispute-candidate-percentage])
        dispute-text (re/subscribe [:page.job-contract/dispute-text])
        candidate-invoiced-amount (get-in latest-disputed-invoice [:invoice/amount-requested])
        human-amount (tokens/human-amount candidate-invoiced-amount token-type decimals)
        resolution-percentage (/ @dispute-candidate-percentage 100)
        resolved-amount (.toFixed (* human-amount resolution-percentage) 3)
        resolution-contract-amount (* candidate-invoiced-amount resolution-percentage)
        feedback-available-for-arbiter? (or
                                          (not-empty (get-in @invoice-result [:job-story :job-story/employer-feedback]))
                                          (not-empty (get-in @invoice-result [:job-story :job-story/candidate-feedback])))
        button-disabled? (re/subscribe [:page.job-contract/buttons-disabled?])]
    [c-tabular-layout
     {:key "arbiter-tabular-layout"
      :default-tab 0}

     {:label "Send Message"}
     [c-direct-message]

     {:label "Resolve Dispute" :active? true} ; TODO: conditionally show
     (if dispute-to-resolve?
       [:div.dispute-input-container
        [:div {:style {:gap "2em"}}
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
                              :flex-direction "row"
                              :justify-content "space-between"
                              :writing-mode "horizontal-tb"
                              :width "200px"}}
           (into [:<>]
                 (map #(vector :option {:value % :label (str %) :style {:padding "0"}})
                      (range 0 101 25)))]]
         [:div
          "Candidate gets: " @dispute-candidate-percentage "%"
          " or " resolved-amount " " token-symbol " of the requested " human-amount " " token-symbol]]
        [:div.label "Explanation:"]
        [c-textarea-input {:placeholder "Please explain the reasoning behind the resolution"
                           :value @dispute-text
                           :on-change #(re/dispatch [:page.job-contract/set-dispute-text %])}]
        [c-button {:color :primary
                   :disabled? @button-disabled?
                   :on-click #(re/dispatch [:page.job-contract/resolve-dispute
                                                            {:job/id job-id
                                                             :job-story/id job-story-id
                                                             :invoice/id invoice-id
                                                             :token-type token-type
                                                             :token-amount resolution-contract-amount
                                                             :token-address token-address
                                                             :token-id token-id}])}
         [c-button-label "Resolve Dispute"]]]

       ;; Else
       [c-information "There are no invoices with unresolved disputes for this job story"])

     {:label "Leave Feedback"}
     (if feedback-available-for-arbiter?
       [c-feedback-panel :arbiter]
       [c-information "Leaving feedback becomes available after employer or candidate have given their feedback (and thus terminated the job contract)"])]))


(defn c-guest-options
  [])


(defmethod page :route.job/contract []
  (let [*active-page-params (re/subscribe [::router.subs/active-page-params])]
    (fn []
      (let [job-story-id (-> @*active-page-params :job-story-id parse-int)
            active-user (:user/id @(re/subscribe [:ethlance.ui.subscriptions/active-session]))
            query [:job-story {:job-story/id job-story-id}
                   [:job/id
                    :job-story/id
                    :job-story/status
                    [:candidate
                     [:user/id
                      [:user [:user/name]]]]
                    [:proposal-message
                     [:message/id :message/text]]
                    [:job
                     [:job/title
                      :balance
                      [:job/employer
                       [:user/id
                        [:user [:user/name]]]]
                      [:job/arbiter
                       [:user/id
                        [:user [:user/name]]]]
                      [:token-details
                       [:token-detail/id
                        :token-detail/type
                        :token-detail/symbol
                        :token-detail/name
                        :token-detail/decimals]]]]]]
            result @(re/subscribe [::gql/query {:queries [query]} {:refetch-on #{:page.job-contract/refetch-messages}}])
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
            profile {:title (get-in job-story [:job :job/title])
                     :job/id (:job/id job-story)
                     :status (get job-story :job-story/status)
                     :funds {:amount (get-in job-story [:job :balance])
                             :token-details (get-in job-story [:job :token-details])}
                     :employer {:name (get-in job-story [:job :job/employer :user :user/name])
                                :address employer-id}
                     :candidate {:name (get-in job-story [:candidate :user :user/name])
                                 :address candidate-id}
                     :arbiter {:name (get-in job-story [:job :job/arbiter :user :user/name])
                               :address arbiter-id}}
            job-participant-viewing? (not (nil? current-user-role))
            graphql-data-ready? (and (not (:graphql/loading? result)) (not (:graphql/processing? result)))]
        [c-main-layout {:container-opts {:class :job-contract-main-container :random (rand)}}
         (if job-participant-viewing?
           [:<>
            [:div.header-container
             (when graphql-data-ready? [c-header-profile profile])
             [c-chat job-story-id involved-users]]

            [:div.options-container
             (case current-user-role
               :employer [c-employer-options message-params]
               :candidate [c-candidate-options message-params]
               :arbiter [c-arbiter-options message-params]
               [c-guest-options])]]
           [:div.header-container
            [c-information "Only the users involved in the job can see the contract details"]])]))))
