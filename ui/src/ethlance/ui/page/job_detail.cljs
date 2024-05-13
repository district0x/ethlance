(ns ethlance.ui.page.job-detail
  (:require
    [clojure.set]
    [clojure.string]
    [district.format :as format]
    [district.ui.component.page :refer [page]]
    [district.ui.graphql.subs :as gql]
    [ethlance.shared.utils :refer [ilike!= ilike=]]
    [ethlance.ui.component.button :refer [c-button c-button-label]]
    [ethlance.ui.component.carousel :refer [c-carousel-old c-feedback-slide]]
    [ethlance.ui.component.info-message :refer [c-info-message]]
    [ethlance.ui.component.loading-spinner :refer [c-loading-spinner]]
    [ethlance.ui.component.main-layout :refer [c-main-layout]]
    [ethlance.ui.component.pagination :as pagination]
    [ethlance.ui.component.profile-image :refer [c-profile-image]]
    [ethlance.ui.component.rating :refer [c-rating]]
    [ethlance.ui.component.scrollable :refer [c-scrollable]]
    [ethlance.ui.component.search-input :refer [c-chip-search-input]]
    [ethlance.ui.component.table :refer [c-table]]
    [ethlance.ui.component.tag :refer [c-tag c-tag-label]]
    [ethlance.ui.component.text-input :refer [c-text-input]]
    [ethlance.ui.component.textarea-input :refer [c-textarea-input]]
    [ethlance.ui.component.token-amount-input :refer [c-token-amount-input]]
    [ethlance.ui.component.token-info :as token-info :refer [c-token-info]]
    [ethlance.ui.util.component :refer [>evt]]
    [ethlance.ui.util.job :as util.job]
    [ethlance.ui.util.navigation :refer [link-params] :as util.navigation]
    [ethlance.ui.util.tokens :as token-utils]
    [re-frame.core :as re]))


(defn spinner-until-data-ready
  [loading-states component-when-loading-finished]
  (if (not-every? false? loading-states)
    [c-loading-spinner]
    component-when-loading-finished))

(defn hidden-until-data-ready
  [loading-states component-when-loading-finished]
  (when (every? false? loading-states)
    component-when-loading-finished))

(defn c-invoice-listing
  [contract-address]
  (let [invoices-query [:job {:job/id contract-address}
                        [[:token-details
                          [:token-detail/id
                           :token-detail/type
                           :token-detail/name
                           :token-detail/decimals
                           :token-detail/symbol]]
                         [:invoices
                          [:total-count
                           [:items
                            [:id
                             :job/id
                             :job-story/id
                             :invoice/status
                             :invoice/amount-requested
                             :invoice/amount-paid
                             [:creation-message
                              [:message/id
                               :message/date-created
                               [:creator
                                [:user/id
                                 :user/name
                                 :user/profile-image]]]]]]]]]]
        result @(re/subscribe [::gql/query {:queries [invoices-query]}])
        token-details (get-in result [:job :token-details])
        invoices (map (fn [invoice]
                        {:name (get-in invoice [:creation-message :creator :user/name])
                         :amount (token-info/token-info-str (get invoice :invoice/amount-requested) token-details)
                         :timestamp (format/time-ago (new js/Date (get-in invoice [:creation-message :message/date-created])))
                         :status (get invoice :invoice/status)})
                      (-> result :job :invoices :items))]
    [:div.invoice-listing
     [:div.label "Invoices"]
     (into [c-table {:headers ["Candidate" "Amount" "Created" "Status"]}]
           (map (fn [invoice]
                  [[:span (:name invoice)]
                   [:span (:amount invoice)]
                   [:span (:timestamp invoice)]
                   [:span (:status invoice)]])
                invoices))]))


(defn c-employer-feedback
  [contract-address]
  (let [feedback-query [:job {:job/id contract-address}
                        [[:job/employer
                          [[:employer/feedback
                            [:total-count
                             [:items [:feedback/rating
                                      :feedback/text
                                      :feedback/date-created
                                      [:feedback/from-user [:user/name
                                                            :user/profile-image]]]]]]]]]]
        result @(re/subscribe [::gql/query {:queries [feedback-query]}])
        feedback-raw (get-in result [:job :job/employer :employer/feedback :items])

        feedback (map (fn [feedback]
                        {:id (:message/id feedback)
                         :rating (:feedback/rating feedback)
                         :text (:feedback/text feedback)
                         :author (-> feedback :feedback/from-user :user/name)
                         :image-url (-> feedback :feedback/from-user :user/profile-image)})
                      feedback-raw)]
    [:div.feedback-listing
     [:div.label "Feedback for employer"]
     (if (empty? feedback)
       [:label "No feedback yet for this employer"]
       (into [c-carousel-old {}] (map #(c-feedback-slide %) feedback)))]))


(defn c-proposals-section
  [job]
  (let [contract-address (:id @(re/subscribe [:district.ui.router.subs/active-page-params]))
        active-user (:user/id @(re/subscribe [:ethlance.ui.subscriptions/active-session]))

        *bid-option (:job/bid-option job)
        *job-token-type (get job :job/token-type "")
        token-detail-symbol (get-in job [:token-details :token-detail/symbol])
        token-display-name (name (or token-detail-symbol *job-token-type ""))
        token-decimals (get-in job [:token-details :token-detail/decimals])
        *proposal-token-amount (re/subscribe [:page.job-detail/proposal-token-amount])
        *proposal-text (re/subscribe [:page.job-detail/proposal-text])

        proposals (re/subscribe [:page.job-detail/active-proposals])
        my-proposal (re/subscribe [:page.job-detail/my-proposal])

        proposal-limit @(re/subscribe [:page.job-detail/proposal-limit])
        proposal-total-count @(re/subscribe [:page.job-detail/proposal-total-count])
        proposal-offset @(re/subscribe [:page.job-detail/proposal-offset])

        my-job-story-id (:job-story/id @my-proposal)
        my-proposal? (not (nil? @my-proposal))
        *employer-address (get-in job [:job/employer :user/id])
        can-send-proposals? (and (not my-proposal?) (ilike!= active-user *employer-address))
        my-proposal-withdrawable? (and @my-proposal (= :proposal (:status @my-proposal)))


        user-query [:user {:user/id active-user}
                    [:user/is-registered-candidate]]
        arbitrations-query [:job {:job/id contract-address}
                            [[:arbitrations
                              [[:items
                                [:user/id]]]]]]
        result @(re/subscribe [::gql/query {:queries [user-query arbitrations-query]}])
        candidate-role? (and
                          (get-in result [:user :user/is-registered-candidate])
                          (not (ilike= active-user *employer-address))
                          (not (some #(ilike= active-user (:user/id %)) (get-in result [:job :arbitrations :items]))))]
    [:div.proposal-listing
     [:div.label "Proposals"]
     [c-scrollable
      {:forceVisible true :autoHide false}
      (into [c-table {:headers ["" "Candidate" "Rate" "Created" "Status"]}]
            (map (fn [proposal]
                   {:row-link (link-params {:route :route.job/contract :params {:job-story-id (:job-story/id proposal)}})
                    :row-cells [[:span (if (:current-user? proposal) "⭐" "")]
                                [:span (:candidate-name proposal)]
                                [:div (token-info/token-info-str (:rate proposal) (:token-details job))]
                                [:span (format/time-ago (new js/Date (:created-at proposal)))] ; TODO: remove new js/Date after switching to district.ui.graphql that converts Date GQL type automatically
                                [:span (:status proposal)]]})
                 @proposals))]
     [pagination/c-pagination-ends
      {:total-count proposal-total-count
       :limit proposal-limit
       :offset proposal-offset
       :set-offset-event :page.job-detail/set-proposal-offset}]

     (if candidate-role?
       [:div.proposal-form
        [:div.label "Send Proposal"]
        [:div
         [c-token-amount-input
          {:value (if my-proposal? (token-utils/human-amount (:rate @my-proposal) *job-token-type token-decimals) (:human-amount @*proposal-token-amount))
           :placeholder "Token amount"
           :decimals token-decimals
           :disabled (not can-send-proposals?)
           :on-change #(re/dispatch [:page.job-detail/set-proposal-token-amount %])}]
         [:label.post-label token-display-name]]
        [:label "The amount is for payment type: " (util.job/get-in-pair-vector util.job/bid-option *bid-option)]
        [:div.description-input
         [c-textarea-input
          {:disabled (not can-send-proposals?)
           :placeholder "Proposal Description"
           :value (if my-proposal? (:message @my-proposal) @*proposal-text)
           :on-change #(re/dispatch [:page.job-detail/set-proposal-text %])}]]

        (when my-proposal-withdrawable?
          [c-button {:color :warning :on-click (fn [] (>evt [:page.job-proposal/remove my-job-story-id]))
                     :size :small}
           [c-button-label "Remove"]])
        (when (not my-proposal?)
          [c-button {:style (when (not can-send-proposals?) {:background :gray})
                     :on-click (fn []
                                 (when can-send-proposals?
                                   (>evt [:page.job-proposal/send contract-address *job-token-type])))
                     :size :small}
           [c-button-label "Send"]])]

       [:div.proposal-form])]))


(defn c-participant-info
  [participant-type user-id]
  (let [rating-kw (keyword participant-type :rating)
        query [participant-type {:user/id user-id}
               [rating-kw
                :user/id
                [:user
                 [:user/id
                  :user/country
                  :user/name
                  :user/profile-image]]]]
        results (if user-id
                  @(re/subscribe [::gql/query {:queries [query]}])
                  {})
        *participant-name (get-in results [participant-type :user :user/name])
        *participant-address (get-in results [participant-type :user/id])
        *participant-rating (get-in results [participant-type rating-kw])
        *participant-country (get-in results [participant-type :user :user/country])
        *participant-profile-image (get-in results [participant-type :user :user/profile-image])]
    [:a.arbiter-detail (util.navigation/link-params
                         {:route :route.user/profile
                          :params {:address *participant-address}
                          :query {:tab participant-type}})
     [:div.header (clojure.string/capitalize (name participant-type))]
     [:div.profile-image [c-profile-image {:src *participant-profile-image}]]
     [:div.name *participant-name]
     [:div.rating [c-rating {:rating *participant-rating}]]
     [:div.location *participant-country]]))


(defn c-accept-arbiter-quote
  []
  (let [arbitration-to-accept @(re/subscribe [:page.job-detail/arbitration-to-accept])
        job-address (get arbitration-to-accept :job/id)
        employer-address (get-in arbitration-to-accept [:job :job/employer-address])
        arbiter-to-be-assigned? (= "quote-set" (:arbitration/status arbitration-to-accept))]
    [:div.proposal-form
     [:div.label "Accept arbiter quote"]
     [:div.amount-input
      [:div.label "Arbiter: "]
      [c-text-input
       {:placeholder ""
        :disabled true
        :value (get-in arbitration-to-accept [:arbiter :user :user/name])}]]
     [:div.amount-input
      [:div.label "Amount: "]
      [c-text-input
       {:placeholder ""
        :disabled true
        :value (token-utils/human-amount (get arbitration-to-accept :arbitration/fee) :eth)}]
      [:label "ETH (Ether)"]]

     (when arbiter-to-be-assigned?
       [c-button {:style (when (nil? arbitration-to-accept) {:background :gray})
                  :size :small
                  :on-click (fn []
                              (when arbitration-to-accept
                                (>evt [:page.job-detail/accept-quote-for-arbitration
                                       {:job/id job-address
                                        :employer employer-address
                                        :user/id (get-in arbitration-to-accept [:arbiter :user/id])
                                        :job-arbiter/fee (:arbitration/fee arbitration-to-accept)
                                        :job-arbiter/fee-currency-id :ETH}])))}
        [c-button-label "Accept"]])]))


(defn c-invite-arbiters
  [job-address]
  (let [selected-arbiters (re/subscribe [:page.job-detail/selected-arbiters])
        arbiter-fields [:arbiter/rating
                        [:arbiter/feedback [:total-count]]
                        [:user [:user/id :user/name]]]
        job-query [:job {:job/id job-address}
                   [:job/employer-address
                    [:arbitrations
                     [[:items
                       [[:arbiter
                         arbiter-fields]]]]]]]
        arbiters-query [:arbiter-search {:search-params {:name ""}}
                        [[:items
                          arbiter-fields]]]
        search-result @(re/subscribe [::gql/query {:queries [arbiters-query job-query]}
                                      {:refetch-on #{:page.job-detail/arbitrations-updated}}])
        employer-address (get-in search-result [:job :job/employer-address])
        all-arbiters (get-in search-result [:arbiter-search :items])
        not-employer (fn [arbiter] (ilike!= employer-address (get-in arbiter [:user :user/id])))
        arbiters-without-current-employer (filter not-employer all-arbiters)
        already-added (map #(get % :arbiter) (get-in search-result [:job :arbitrations :items]))
        uninvited-arbiters (clojure.set/difference (set arbiters-without-current-employer) (set already-added))

        nothing-added? (empty? @selected-arbiters)
        arbiter-info-fn (fn [arbiter]
                          (clojure.string/join
                            [(get-in arbiter [:user :user/name])
                             " "
                             (apply str (repeat (:arbiter/rating arbiter) "⭐"))
                             (apply str (repeat (- 5 (:arbiter/rating arbiter)) "☆"))
                             " ("
                             (get-in arbiter [:arbiter/feedback :total-count])
                             ")"]))
        arbiter-address-fn (comp :user/id :user)
        invite-arbiters-tx-in-progress? (re/subscribe [:page.job-detail/invite-arbiters-tx-in-progress?])]
    [:div.proposal-form
     [:div.label "Invite arbiter"]
     [c-chip-search-input
      {:chip-listing @selected-arbiters
       :on-chip-listing-change #(re/dispatch [:page.job-detail/set-selected-arbiters %])
       :auto-suggestion-listing uninvited-arbiters
       :label-fn arbiter-info-fn
       :value-fn arbiter-address-fn
       :allow-custom-chips? false
       :placeholder "Searh arbiter by name"}]

     [c-button
      {:style (when (or nothing-added? @invite-arbiters-tx-in-progress?) {:background :gray})
       :size :small
       :on-click (fn []
                   (when (and
                           (not (empty? @selected-arbiters))
                           (not @invite-arbiters-tx-in-progress?))
                     (>evt [:page.job-detail/invite-arbiters
                            {:job/id job-address
                             :employer employer-address
                             :arbiters (map arbiter-address-fn @selected-arbiters)}])))}
      [c-button-label "Invite"]]]))


(defn c-set-arbiter-quote
  [arbitration-by-current-user]
  (let [token-amount (re/subscribe [:page.job-detail/arbitration-token-amount])
        token-amount-usd (re/subscribe [:page.job-detail/arbitration-token-amount-usd])
        quote-set? (= "quote-set" (:arbitration/status arbitration-by-current-user))
        invited? (= "invited" (:arbitration/status arbitration-by-current-user))
        job-address (get arbitration-by-current-user :job/id)
        active-user (get-in arbitration-by-current-user [:arbiter :user/id])]
    (if invited?
      [:div.proposal-form
       [:div.label "Set quote to be arbiter"]
       [:div.amount-input
        [c-token-amount-input
         {:value @token-amount
          :decimals 3
          :placeholder "Token amount"
          :on-change #(re/dispatch [:page.job-detail/set-arbitration-token-amount %])}]
        [:label "ETH (Ether)"]]

       [:div.amount-input
        [c-text-input
         {:placeholder "USD amount"
          :type :number
          :disabled (not invited?)
          :value @token-amount-usd
          :on-change #(re/dispatch [:page.job-detail/set-arbitration-token-amount-usd (js/parseFloat %)])}]
        [:label "USD"]]

       [c-button
        {:style (when quote-set? {:background :gray})
         :size :small
         :on-click (fn []
                     (when invited? (>evt [:page.job-detail/set-quote-for-arbitration
                                           {:job/id job-address
                                            :user/id active-user
                                            :job-arbiter/fee @token-amount
                                            :job-arbiter/fee-currency-id :ETH}])))}
        [c-button-label "Send"]]]

      [:div.proposal-form
       [c-info-message
        "You already set the quote for arbitration. Now the employer must
        accept, which will transfer the quoted amount to you"]])))


(defn c-arbitrations-section
  [job-address]
  (let [limit @(re/subscribe [:page.job-detail/arbitrations-limit])
        offset @(re/subscribe [:page.job-detail/arbitrations-offset])
        active-user (:user/id @(re/subscribe [:ethlance.ui.subscriptions/active-session]))
        query [:job {:job/id job-address}
               [:job/id
                :job/employer-address
                [:job/arbiter
                 [:user/id]]
                [:arbitrations {:limit limit :offset offset}
                 [:total-count
                  [:items
                   [:id
                    :job/id
                    :arbitration/date-arbiter-accepted
                    :arbitration/fee
                    [:fee-token-details
                     [:token-detail/id
                      :token-detail/type
                      :token-detail/name
                      :token-detail/symbol]]
                    :arbitration/status
                    [:job
                     [:job/employer-address]]
                    [:arbiter
                     [:user/id
                      [:user
                       [:user/name]]]]]]]]]]
        result @(re/subscribe [::gql/query {:queries [query]} {:refetch-on #{:page.job-detail/arbitrations-updated}}])

        arbitrations (get-in result [:job :arbitrations :items])
        total-count (get-in result [:job :arbitrations :total-count])

        arbitration-by-current-user (first (filter #(ilike= active-user (get-in % [:arbiter :user/id])) arbitrations))
        viewer-role (cond
                      (ilike= active-user (get-in result [:job :job/employer-address]))
                      :employer

                      (not (nil? arbitration-by-current-user))
                      :invited-arbiter

                      :else
                      :other)

        arbitration-to-accept @(re/subscribe [:page.job-detail/arbitration-to-accept])
        job-arbiter (get-in result [:job :job/arbiter :user/id])
        job-arbitration (first (filter #(ilike= job-arbiter (get-in % [:arbiter :user/id])) arbitrations))

        arbiter-accepted? (= "accepted" (:arbitration/status job-arbitration))
        arbiter-selected? (not (nil? arbitration-to-accept))
        show-invite-arbiters? @(re/subscribe [:page.job-detail/show-invite-arbiters])
        arbiter-idle? @(re/subscribe [:page.job-detail/job-arbiter-idle])]
    ;; TODO: remove after figuring out why at events/initialize DB doesn't have web3 and contract-instance
    ;; setTimeout is because dispatching at first render web3 ist not ready and causes errors
    (js/setTimeout #(re/dispatch [:page.job-detail/fetch-job-arbiter-status]) 1000)
    [:div.proposal-listing
     [:div.label "Arbitrations"]
     [c-scrollable
      {:forceVisible true :autoHide false}
      (into [c-table {:headers ["" "Arbiter" "Rate" "Accepted at" "Status" ""]}]
            (map (fn [arbitration]
                   [[:span (cond
                             (ilike= active-user (get-in arbitration [:arbiter :user/id]))
                             "⭐"
                             (= "accepted" (get arbitration :arbitration/status))
                             "✅")]
                    [:span (get-in arbitration [:arbiter :user :user/name])]
                    [c-token-info (:arbitration/fee arbitration) (:fee-token-details arbitration)]
                    [:span (when (:arbitration/date-arbiter-accepted arbitration)
                             (format/time-ago (new js/Date (:arbitration/date-arbiter-accepted arbitration))))]
                    [:span (:arbitration/status arbitration)]
                    (cond
                      (and (= "quote-set" (:arbitration/status arbitration))
                           (or (not arbiter-accepted?) arbiter-idle?)
                           (= viewer-role :employer))
                      [:div.button.primary.active.small
                       {:style {:height "2em" :background (if (ilike= arbitration-to-accept arbitration)
                                                            "orange"
                                                            "")}
                        :on-click #(re/dispatch [:page.job-detail/set-arbitration-to-accept arbitration])}
                       [:div.button-label (if (ilike= arbitration-to-accept arbitration)
                                            "Selected"
                                            "Select")]]
                      (= "invited" (:arbitration/status arbitration))
                      [:div "(arbiter to set quote)"]

                      (= "quote-set" (:arbitration/status arbitration))
                      [:div "(employer to accept)"]

                      :else
                      [:div ""])])
                 arbitrations))]

     [pagination/c-pagination-ends
      {:total-count total-count
       :limit limit
       :offset offset
       :set-offset-event :page.job-detail/set-arbitrations-offset}]

     (case viewer-role
       :invited-arbiter
       [c-set-arbiter-quote arbitration-by-current-user]

       :employer
       (if (and arbiter-accepted? (not show-invite-arbiters?))
         [:div.proposal-form.profiles
          [c-participant-info :arbiter job-arbiter] ; TODO: Fix styling
          (when arbiter-idle?
            [c-info-message
             "Idle arbiter"
             [:div
              "This arbiter has unresolved dispute for more than 30 days. You can accept new one to replace him"
              [:div.button.primary.active
               {:on-click (fn [] (re/dispatch [:page.job-detail/set-show-invite-arbiters true]))}
               [:div.button-label "Invite arbiters"]]]])]

         (if (and arbiter-selected?
                  (not show-invite-arbiters?))
           [c-accept-arbiter-quote]
           [c-invite-arbiters job-address]))

       :other
       [:div.proposal-form])]))


(defn c-add-funds
  [contract-address token-id token-details]
  (let [amount @(re/subscribe [:page.job-detail/add-funds-amount])
        adding-funds? (re/subscribe [:page.job-detail/adding-funds?])
        add-funds-tx-in-progress? (re/subscribe [:page.job-detail/add-funds-tx-in-progress?])]
    (if @adding-funds?
      [:div.add-funds
       [c-token-amount-input
        {:value (:human-amount amount)
         :placeholder "Token amount"
         :decimals (:token-detail/decimals token-details)
         :on-change #(re/dispatch [:page.job-detail/set-add-funds-amount %])}]
       [c-button {:on-click (fn []
                              (when (not @add-funds-tx-in-progress?)
                                (>evt [:page.job-detail/finish-adding-funds
                                       contract-address
                                       token-details
                                       token-id
                                       (:token-amount amount)])))
                  :disabled? @add-funds-tx-in-progress?
                  :size :small}
        [c-button-label "Confirm"]]]

      [:div.add-funds
       [c-button {:on-click (fn []
                              (>evt [:page.job-detail/start-adding-funds true]))
                  :size :small}
        [c-button-label "Add funds"]]])))


(defn c-job-info-section
  [results]
  (let [active-user (:user/id @(re/subscribe [:ethlance.ui.subscriptions/active-session]))
        page-params (re/subscribe [:district.ui.router.subs/active-page-params])
        contract-address (:id @page-params)
        *title (:job/title results)
        *description (:job/description results)
        *sub-title (:job/category results)
        job-creation-time (.fromTimestamp goog.date.DateTime (:job/date-created results))
        *posted-time-relative (str "Posted " (format/time-ago job-creation-time))
        *posted-time-absolute (str "(" (format/format-local-date job-creation-time) ")")
        job-status (:job/status results)
        desc-from-vec (fn [options source job]
                        ((source job) (into {} options)))
        tag-definitions [{:desc "Estimated duration: " :source (partial desc-from-vec util.job/estimated-durations :job/estimated-project-length)}
                         {:desc "Required experience: " :source (partial desc-from-vec util.job/experience-level :job/required-experience-level)}
                         {:desc "Job status: " :source #(name (:job/status %))}
                         {:desc "Bid option: " :source (partial desc-from-vec util.job/bid-option :job/bid-option)}]
        *job-info-tags (remove nil?
                               (map (fn [tag-def]
                                      (when (and
                                              (not (nil? results))
                                              ((:source tag-def) results))
                                        (str (:desc tag-def) ((:source tag-def) results))))
                                    tag-definitions))
        *required-skills (:job/required-skills results)

        employer-id (get-in results [:job/employer :user/id])
        arbiter-id (get-in results [:job/arbiter :user/id])
        has-accepted-arbiter? (not (nil? (get results :job/arbiter)))
        token-details (get results :token-details)
        job-balance (get results :balance)

        invoices (get-in results [:invoices :items])
        unpaid-invoices (filter #(= "created" (:invoice/status %)) invoices)
        unresolved-disputes (filter #(= "dispute-raised" (:invoice/status %)) invoices)
        has-unpaid-invoices? (not (empty? unpaid-invoices))
        has-unresolved-disputes? (not (empty? unresolved-disputes))
        show-end-job? (and
                        (ilike= employer-id active-user)
                        (not= :ended job-status))
        job-ongoing? (not= :ended job-status)
        can-end-job? (not (or has-unpaid-invoices? has-unresolved-disputes?))
        end-job-tx-in-progress? (re/subscribe [:page.job-detail/end-job-tx-in-progress?])]
    [:div.header
     [:div.main
      [:div.title *title]
      [:div.sub-title *sub-title] ; TODO: where this comes from
      [:div.description {:style {:white-space "pre-wrap"}} *description]
      [:div.label "Required Skills"]
      [:div.skill-listing
       (for [skill *required-skills] [c-tag {:key skill} [c-tag-label skill]])]
      [:div.ticket-listing
       [:div.ticket
        [:div.label "Available Funds"]
        [c-token-info job-balance token-details]]]

      (when job-ongoing? [c-add-funds contract-address (:job/token-id results) token-details])
      [:div.profiles
       [c-participant-info :employer employer-id]
       (when has-accepted-arbiter? [c-participant-info :arbiter arbiter-id])]]
     [:div.side
      [:div.label *posted-time-relative]
      [:div.label *posted-time-absolute]
      (for [tag-text *job-info-tags] [c-tag {:key tag-text} [c-tag-label tag-text]])
      (when show-end-job?
        [:div
         [:div.button.primary.active
          {:style (when (or @end-job-tx-in-progress? has-unpaid-invoices? has-unresolved-disputes?) {:background :gray})
           :disabled @end-job-tx-in-progress?
           :on-click (fn []
                       (when (and can-end-job? (not @end-job-tx-in-progress?))
                         (re/dispatch [:page.job-detail/end-job {:job/id contract-address :employer employer-id}])))}
          [:div.button-label "End job"]]
         (when has-unpaid-invoices?
           [c-info-message "Job has unpaid invoices"
            [:ul
             (for [invoice unpaid-invoices]
               ^{:key (:id invoice)}
               [:li [:a (link-params {:route :route.invoice/index
                                      :params {:invoice-id (:invoice/id invoice) :job-id (:job/id invoice)}})
                     (str "Invoice #" (:invoice/id invoice))]])]])
         (when has-unresolved-disputes?
           [c-info-message  "Job has unresolved disputes"
            [:ul
             (for [invoice unresolved-disputes]
               ^{:key (:id invoice)}
               [:li [:a (link-params {:route :route.job/contract
                                      :params {:job-story-id (:job-story/id invoice)}})
                     (str "Dispute #" (:invoice/id invoice))]])]])
         (when can-end-job? [:div "Ending the job will return all remaining funds to who contributed them"])])]]))


(defmethod page :route.job/detail []
  (fn []
    (let [page-params (re/subscribe [:district.ui.router.subs/active-page-params])
          contract-address (:id @page-params)
          job-query [:job {:job/id contract-address}
                     [:job/id
                      :job/title
                      :job/description
                      :job/required-skills
                      :job/category
                      :job/status
                      :job/required-experience-level
                      :job/required-availability
                      :job/bid-option
                      :job/estimated-project-length
                      :job/date-created

                      :job/token-type
                      :job/token-amount
                      :job/token-address
                      :job/token-id
                      :balance

                      [:token-details
                       [:token-detail/id
                        :token-detail/type
                        :token-detail/name
                        :token-detail/decimals
                        :token-detail/symbol]]
                      [:invoices
                       [[:items
                         [:id
                          :invoice/id
                          :job/id
                          :job-story/id
                          :invoice/status]]]]
                      [:job/employer [:user/id]]
                      [:job/arbiter [:user/id]]]]
          query-results (re/subscribe [::gql/query
                                       {:queries [job-query]}
                                       {:refetch-on #{:page.job-detail/job-updated}}])
          [loading? processing?] (map @query-results [:graphql/loading? :graphql/preprocessing?])
          results (:job @query-results)]
      [c-main-layout {:container-opts {:class :job-detail-main-container}}
       [spinner-until-data-ready [loading? processing?]
        [c-job-info-section results]]
       [hidden-until-data-ready [loading? processing?] [c-proposals-section results]]
       [hidden-until-data-ready [loading? processing?] [c-arbitrations-section contract-address]]
       [hidden-until-data-ready [loading? processing?] [c-invoice-listing contract-address]]
       [hidden-until-data-ready [loading? processing?] [c-employer-feedback contract-address]]])))
