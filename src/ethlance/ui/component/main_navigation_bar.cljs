(ns ethlance.ui.component.main-navigation-bar
  (:require
   [re-frame.core :as re]

   ;; Ethlance Components
   [ethlance.ui.component.ethlance-logo :refer [c-ethlance-logo]]
   [ethlance.ui.component.profile-image :refer [c-profile-image]]

   ;; Ethlance Utils
   [ethlance.ui.util.navigation :as util.navigation]))


(defn c-main-navigation-bar
  "Main Navigation bar seen while the site is in desktop-mode."
  []
  [:div.main-navigation-bar
   [c-ethlance-logo
    {:color :white
     :size :small
     :title "Go to Home Page"
     :on-click (util.navigation/create-handler {:route :route/home})
     :href (util.navigation/resolve-route {:route :route/home})
     :inline? false}]
   [:div.profile
    [c-profile-image {:size :small}]
    [:div.name "Brian Curran"]]
   [:div.account-balances
    [:div.token-value "9.20 ETH"]
    [:div.usd-value "$1,337.0 USD"]]])
   
