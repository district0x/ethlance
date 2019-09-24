(ns ethlance.ui.component.main-navigation-bar
  (:require
   [re-frame.core :as re]

   ;; Ethlance Components
   [ethlance.ui.component.ethlance-logo :refer [c-ethlance-logo]]
   [ethlance.ui.component.profile-image :refer [c-profile-image]]))


(defn c-main-navigation-bar
  []
  [:div.main-navigation-bar
   [c-ethlance-logo {:color :white :size :small}]
   [:div.profile
    [c-profile-image {:size :small}]
    [:div.name "Brian Curran"]]
   [:div.account-balances
    [:div.token-value "9.20 ETH"]
    [:div.usd-value "$1,337.0 USD"]]])
   
