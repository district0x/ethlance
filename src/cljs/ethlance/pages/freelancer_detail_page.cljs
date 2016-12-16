(ns ethlance.pages.freelancer-detail-page
  (:require
    [cljs-react-material-ui.icons :as icons]
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.components.categories-chips :refer [categories-chips]]
    [ethlance.components.contracts-table :refer [contracts-table]]
    [ethlance.components.feedback-list :refer [feedback-list]]
    [ethlance.components.languages-chips :refer [languages-chips]]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a center-layout]]
    [ethlance.components.skills-chips :refer [skills-chips]]
    [ethlance.components.star-rating :refer [star-rating]]
    [ethlance.components.truncated-text :refer [truncated-text]]
    [ethlance.ethlance-db :as ethlance-db]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [goog.string :as gstring]
    [re-frame.core :refer [subscribe dispatch]]
    [ethlance.constants :as constants]
    [reagent.core :as r]))

(defn freelancer-info [{:keys [:user/gravatar :user/name :user/country :freelancer/job-title :freelancer/avg-rating
                               :freelancer/ratings-count :freelancer/hourly-rate :freelancer/total-earned
                               :freelancer/available? :user/created-on :freelancer/description
                               :freelancer/skills :freelancer/categories :user/languages :user/id
                               :user/status] :as user}]
  (when (seq name)
    [:div
     [row
      {:middle "xs"}
      [col
       {:xs 6 :md 2}
       [ui/avatar
        {:size 110
         :src (u/gravatar-url gravatar)}]]
      [col
       {:xs 6 :md 7}
       [:h1 name]
       [:h3 job-title]
       [star-rating
        {:value (u/rating->star avg-rating)
         :show-number? true
         :ratings-count ratings-count}]
       [misc/country-marker
        {:country country}]]
      [col {:xs 6 :md 3
            :style styles/text-right}
       [row-plain
        {:end "xs"}
        (case status
          1 [misc/status-chip
             {:background-color (styles/freelancer-available?-color available?)}
             (if available?
               "available for hire!"
               "not available for hire")]
          2 [misc/status-chip
             {:background-color styles/danger-color}
             "this user has been blocked"])]
       [misc/elegant-line "rate per hour" (u/eth hourly-rate)]
       [misc/elegant-line "earned" (u/eth total-earned)]]]
     [misc/hr]
     [:h4 {:style (merge styles/fade-text
                         {:margin-bottom 5})} "joined on " (u/format-date created-on)]
     [truncated-text description]
     [misc/subheader "Skills"]
     [skills-chips
      {:selected-skills skills
       :always-show-all? true}]
     [misc/subheader "Interested in categories"]
     [misc/call-on-change
      {:load-on-mount? true
       :args {id (select-keys user [:freelancer/categories-count])}
       :on-change #(dispatch [:after-eth-contracts-loaded [:contract.db/load-freelancer-categories %]])}
      [categories-chips
       {:value categories}]]
     [misc/subheader "Speaks languages"]
     [misc/call-on-change
      {:load-on-mount? true
       :args {id (select-keys user [:user/languages-count])}
       :on-change #(dispatch [:after-eth-contracts-loaded [:contract.db/load-user-languages %]])}
      [languages-chips
       {:value languages}]]]))

(defn freelancer-detail [{:keys [:user/id :user/name :user/gravatar :user/employer? :user/freelancer?] :as user}]
  [paper
   {:loading? (empty? name)
    :style styles/paper-section-main}
   (when (seq name)
     [:div
      [row
       {:middle "xs"
        :style styles/margin-bottom-gutter}
       [col
        {:xs 12 :md 6}
        [:h2 "Freelancer Profile"]]
       [col
        {:xs 12 :md 6
         :style styles/text-right}
        (when employer?
          [ui/raised-button
           {:label "See employer profile"
            :primary true
            :href (u/path-for :employer/detail :user/id id)}])]]
      (if freelancer?
        [freelancer-info user]
        [row-plain
         {:center "xs"}
         "This user is not registered as a freelancer"])])])

(defn freelancer-contracts [{:keys [:user/id]}]
  [contracts-table
   {:list-subscribe [:list/contracts :list/freelancer-contracts]
    :show-rate? true
    :show-total-paid? true
    :show-job? true
    :show-status? true
    :initial-dispatch {:list-key :list/freelancer-contracts
                       :fn-key :ethlance-views/get-freelancer-contracts
                       :load-dispatch-key :contract.db/load-contracts
                       :schema (select-keys ethlance-db/contract-all-schema
                                            [:contract/job :contract/created-on :proposal/rate :contract/total-paid
                                             :contract/status])
                       :args {:user/id id :contract/status 0 :job/status 0}}
    :all-ids-subscribe [:list/ids :list/freelancer-contracts]
    :title "Job Activity"
    :no-items-text "Freelancer has no job activity"}])

(defn freelancer-feedback [{:keys [:user/id]}]
  [feedback-list
   {:list-subscribe [:list/contracts :list/freelancer-feedbacks]
    :initial-dispatch [:list/load-ids {:list-key :list/freelancer-feedbacks
                                       :fn-key :ethlance-views/get-freelancer-contracts
                                       :load-dispatch-key :contract.db/load-contracts
                                       :schema ethlance-db/feedback-schema
                                       :args {:user/id id :contract/status 4 :job/status 0}}]}])

(defn employer-jobs-select-field [jobs-list {:keys [:contract/job]} freelancer active-user]
  [misc/call-on-change
   {:args (:user/id active-user)
    :load-on-mount? true
    :on-change #(dispatch [:contract.views/load-employer-jobs-for-freelancer-invite
                           {:employer/id % :freelancer/id (:user/id freelancer)}])}
   [ui/select-field
    {:floating-label-text "Job"
     :hint-text "Choose Job"
     :value (when (pos? job) job)
     :auto-width true
     :style styles/overflow-ellipsis
     :on-change #(dispatch [:form/value-changed :form.contract/add-invitation :contract/job %3])}
    (for [{:keys [:job/id :job/title]} (:items jobs-list)]
      [ui/menu-item
       {:value id
        :primary-text (gstring/format "%s (#%s)" title id)
        :key id}])]])

(defn invite-freelancer-form []
  (let [form-open? (r/atom false)
        active-user (subscribe [:db/active-user])
        form (subscribe [:form.contract/add-invitation])
        jobs-list (subscribe [:list/jobs :list/employer-jobs-open-select-field])]
    (fn [user]
      (let [{:keys [:loading? :errors :data]} @form
            {:keys [:invitation/description :contract/job]} data
            {:keys [:user/employer?]} @active-user]
        (when (and employer?
                   (= (:user/status @active-user) 1)
                   (= (:user/status user) 1))
          [paper
           {:loading? (or loading? (:loading? @jobs-list))}
           [row
            [col {:xs 6}
             (when @form-open?
               [:h2 "New Invitation"])]
            [col {:xs 6 :style styles/text-right}
             (when-not @form-open?
               [ui/raised-button
                {:label "Write Invitation"
                 :primary true
                 :on-touch-tap #(reset! form-open? true)
                 :icon (icons/content-create)}])]]
           (when @form-open?
             [:div
              [employer-jobs-select-field @jobs-list data user @active-user]
              [misc/textarea
               {:floating-label-text "Invitation Text"
                :form-key :form.contract/add-invitation
                :field-key :invitation/description
                :max-length-key :max-invitation-desc
                :default-value description}]
              [misc/send-button
               {:disabled (or loading? (boolean (seq errors)))
                :on-touch-tap #(dispatch [:contract.contract/add-job-invitation
                                          (merge data {:contract/freelancer (:user/id user)})])}]])])))))

(defn freelancer-detail-page []
  (let [user-id (subscribe [:user/route-user-id])
        user (subscribe [:user/detail])]
    (dispatch [:after-eth-contracts-loaded
               [:contract.db/load-users (merge ethlance-db/user-schema ethlance-db/freelancer-schema) [@user-id]]])
    (fn []
      [center-layout
       [freelancer-detail @user]
       [invite-freelancer-form @user]
       (when (:user/freelancer? @user)
         [:div
          [freelancer-contracts @user]
          [freelancer-feedback @user]])])))