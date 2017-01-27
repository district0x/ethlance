(ns ethlance.pages.freelancer-detail-page
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [clojure.set :as set]
    [ethlance.components.categories-chips :refer [categories-chips]]
    [ethlance.components.contracts-table :refer [contracts-table]]
    [ethlance.components.feedback-list :refer [feedback-list]]
    [ethlance.components.icons :as icons]
    [ethlance.components.languages-chips :refer [languages-chips]]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a center-layout currency]]
    [ethlance.components.skills-chips :refer [skills-chips]]
    [ethlance.components.star-rating :refer [star-rating]]
    [ethlance.constants :as constants]
    [ethlance.ethlance-db :as ethlance-db]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [goog.string :as gstring]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn freelancer-info []
  (let [xs-width? (subscribe [:window/xs-width?])]
    (fn [{:keys [:user/gravatar :user/name :user/country :user/state :freelancer/job-title :freelancer/avg-rating
                 :freelancer/ratings-count :freelancer/hourly-rate :freelancer/total-earned
                 :freelancer/available? :user/created-on :freelancer/description
                 :freelancer/skills :freelancer/categories :user/languages :user/id
                 :user/status :user/address :user/balance] :as user}]
      [:div
       [row
        {:middle "xs"
         :center "xs"
         :start "sm"}
        [col
         {:xs 12 :sm 2}
         [ui/avatar
          {:size (if @xs-width? 150 100)
           :src (u/gravatar-url gravatar id)}]]
        [col
         {:xs 12 :sm 6 :lg 7
          :style (if @xs-width? {:margin-top 10} {})}
         [:h1 name]
         [:h3 job-title]
         [star-rating
          {:value (u/rating->star avg-rating)
           :show-number? true
           :ratings-count ratings-count
           :center "xs"
           :start "sm"
           :style (if @xs-width? {:margin-top 5} {})}]
         [misc/country-marker
          {:row-props {:center "xs"
                       :start "sm"
                       :style (if @xs-width? {:margin-top 5} {})}
           :country country
           :state state}]]
        [col {:xs 12 :sm 4 :lg 3
              :style (merge
                       {:padding-left 0}
                       (when-not @xs-width? styles/text-right))}
         [row-plain
          {:center "xs"
           :end "sm"}
          (case status
            1 [misc/status-chip
               {:style (merge {:margin-bottom 5}
                              (when @xs-width? {:margin-top 5}))
                :background-color (styles/freelancer-available?-color available?)}
               (if available?
                 "available for hire!"
                 "not available for hire")]
            2 [misc/blocked-user-chip]
            nil)]
         [misc/elegant-line "hourly rate" [currency hourly-rate]]
         [misc/elegant-line "earned" [currency total-earned]]
         [misc/elegant-line "balance" [currency balance]]]]
       [misc/hr]
       [misc/user-address address]
       [misc/user-created-on created-on]
       [misc/long-text
        description]
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
         {:value languages}]]])))

(defn freelancer-detail []
  (let [xs-width? (subscribe [:window/xs-width?])]
    (fn [{:keys [:user/id :user/name :user/gravatar :user/employer? :user/freelancer?] :as user}]
      [paper
       {:loading? (empty? name)
        :style styles/paper-section-main}
       (when (seq name)
         [:div
          [row
           {:middle "xs"
            :start "sm"
            :center "xs"
            :style styles/margin-bottom-gutter}
           [col
            {:xs 12 :sm 6}
            [:h2 "Freelancer Profile"]]
           [col
            {:xs 12 :sm 6
             :style (if @xs-width?
                      {:margin-top 10}
                      styles/text-right)}
            (when employer?
              [ui/flat-button
               {:label "Employer Profile"
                :primary true
                :href (u/path-for :employer/detail :user/id id)}])]]
          (if freelancer?
            [freelancer-info user]
            [row-plain
             {:center "xs"}
             "This user is not registered as a freelancer"])])])))

(defn freelancer-contracts []
  (let [xs-width? (subscribe [:window/xs-width?])]
    (fn [{:keys [:user/id]}]
      [contracts-table
       {:list-subscribe [:list/contracts :list/freelancer-contracts]
        :show-rate? (not @xs-width?)
        :show-total-paid? true
        :show-job? true
        :show-status? true
        :initial-dispatch {:list-key :list/freelancer-contracts
                           :fn-key :ethlance-views/get-freelancer-contracts
                           :load-dispatch-key :contract.db/load-contracts
                           :fields #{:contract/job
                                     :contract/created-on
                                     :proposal/rate
                                     :contract/total-paid
                                     :contract/status}
                           :args {:user/id id :contract/status 0 :job/status 0}}
        :all-ids-subscribe [:list/ids :list/freelancer-contracts]
        :title "Job Activity"
        :no-items-text "Freelancer has no job activity"}])))

(defn freelancer-feedback [{:keys [:user/id]}]
  [feedback-list
   {:list-subscribe [:list/contracts :list/freelancer-feedbacks]
    :list-db-path [:list/freelancer-feedbacks]
    :all-ids-subscribe [:list/ids :list/freelancer-feedbacks]
    :initial-dispatch [:list/load-ids {:list-key :list/freelancer-feedbacks
                                       :fn-key :ethlance-views/get-freelancer-contracts
                                       :load-dispatch-key :contract.db/load-contracts
                                       :fields ethlance-db/feedback-entity-fields
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
     :on-change #(dispatch [:form/set-value :form.contract/add-invitation :contract/job %3])}
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
                   (= (:user/status user) 1)
                   (:freelancer/available? user)
                   (not= (:user/id @active-user) (:user/id user)))
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
                 :icon (icons/pencil)}])]]
           (when @form-open?
             [:div
              [employer-jobs-select-field @jobs-list data user @active-user]
              [misc/textarea
               {:floating-label-text "Invitation Text"
                :form-key :form.contract/add-invitation
                :field-key :invitation/description
                :max-length-key :max-invitation-desc
                :value description
                :hint-text misc/privacy-warning-hint}]
              [misc/send-button
               {:disabled (or loading? (boolean (seq errors)))
                :on-touch-tap #(dispatch [:contract.contract/add-job-invitation
                                          (merge data {:contract/freelancer (:user/id user)})])}]])])))))

(defn freelancer-detail-page []
  (let [user-id (subscribe [:user/route-user-id])
        user (subscribe [:user/detail])]
    (fn []
      [misc/call-on-change
       {:load-on-mount? true
        :args @user-id
        :on-change #(dispatch [:after-eth-contracts-loaded
                               [:contract.db/load-users (set/union ethlance-db/user-entity-fields
                                                                   ethlance-db/freelancer-entity-fields
                                                                   ethlance-db/user-balance-entity-fields) [@user-id]]])}
       [center-layout
        [freelancer-detail @user]
        [invite-freelancer-form @user]
        (when (:user/freelancer? @user)
          [:div
           [freelancer-contracts @user]
           [freelancer-feedback @user]])]])))