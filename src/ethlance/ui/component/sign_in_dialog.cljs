(ns ethlance.ui.component.sign-in-dialog
  "Sign In Dialog, which makes use of modal component."
  (:require
   [re-frame.core :as re]

   ;; re-frame Prerequisites
   [ethlance.ui.component.modal.subscriptions]
   [ethlance.ui.events]
  
   ;; Ethlance Components
   [ethlance.ui.component.button :refer [c-button c-button-label]]
   [ethlance.ui.component.modal :refer [c-modal]]))


(defn open! []
  (re/dispatch [:modal/open ::sign-in]))


(defn close! []
  (re/dispatch [:modal/close]))


(defn sign-in! []
  (re/dispatch [:sign-in])
  (close!))


(defn c-sign-in-dialog
  []
  (let [open? (re/subscribe [:modal/open? ::sign-in])]
   (fn []
     (when @open?
       [c-modal
        {:class "animation-fade-in-fast"}
        [:div.sign-in-dialog
         [:span.label "Sign In Dialog"]
         [c-button {:on-click sign-in!} [c-button-label "Sign"]]]]))))

