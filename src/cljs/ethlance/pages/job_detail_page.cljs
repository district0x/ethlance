(ns ethlance.pages.job-detail-page
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [cljs-web3.core :as web3]
    [clojure.set :as set]
    [ethlance.components.contracts-table :refer [contracts-table]]
    [ethlance.components.feedback-list :refer [feedback-list]]
    [ethlance.components.icons :as icons]
    [ethlance.components.invoices-table :refer [invoices-table]]
    [ethlance.components.list-pagination :refer [list-pagination]]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a currency]]
    [ethlance.components.skills-chips :refer [skills-chips]]
    [ethlance.components.sponsorships-table :refer [sponsorships-table]]
    [ethlance.components.star-rating :refer [star-rating]]
    [ethlance.constants :as constants]
    [ethlance.ethlance-db :as ethlance-db]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [medley.core :as medley]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn job-refunding? [job-status]
  (contains? #{5 6} job-status))

(defn employer-details []
  (let [job (subscribe [:job/detail])]
    (fn []
      (let [{:keys [:user/name :user/gravatar :user/id :employer/avg-rating
                    :employer/total-paid :employer/ratings-count :user/country
                    :user/balance :user/state]} (:job/employer @job)
            route-props {:route :employer/detail
                         :route-params {:user/id id}}]
        [row
         {:middle "xs"
          :center "xs"
          :start "sm"}
         [col
          {:xs 12 :sm 3 :md 2 :lg 3}
          [a route-props
           [ui/avatar
            {:size 100
             :src (u/gravatar-url gravatar id)}]]]
         [col
          [:h2 [a route-props name]]
          [row-plain
           {:middle "xs"}
           [star-rating
            {:value (u/rating->star avg-rating)
             :small? true
             :show-number? true
             :ratings-count ratings-count}]]
          [line [:span [currency total-paid] " spent"]]
          [line [:span [currency balance] " balance"]]
          [misc/country-marker
           {:row-props {:center "xs"
                        :start "sm"}
            :country country
            :state state}]]]))))

(defn my-contract? [active-user-id {:keys [:contract/freelancer]}]
  (= active-user-id (:user/id freelancer)))

(defn job-proposals-header []
  (let [job (subscribe [:job/detail])]
    (fn []
      [:div
       {:style (merge styles/margin-top-gutter-less
                      styles/fade-text)}
       "Bids for this job are fixed to " (u/currency-full-name (:job/reference-currency @job))])))

(defn job-proposals []
  (let [active-user-id (subscribe [:db/active-address])
        xs-width (subscribe [:window/xs-width?])]
    (fn [job-id]
      (when job-id
        [contracts-table
         {:list-subscribe [:list/contracts :list/job-proposals {:loading-till-freelancer? true}]
          :show-freelancer? true
          :show-invitation-or-proposal-time? (not @xs-width)
          :show-rate? true
          :show-status? true
          :highlight-row-pred (partial my-contract? @active-user-id)
          :initial-dispatch {:list-key :list/job-proposals
                             :fn-key :ethlance-views/get-job-contracts
                             :load-dispatch-key :contract.db/load-contracts
                             :fields ethlance-db/job-proposals-list-fields
                             :args {:job/id job-id :contract/status 0}}
          :all-ids-subscribe [:list/ids :list/job-proposals]
          :title "Proposals"
          :header job-proposals-header
          :no-items-text "No proposals for this job"}]))))

(defn add-sponsorship-form []
  (let [form-open? (r/atom false)
        form (subscribe [:form.sponsor/add-job-sponsorship])
        job (subscribe [:job/detail])
        eth-config (subscribe [:eth/config])]
    (fn []
      (let [{:keys [:job/id :job/status]} @job
            {:keys [:loading? :errors :data]} @form
            {:keys [:sponsorship/name :sponsorship/link :sponsorship/amount
                    :sponsorship/currency]} data
            min-ether-amount (web3/from-wei (:min-sponsorship-amount @eth-config) :ether)
            ether-amount (u/parse-float @(subscribe [:currency/ether-value amount currency]))
            valid-amount? (u/pos-ether-value? ether-amount)
            more-than-min-amount? (<= min-ether-amount ether-amount)]
        (when (contains? #{1 2} status)
          (if-not @form-open?
            [row-plain
             {:end "xs"}
             [ui/raised-button
              {:label "Sponsor"
               :primary true
               :style styles/margin-top-gutter-less
               :on-touch-tap #(reset! form-open? true)}]]
            [:div
             [misc/text-field
              {:floating-label-text "Your Name"
               :form-key :form.sponsor/add-job-sponsorship
               :field-key :sponsorship/name
               :max-length-key :max-sponsor-name
               :value name}]
             [misc/url-field
              {:floating-label-text "Your URL"
               :form-key :form.sponsor/add-job-sponsorship
               :field-key :sponsorship/link
               :max-length-key :max-sponsor-link
               :allow-empty? true
               :value link}]
             [misc/ether-field-with-currency-select-field
              {:ether-field-props
               {:floating-label-text "Amount"
                :form-key :form.sponsor/add-job-sponsorship
                :field-key :sponsorship/amount
                :value amount
                :only-positive? true}
               :currency-select-field-props
               {:value currency
                :on-change #(dispatch [:form/set-value :form.sponsor/add-job-sponsorship :sponsorship/currency %3])}}]
             [:h3
              {:style styles/margin-top-gutter-less}
              (let []
                (if valid-amount?
                  ether-amount
                  0))
              " "
              (u/currency-full-name 0)]
             (when (and (not more-than-min-amount?) valid-amount?)
               [:small
                {:style {:color styles/accent1-color}}
                "Min. sponsorship amount is " min-ether-amount " " (u/currency-full-name 0)])
             [misc/send-button
              {:disabled (or loading? (boolean (seq errors)) (not more-than-min-amount?))
               :on-touch-tap #(dispatch [:contract.sponsor/add-job-sponsorship
                                         (merge data {:sponsorship/job id
                                                      :sponsorship/amount ether-amount})])}]]))))))

(defn job-sponsorships-stats []
  (let [job (subscribe [:job/detail])]
    (fn []
      (let [{:keys [:job/sponsorships-total :job/sponsorships-balance :job/sponsorships-total-refunded
                    :job/status]} @job]
        [:div
         {:style styles/margin-top-gutter-less}
         [:h3 "Total Sponsored: " [currency sponsorships-total]]
         [:h3 "Current Balance: " [currency sponsorships-balance]]
         (when (job-refunding? status)
           [:h3 "Total Refunded: " [currency sponsorships-total-refunded]])]))))

(defn job-sponsorships []
  (let [xs-width? (subscribe [:window/xs-width?])
        job (subscribe [:job/detail])
        form (subscribe [:form.sponsor/add-job-sponsorship])]
    (fn []
      (let [{:keys [:job/status :job/id :job/sponsorable?]} @job
            refunding? (job-refunding? status)]
        (when sponsorable?
          [sponsorships-table
           {:list-subscribe [:list/sponsorships :list/job-sponsorships]
            :show-updated-on? (and (not refunding?) (not @xs-width?))
            :show-name? true
            :show-refunded-amount? refunding?
            :title "Sponsors"
            :no-items-text "Nobody sponsored this job yet"
            :loading? (:loading? @form)
            :show-position-number? true
            :header job-sponsorships-stats
            :link-to-sponsor-detail? true
            :initial-dispatch {:list-key :list/job-sponsorships
                               :fn-key :ethlance-views/get-job-sponsorships
                               :load-dispatch-key :contract.db/load-sponsorships
                               :fields ethlance-db/job-sponsorships-table-entity-fields
                               :args {:job/id id}}
            :all-ids-subscribe [:list/ids :list/job-sponsorships]}
           [add-sponsorship-form]])))))

(defn create-proposal-allowed? [{:keys [:job/invitation-only?]} {:keys [:contract/status]}]
  (or
    (and invitation-only? (= status 1))
    (and (not invitation-only?)
         (or
           (not status)
           (= status 1)))))

(defn job-action-buttons []
  (let [job (subscribe [:job/detail])
        contract (subscribe [:db/active-freelancer-job-detail-contract])]
    (fn [{:keys [:form-open?]}]
      (let [{:keys [:contract/status :contract/id]} @contract]
        [row-plain
         {:end "xs"}
         (when (and (not @form-open?) (create-proposal-allowed? @job @contract))
           [ui/raised-button
            {:label "Write Proposal"
             :primary true
             :on-touch-tap #(reset! form-open? true)
             :style styles/detail-action-button
             :icon (icons/pencil)}])
         (when (= status 1)
           [ui/raised-button
            {:label "You were invited!"
             :style (merge {:margin-left 10}
                           styles/detail-action-button)
             :href (u/path-for :contract/detail :contract/id id)
             :secondary true}])
         (when (>= status 2)
           [ui/raised-button
            {:label "My Proposal"
             :href (u/path-for :contract/detail :contract/id id)
             :style styles/detail-action-button
             :primary true
             :icon (icons/pencil)}])]))))

(defn job-proposal-form []
  (let [form-open? (r/atom false)
        form (subscribe [:form.contract/add-proposal])
        job (subscribe [:job/detail])
        active-user (subscribe [:db/active-user])
        contract (subscribe [:db/active-freelancer-job-detail-contract])]
    (fn []
      (let [{:keys [:loading? :errors :data]} @form
            {:keys [:proposal/description :proposal/rate]} data]
        (when (and (= (:job/status @job) 1)
                   (:user/freelancer? @active-user)
                   (not= (:user/id (:job/employer @job)) (:user/id @active-user))
                   (or (and (:job/invitation-only? @job)
                            (pos? (:contract/status @contract)))
                       (not (:job/invitation-only? @job))))
          [paper
           {:loading? loading?}
           [row
            [col {:xs 12 :sm 6}
             (when (and @form-open? (create-proposal-allowed? @job @contract))
               [:h2 "New Proposal"])]
            [col {:xs 12 :sm 6}
             [job-action-buttons
              {:form-open? form-open?}]]]
           (when (and @form-open? (create-proposal-allowed? @job @contract))
             [:div
              [misc/ether-field-with-currency
               {:floating-label-text (str (constants/payment-types (:job/payment-type @job)))
                :value rate
                :form-key :form.contract/add-proposal
                :field-key :proposal/rate
                :currency (:job/reference-currency @job)}]
              [misc/textarea
               {:floating-label-text "Proposal Text"
                :form-key :form.contract/add-proposal
                :field-key :proposal/description
                :max-length-key :max-proposal-desc
                :value description
                :hint-text misc/privacy-warning-hint}]
              [misc/send-button
               {:disabled (or loading? (boolean (seq errors)))
                :on-touch-tap #(dispatch [:contract.contract/add-job-proposal
                                          (merge data {:contract/job (:job/id @job)})])}]])])))))

(defn allowed-user-chip [{:keys [:approved?] :as props} & children]
  (into [ui/chip
         (r/merge-props
           {:label-style styles/overflow-ellipsis
            :style styles/overflow-ellipsis
            :background-color (if approved?
                                styles/allowed-user-approved-color
                                styles/allowed-user-not-approved-color)}
           (dissoc props :approved?))]
        children))

(defn allowed-users-list []
  (let [job (subscribe [:job/detail])]
    (fn []
      (let [{:keys [:job/allowed-users :job/status :job/id]} @job]
        [misc/call-on-change
         {:args allowed-users
          :load-on-mount? true
          :on-change (fn [allowed-users]
                       (dispatch [:contract.db/load-users #{:user/name
                                                            :user/freelancer?
                                                            :user/gravatar}
                                  allowed-users])
                       (when (= status 4)
                         (dispatch [:contract.views/load-job-approvals {:job/id id}])))}
         [misc/subheader "Accounts Allowed to spend Sponsorships"]
         (doall
           (for [allowed-user allowed-users]
             (let [{:keys [:user/name :user/freelancer? :user/gravatar :user/id]} @(subscribe [:user/by-id allowed-user])
                   allowed-user-approved? (if (= status 4)
                                            @(subscribe [:job/allowed-user-approved? (:job/id @job) allowed-user])
                                            true)]
               [row-plain
                {:start "xs"
                 :key allowed-user
                 :style {:margin-bottom 3}}
                (if name
                  [allowed-user-chip
                   {:key allowed-user
                    :approved? allowed-user-approved?
                    :on-touch-tap (u/nav-to-fn (if freelancer? :freelancer/detail :employer/detail)
                                               {:user/id id})}
                   [ui/avatar
                    {:src (u/gravatar-url gravatar id)}]
                   name]
                  [allowed-user-chip
                   {:key allowed-user
                    :approved? allowed-user-approved?
                    :on-touch-tap (fn [])}
                   [:a {:href (u/etherscan-url allowed-user)
                        :target :_blank
                        :style {:color styles/text-color}}
                    allowed-user]])])))]))))

(defn job-details []
  (let [job (subscribe [:job/detail])
        job-id (subscribe [:job/route-job-id])
        my-job? (subscribe [:job/my-job?])
        set-hiring-done-form (subscribe [:form.job/set-hiring-done])
        approve-sponsorable-job-form (subscribe [:form.job/approve-sponsorable-job])
        refund-sponsorships-form (subscribe [:form.sponsor/refund-job-sponsorships])
        xs-width? (subscribe [:window/xs-width?])
        waiting-for-my-approval? (subscribe [:job/waiting-for-my-approval?])]
    (fn []
      (let [{:keys [:job/title :job/id :job/payment-type :job/estimated-duration
                    :job/experience-level :job/hours-per-week :job/created-on
                    :job/description :job/budget :job/skills :job/category
                    :job/status :job/hiring-done-on :job/freelancers-needed
                    :job/employer :job/reference-currency :job/sponsorable? :job/invitation-only?
                    :job/allowed-users :job/sponsorships-balance :job/contracts-count
                    :job/sponsorships-count]} @job
            loading? (some true? (map :loading? [@set-hiring-done-form
                                                 @approve-sponsorable-job-form
                                                 @refund-sponsorships-form]))]
        [misc/call-on-change
         {:load-on-mount? true
          :args @job-id
          :on-change #(dispatch [:after-eth-contracts-loaded [:contract.db/load-jobs
                                                              (set/union ethlance-db/job-entity-fields
                                                                         #{:user/name :user/gravatar :employer/avg-rating
                                                                           :employer/total-paid :employer/ratings-count
                                                                           :user/country :user/balance :user/state})
                                                              [@job-id]]])}
         [paper
          {:loading? (or (empty? (:user/name employer))
                         (nil? description)
                         (and sponsorable? (empty? allowed-users))
                         loading?)
           :style styles/paper-section-main}
          (when id
            [:div
             [:h1 title]
             [:h3 {:style {:margin-top 5}} (constants/categories category)]
             [:h4 {:style styles/fade-text} "Created on " (u/format-datetime created-on)]
             (when hiring-done-on
               [:h4 {:style styles/fade-text} "Hiring closed on " (u/format-datetime hiring-done-on)])
             [row-plain
              {:style (merge styles/margin-top-gutter-less
                             styles/margin-bottom-gutter-less)}
              (when (pos? status)
                [misc/status-chip
                 {:background-color (styles/job-status-colors status)
                  :style styles/job-status-chip}
                 (constants/job-statuses status)])
              (when (and sponsorable? (not (contains? #{3 5 6} status)))
                [misc/status-chip
                 {:background-color styles/job-sposorable-chip-color
                  :style styles/job-status-chip}
                 "Looking for Sponsors"])
              (when invitation-only?
                [misc/status-chip
                 {:background-color styles/job-invitation-only-chip-color
                  :style styles/job-status-chip}
                 "Invitation Only"])
              (when (pos? payment-type)
                [misc/status-chip
                 {:background-color (styles/job-payment-type-colors payment-type)
                  :style styles/job-status-chip}
                 (constants/payment-types payment-type)])
              (when (pos? estimated-duration)
                [misc/status-chip
                 {:background-color (styles/job-estimation-duration-colors estimated-duration)
                  :style styles/job-status-chip}
                 "For " (constants/estimated-durations estimated-duration)])
              (when (pos? experience-level)
                [misc/status-chip
                 {:background-color (styles/job-experience-level-colors experience-level)
                  :style styles/job-status-chip}
                 "For " (constants/experience-levels experience-level)])
              (when (pos? hours-per-week)
                [misc/status-chip
                 {:background-color (styles/job-hours-per-week-colors hours-per-week)
                  :style styles/job-status-chip}
                 (constants/hours-per-weeks hours-per-week)])
              (when (u/big-num-pos? budget)
                [misc/status-chip
                 {:background-color styles/budget-chip-color
                  :style styles/job-status-chip}
                 "Budget " [currency budget {:value-currency reference-currency}]])
              (when (< 1 freelancers-needed)
                [misc/status-chip
                 {:background-color styles/freelancers-needed-status-color
                  :style styles/job-status-chip}
                 "Needs " freelancers-needed " freelancers"])]
             [misc/long-text
              description]
             [misc/subheader "Required Skills"]
             [skills-chips
              {:selected-skills skills
               :always-show-all? true}]
             (when sponsorable?
               [allowed-users-list])
             [misc/hr]
             [employer-details]
             [row-plain
              {:end "sm" :center "xs"
               :style (when @xs-width? {:margin-top 10})}
              (when (and @my-job? (= status 1))
                [ui/raised-button
                 {:label "Close Hiring"
                  :secondary true
                  :disabled loading?
                  :style styles/job-manage-button
                  :on-touch-tap #(dispatch [:contract.job/set-hiring-done {:job/id id}])}])
              (when (and @my-job?
                         (contains? #{1 2} status)
                         sponsorable?)
                (if (u/big-num-pos? sponsorships-balance)
                  [ui/raised-button
                   {:label "Refund Sponsors"
                    :primary true
                    :style styles/job-manage-button
                    :disabled loading?
                    :on-touch-tap #(dispatch [:dialog/open-confirmation
                                              {:title "Are you sure you want to start refunding?"
                                               :body
                                               (str
                                                 "After you start refunding sponsors, no more invoices can be
                                                  paid and no new hires can be made. Use this only when
                                                  you're done with the job. Sponsors will be refunded per " constants/refund-sponsors-limit
                                                 " due to gas limitations, so you might need to use this several
                                                 times to refund all sponsors.")
                                               :on-confirm (fn []
                                                             (dispatch [:contract.sponsor/refund-job-sponsorships
                                                                        {:sponsorship/job id
                                                                         :limit constants/refund-sponsors-limit}]))}])}]
                  [ui/raised-button
                   {:label "Close Job"
                    :secondary true
                    :style styles/job-manage-button
                    :disabled loading?
                    :on-touch-tap #(dispatch [:contract.sponsor/refund-job-sponsorships {:sponsorship/job id :limit 0}])}]))
              (when (and @my-job? (= status 5))
                [ui/raised-button
                 {:label "Continue Refunding"
                  :primary true
                  :style styles/job-manage-button
                  :disabled loading?
                  :on-touch-tap #(dispatch [:contract.sponsor/refund-job-sponsorships
                                            {:sponsorship/job id
                                             :limit constants/refund-sponsors-limit}])}])
              (when (and @my-job?
                         (zero? contracts-count)
                         (zero? sponsorships-count)
                         (contains? #{1 4} status))
                [ui/raised-button
                 {:label "Edit Job"
                  :primary true
                  :style styles/job-manage-button
                  :disabled loading?
                  :icon (icons/pencil)
                  :href (u/path-for :job/edit :job/id id)}])
              (when (and (= status 4) @waiting-for-my-approval?)
                [ui/raised-button
                 {:label "Approve"
                  :primary true
                  :icon (icons/check)
                  :disabled loading?
                  :style styles/job-manage-button
                  :on-touch-tap #(dispatch [:contract.job/approve-sponsorable-job {:job/id id}])}])]])]]))))

(defn job-invoices []
  (let [job-id (subscribe [:job/route-job-id])]
    (fn []
      [invoices-table
       {:list-subscribe [:list/invoices :list/job-invoices]
        :show-freelancer? true
        :show-status? true
        :initial-dispatch {:list-key :list/job-invoices
                           :fn-key :ethlance-views/get-job-invoices
                           :load-dispatch-key :contract.db/load-invoices
                           :fields #{:invoice/created-on
                                     :invoice/amount
                                     :invoice/status
                                     :invoice/contract
                                     :contract/freelancer
                                     :user/name}
                           :args {:job/id @job-id :invoice/status 0}}
        :all-ids-subscribe [:list/ids :list/job-invoices]}])))

(defn job-feedbacks []
  (let [job-id (subscribe [:job/route-job-id])]
    (fn []
      [feedback-list
       {:list-subscribe [:list/contracts :list/job-feedbacks {:loading-till-freelancer? true}]
        :list-db-path [:list/job-feedbacks]
        :all-ids-subscribe [:list/ids :list/job-feedbacks]
        :initial-dispatch [:list/load-ids {:list-key :list/job-feedbacks
                                           :fn-key :ethlance-views/get-job-contracts
                                           :load-dispatch-key :contract.db/load-contracts
                                           :fields ethlance-db/feedback-list-fields
                                           :args {:job/id @job-id :contract/status 4}}]}])))

(defn job-detail-page []
  (let [job-id (subscribe [:job/route-job-id])]
    (dispatch [:after-my-users-loaded [:contract.views/load-my-freelancers-contracts-for-job {:job/id @job-id}]])
    (fn []
      [misc/center-layout
       [job-details]
       [job-proposal-form]
       [job-sponsorships]
       [job-proposals @job-id]
       [job-invoices]
       [job-feedbacks]])))
