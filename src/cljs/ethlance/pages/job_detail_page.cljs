(ns ethlance.pages.job-detail-page
  (:require
    [cljs-react-material-ui.icons :as icons]
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.contracts-table :refer [contracts-table]]
    [ethlance.components.feedback-list :refer [feedback-list]]
    [ethlance.components.invoices-table :refer [invoices-table]]
    [ethlance.components.list-pagination :refer [list-pagination]]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a currency]]
    [ethlance.components.skills-chips :refer [skills-chips]]
    [ethlance.components.star-rating :refer [star-rating]]
    [ethlance.constants :as constants]
    [ethlance.ethlance-db :as ethlance-db]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    ))


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

(defn job-proposals []
  (let [active-user-id (subscribe [:db/active-user-id])
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
                             :schema (select-keys ethlance-db/contract-all-schema
                                                  [:contract/freelancer :proposal/rate :proposal/created-on
                                                   :invitation/created-on :contract/status])
                             :args {:job/id job-id :contract/status 0}}
          :all-ids-subscribe [:list/ids :list/job-proposals]
          :title "Proposals"
          :no-items-text "No proposals for this job"}]))))

(defn create-proposal-allowed? [{:keys [:contract/status]}]
  (or (not status) (= status 1)))

(defn job-action-buttons [contract form-open?]
  (let [{:keys [:contract/status :contract/id]} contract]
    [row-plain
     {:end "xs"}
     (when (and (not @form-open?) (create-proposal-allowed? contract))
       [ui/raised-button
        {:label "Write Proposal"
         :primary true
         :on-touch-tap #(reset! form-open? true)
         :style styles/detail-action-button
         :icon (icons/content-create)}])
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
         :icon (icons/content-create)}])]))

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
                   (not= (:user/id (:job/employer @job)) (:user/id @active-user)))
          [paper
           {:loading? loading?}
           [row
            [col {:xs 12 :sm 6}
             (when (and @form-open? (create-proposal-allowed? @contract))
               [:h2 "New Proposal"])]
            [col {:xs 12 :sm 6}
             [job-action-buttons @contract form-open?]]]
           (when (and @form-open? (create-proposal-allowed? @contract))
             [:div
              [misc/ether-field
               {:floating-label-text (str (constants/payment-types (:job/payment-type @job)) " Rate (Ether)")
                :value rate
                :form-key :form.contract/add-proposal
                :field-key :proposal/rate}]
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
                                          (merge data {:contract/job (:job/id @job)})])}]])
           ])))))

(defn job-details []
  (let [job (subscribe [:job/detail])
        job-id (subscribe [:job/route-job-id])
        my-job? (subscribe [:job/my-job?])
        set-hiring-done-form (subscribe [:form.job/set-hiring-done])
        xs-width? (subscribe [:window/xs-width?])]
    (dispatch [:after-eth-contracts-loaded [:contract.db/load-jobs ethlance-db/job-schema [@job-id]]])
    (fn []
      (let [{:keys [:job/title :job/id :job/payment-type :job/estimated-duration
                    :job/experience-level :job/hours-per-week :job/created-on
                    :job/description :job/budget :job/skills :job/category
                    :job/status :job/hiring-done-on :job/freelancers-needed
                    :job/employer]} @job]
        [paper
         {:loading? (or (empty? (:user/name employer)) (:loading @set-hiring-done-form))
          :style styles/paper-section-main}
         (when id
           [:div
            [:h1 title]
            [:h3 {:style {:margin-top 5}} (constants/categories category)]
            [:h4 {:style styles/fade-text} "Posted on " (u/format-datetime created-on)]
            (when hiring-done-on
              [:h4 {:style styles/fade-text} "Hiring done on " (u/format-datetime hiring-done-on)])
            [row-plain
             {:style (merge styles/margin-top-gutter-less
                            styles/margin-bottom-gutter-less)}
             [misc/status-chip
              {:background-color (styles/job-status-colors status)
               :style styles/job-status-chip}
              (constants/job-statuses status)]
             [misc/status-chip
              {:background-color (styles/job-payment-type-colors payment-type)
               :style styles/job-status-chip}
              (constants/payment-types payment-type) " Rate"]
             [misc/status-chip
              {:background-color (styles/job-estimation-duration-colors estimated-duration)
               :style styles/job-status-chip}
              "For " (constants/estimated-durations estimated-duration)]
             [misc/status-chip
              {:background-color (styles/job-experience-level-colors experience-level)
               :style styles/job-status-chip}
              "For " (constants/experience-levels experience-level)]
             [misc/status-chip
              {:background-color (styles/job-hours-per-week-colors hours-per-week)
               :style styles/job-status-chip}
              (constants/hours-per-weeks hours-per-week)]
             (when (u/big-num-pos? budget)
               [misc/status-chip
                {:background-color styles/budget-chip-color
                 :style styles/job-status-chip}
                "Budget " [currency budget]])]
            [misc/long-text
             description]
            [misc/subheader "Required Skills"]
            [skills-chips
             {:selected-skills skills
              :always-show-all? true}]
            [misc/hr]
            [employer-details]
            (when (and @my-job? (= status 1))
              [row-plain
               {:end "sm" :center "xs"
                :style (when @xs-width? {:margin-top 10})}
               [ui/raised-button
                {:label "Close Hiring"
                 :secondary true
                 :on-touch-tap #(dispatch [:contract.job/set-hiring-done {:job/id id}])}]])])]))))

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
                           :schema ethlance-db/invoices-table-schema
                           :args {:job/id @job-id :invoice/status 0}}
        :all-ids-subscribe [:list/ids :list/job-invoices]}])))

(defn job-feedbacks []
  (let [job-id (subscribe [:job/route-job-id])]
    (fn []
      [feedback-list
       {:list-subscribe [:list/contracts :list/job-feedbacks {:loading-till-freelancer? true}]
        :initial-dispatch [:list/load-ids {:list-key :list/job-feedbacks
                                           :fn-key :ethlance-views/get-job-contracts
                                           :load-dispatch-key :contract.db/load-contracts
                                           :schema ethlance-db/feedback-schema
                                           :args {:job/id @job-id :contract/status 4}}]}])))

(defn job-detail-page []
  (let [job-id (subscribe [:job/route-job-id])]
    (dispatch [:after-my-users-loaded [:contract.views/load-my-freelancers-contracts-for-job {:job/id @job-id}]])
    (fn []
      [misc/center-layout
       [job-details]
       [job-proposal-form]
       [job-proposals @job-id]
       [job-invoices]
       [job-feedbacks]])))
