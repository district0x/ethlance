(ns ethlance.pages.employer-create-page
  (:require
    [cljs-react-material-ui.icons :as icons]
    [ethlance.components.misc :as misc :refer [col row paper row-plain line a center-layout]]
    [ethlance.components.user-forms :refer [user-form employer-form]]
    [ethlance.ethlance-db :as ethlance-db]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [goog.string :as gstring]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [cljs-react-material-ui.reagent :as ui]))

(defn employer-create-page []
  (let [register-employer-form (subscribe [:form.user/register-employer])]
    (fn []
      (let [{:keys [:loading? :data :errors]} @register-employer-form]
        [misc/only-unregistered
         [misc/center-layout
          [paper
           {:loading? loading?}
           [:h1 "Sign up as Employer"]
           [user-form
            {:user data
             :open? true
             :form-key :form.user/register-employer}]
           [employer-form
            {:user data
             :open? true
             :form-key :form.user/register-employer}]
           [misc/send-button
            {:disabled (or loading? (boolean (seq errors)))
             :on-touch-tap #(dispatch [:contract.user/register-employer data])}]]]]))))
