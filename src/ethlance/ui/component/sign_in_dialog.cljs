(ns ethlance.ui.component.sign-in-dialog
  "Sign In Dialog, which makes use of modal component."
  (:require
   [re-frame.core :as re]

   ;; re-frame Prerequisites
   [ethlance.ui.component.modal.subscriptions]
   [ethlance.ui.events]
  
   ;; Ethlance Components
   [ethlance.ui.component.icon :refer [c-icon]]
   [ethlance.ui.component.button :refer [c-button c-button-label]]
   [ethlance.ui.component.modal :refer [c-modal]]))


(defn open! []
  (re/dispatch [:modal/open ::sign-in]))


(defn close! []
  (re/dispatch [:modal/close]))


(defn sign-in! []
  (re/dispatch [:user/sign-in])
  (close!))


(defn c-sign-in-dialog
  []
  (let [open? (re/subscribe [:modal/open? ::sign-in])]
    (fn []
      (when @open?
        [c-modal
         {:class "animation-fade-in-fast"}
         [:div.sign-in-dialog
          [c-icon 
           {:name :close
            :on-click close!
            :class "close-button"
            :color :secondary
            :title "Close Dialog"}]
          [:img.sign-in-dialog {:src "/images/svg/sign_in_dialog.svg"}]
          [:h1 "Sign In and Verify Address"]
          [:p "After clicking \"Continue\", a wallet dialogue will prompt you to verify your unique address."]
          [:p "Once you verify, you will be signed in to the network."]
          [:div.button-listing
           [c-button
            {:color :primary
             :on-click sign-in!}
            [c-button-label [:span "Continue"]]]]]]))))

