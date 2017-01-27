(ns ethlance.pages.user-edit-page
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [clojure.set :as set]
    [ethlance.db :refer [default-db]]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a center-layout]]
    [ethlance.components.user-forms :refer [user-form freelancer-form employer-form]]
    [ethlance.ethlance-db :as ethlance-db]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [goog.string :as gstring]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

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

(defn user-edit-page []
  (let [set-user-form (subscribe [:form.user/set-user])
        set-freelancer-form (subscribe [:form.user/set-freelancer])
        set-employer-form (subscribe [:form.user/set-employer])
        active-user (subscribe [:db/active-user])]
    (fn []
      (let [{:keys [:user/id :user/freelancer? :user/employer?]} @active-user
            loading? (or (empty? (:user/name @active-user))
                         (and freelancer? (not (:freelancer/description @active-user)))
                         (and employer? (not (:employer/description @active-user)))
                         (:loading? @set-user-form)
                         (:loading? @set-freelancer-form)
                         (:loading? @set-employer-form))]
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
                            (dispatch [:form/clear-data :form.user/set-freelancer
                                       (when-not freelancer?
                                         (get-form-default-errors :form.user/register-freelancer :freelancer))])
                            (dispatch [:form/clear-data :form.user/set-employer
                                       (when-not employer?
                                         (get-form-default-errors :form.user/register-employer :employer))])
                            (dispatch [:contract.db/load-user-languages {user-id @active-user}])
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
                  :loading? loading?}])])]]]))))
