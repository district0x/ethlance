(ns ethlance.ui.component.main-navigation-bar
  (:require
   [re-frame.core :as re]

   ;; Ethlance Components
   [ethlance.ui.component.ethlance-logo :refer [c-ethlance-logo]]))


(defn c-main-navigation-bar
  []
  [:div.main-navigation-bar
   [c-ethlance-logo {:color :white :size :small}]
   [:div.profile "profile"]
   [:div.account-transactions "txs"]])
   
