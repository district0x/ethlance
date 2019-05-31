(ns ethlance.ui.component.main-layout
  (:require
   [re-frame.core :as rf]
   
   ;; Ethlance Components
   [ethlance.ui.component.circle-button :refer [c-circle-icon-button]]
   [ethlance.ui.component.main-navigation-bar :refer [c-main-navigation-bar]]
   [ethlance.ui.component.main-navigation-menu :refer [c-main-navigation-menu]]))


(defn c-main-layout
  []       
  [:div.main-layout
   [c-main-navigation-bar]
   [c-main-navigation-menu]
   [:div.content "Main Content"]
   [:div.footer
    [:div.copyright "Copyright © 2019 Ethlance.com | All rights reserved."]
    [:div.social
     [c-circle-icon-button {:name :facebook :size :small}]
     [c-circle-icon-button {:name :twitter :size :small}]
     [c-circle-icon-button {:name :github :size :small}]
     [c-circle-icon-button {:name :slack :size :small}]]]])
