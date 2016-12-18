(ns ethlance.pages.freelancer-create-page
  (:require
    [cljs-react-material-ui.icons :as icons]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a center-layout]]
    [ethlance.components.user-forms :refer [user-form freelancer-form]]
    [ethlance.ethlance-db :as ethlance-db]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [goog.string :as gstring]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [cljs-react-material-ui.reagent :as ui]))

(defn freelancer-create-page []
  (let [register-freelancer-form (subscribe [:form.user/register-freelancer])]
    (fn []
      (let [{:keys [:loading? :data :errors]} @register-freelancer-form]
        [misc/only-unregistered
         [misc/center-layout
          [paper
           {:loading? loading?}
           [:h1 "Sign up as Freelancer"]
           [user-form
            {:user data
             :open? true
             :form-key :form.user/register-freelancer}]
           [freelancer-form
            {:user data
             :open? true
             :form-key :form.user/register-freelancer}]
           [misc/send-button
            {:disabled (or loading? (boolean (seq errors)))
             :on-touch-tap #(dispatch [:contract.user/register-freelancer data])}]]]]))))
