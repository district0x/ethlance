(ns ethlance.ui.component.main-navigation-menu
  (:require
   [re-frame.core :as re]

   ;; Ethlance Components
   [ethlance.ui.component.icon :refer [c-icon]]))


(defn- c-menu-item
  [{:keys [name label route active?]}]
  [:div.nav-element
   [c-icon {:name name :color :primary :size :small}]
   [:span.label label]])


(defn c-main-navigation-menu
  []
  [:div.main-navigation-menu
   [c-menu-item {:name :jobs :label "Jobs"}]
   [c-menu-item {:name :candidates :label "Candidates"}]
   [c-menu-item {:name :arbiters :label "Arbiters"}]
   [c-menu-item {:name :about :label "About"}]
   [c-menu-item {:name :sign-up :label "Sign Up"}]
   [c-menu-item {:name :my-activity :label "My Activity"}]])
