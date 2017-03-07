(ns ethlance.pages.user-edit-page
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [clojure.set :as set]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a center-layout]]
    [ethlance.components.user-forms :refer [user-form freelancer-form employer-form]]
    [ethlance.constants :as constants]
    [ethlance.db :refer [default-db]]
    [ethlance.ethlance-db :as ethlance-db]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [goog.string :as gstring]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(def user-fields (set/union ethlance-db/user-notifications-fields
                            #{:user/linkedin :user/github}))

(def freelancer-fields
  (set/difference ethlance-db/freelancer-entity-fields
                  #{:freelancer/avg-rating
                    :freelancer/ratings-count
                    :freelancer/total-earned
                    :freelancer/total-invoiced}))

(def employer-fields
  (set/difference ethlance-db/employer-entity-fields
                  #{:employer/avg-rating
                    :employer/ratings-count
                    :employer/total-paid
                    :employer/total-invoiced}))

(defn get-form-default-errors [form-key nmsp]
  (set (u/filter-by-namespace nmsp (get-in default-db [form-key :errors]))))

(defn dispatch-notif-form [key value]
  (dispatch [:form/set-value :form.user2/set-user-notifications key value]))

(def notifications
  [[:user.notif/disabled-on-job-invitation-added? "When I get job invitation" :user/freelancer?]
   [:user.notif/disabled-on-job-contract-added? "When I get hired" :user/freelancer?]
   [:user.notif/disabled-on-invoice-paid? "When my invoice is paid" :user/freelancer?]
   [:user.notif/disabled-on-job-proposal-added? "When my job receives a job proposal" :user/employer?]
   [:user.notif/disabled-on-invoice-added? "When I receive invoice to pay" :user/employer?]
   [:user.notif/disabled-on-job-contract-feedback-added? "When I receive feedback"]
   [:user.notif/disabled-newsletter? "Ethlance news & announcements (occasional)"]])

(defn user-notifications-form []
  (let [set-user-notifications (subscribe [:form.user2/set-user-notifications])
        active-user (subscribe [:db/active-user])]
    (fn [props]
      (let [{:keys [:data :loading? :errors]} @set-user-notifications
            {:keys [:user.notif/disabled-all? :user.notif/job-recommendations]} data
            {:keys [:user/freelancer?]} @active-user]
        [:div
         [ui/toggle
          {:label "Disable all email notifications"
           :toggled (boolean disabled-all?)
           :label-position "right"
           :on-toggle #(dispatch-notif-form :user.notif/disabled-all? %2)
           :style styles/margin-bottom-gutter-less}]
          (for [[key label pred] notifications]
           (when (or (not pred)
                     (pred @active-user))
             [ui/checkbox
              {:key key
               :label label
               :checked (not (key data))
               :disabled disabled-all?
               :on-check #(dispatch-notif-form key (not %2))}]))
         (when freelancer?
           [ui/select-field
            {:floating-label-text "Send me job recommendations"
             :value (if (zero? job-recommendations) 1 job-recommendations)
             :on-change #(dispatch-notif-form :user.notif/job-recommendations %3)
             :disabled disabled-all?}
            (for [[key label] constants/job-recommendations]
              [ui/menu-item
               {:value key
                :primary-text label
                :key key}])])
         [misc/send-button
          {:label "Save Notifications"
           :disabled (or loading? (boolean (seq errors)) (:loading? props))
           :on-touch-tap #(dispatch [:contract.user2/set-user-notifications data])}]]))))

(defn user-edit-page []
  (let [set-user-form (subscribe [:form.user/set-user])
        set-freelancer-form (subscribe [:form.user/set-freelancer])
        set-employer-form (subscribe [:form.user/set-employer])
        set-user-notifications (subscribe [:form.user2/set-user-notifications])
        active-user (subscribe [:db/active-user])]
    (fn []
      (let [{:keys [:user/id :user/freelancer? :user/employer?]} @active-user
            loading? (or (empty? (:user/name @active-user))
                         (not (boolean? (:user.notif/disabled-all? @active-user)))
                         (and freelancer? (not (:freelancer/description @active-user)))
                         (and employer? (not (:employer/description @active-user)))
                         (:loading? @set-user-form)
                         (:loading? @set-freelancer-form)
                         (:loading? @set-employer-form)
                         (:loading? @set-user-notifications))]
        [misc/only-registered
         [center-layout
          [paper
           {:style {:min-height 700}
            :loading? loading?}
           (when (seq (:user/name @active-user))
             [misc/call-on-change
              {:args id
               :load-on-mount? true
               :on-change (fn [user-id]
                            (dispatch [:form/set-open? :form.user/set-employer employer?])
                            (dispatch [:form/set-open? :form.user/set-freelancer freelancer?])
                            (dispatch [:form/clear-data :form.user/set-user])
                            (dispatch [:form/clear-data :form.user2/set-user-notifications])
                            (dispatch [:form/clear-data :form.user/set-freelancer
                                       (if-not freelancer?
                                         (get-form-default-errors :form.user/register-freelancer :freelancer)
                                         #{})])
                            (dispatch [:form/clear-data :form.user/set-employer
                                       (if-not employer?
                                         (get-form-default-errors :form.user/register-employer :employer)
                                         #{})])
                            (dispatch [:contract.db/load-user-languages {user-id @active-user}])
                            (dispatch [:after-eth-contracts-loaded
                                       [:contract.db/load-users user-fields [user-id]]])
                            (when employer?
                              (dispatch [:after-eth-contracts-loaded
                                         [:contract.db/load-users employer-fields [user-id]]]))
                            (when freelancer?
                              (dispatch [:after-eth-contracts-loaded
                                         [:contract.db/load-users freelancer-fields [user-id]]])
                              (dispatch [:contract.db/load-freelancer-categories {user-id @active-user}])))}
              [:h2 "User Information"]
              (let [{:keys [:data :errors]} @set-user-form]
                [user-form
                 {:user data
                  :form-key :form.user/set-user
                  :show-save-button? true
                  :errors errors
                  :loading? loading?}])
              [:h2
               {:style styles/margin-top-gutter-more}
               "Freelancer Information"]
              (let [{:keys [:data :open? :errors]} @set-freelancer-form]
                [freelancer-form
                 {:user data
                  :open? open?
                  :form-key :form.user/set-freelancer
                  :show-save-button? true
                  :show-add-more-skills? true
                  :errors errors
                  :loading? loading?}])
              [:h2
               {:style styles/margin-top-gutter-more}
               "Employer Information"]
              (let [{:keys [:data :open? :errors]} @set-employer-form]
                [employer-form
                 {:user data
                  :open? open?
                  :form-key :form.user/set-employer
                  :show-save-button? true
                  :errors errors
                  :loading? loading?}])
              [:h2
               {:style (merge styles/margin-top-gutter-more
                              styles/margin-bottom-gutter)}
               "Notifications"]
              [user-notifications-form
               {:loading? loading?}]])]]]))))
