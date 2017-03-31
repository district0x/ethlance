(ns ethlance.components.confirm-dialog
  (:require
    [cljs-react-material-ui.reagent :as ui]
    [cljs-web3.core :as web3]
    [ethlance.constants :as constants]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn create-confirm-dialog-action-buttons [{:keys [:confirm-button-props]}]
  [(r/as-element
     [ui/flat-button
      {:label "Cancel"
       :primary true
       :keyboard-focused true
       :on-touch-tap #(dispatch [:dialog/close])}])
   (r/as-element
     [ui/flat-button
      (r/merge-props
        {:label "Confirm"
         :secondary true
         :on-touch-tap (fn []
                         (dispatch [:dialog/close])
                         (when (fn? (:on-confirm confirm-button-props))
                           ((:on-confirm confirm-button-props))))}
        (dissoc confirm-button-props :on-confirm))])])
