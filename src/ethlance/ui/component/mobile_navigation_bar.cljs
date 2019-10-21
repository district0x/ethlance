(ns ethlance.ui.component.mobile-navigation-bar
  (:require
   [reagent.core :as r]

   ;; Ethlance Components
   [ethlance.ui.component.ethlance-logo :refer [c-ethlance-logo]]
   [ethlance.ui.component.profile-image :refer [c-profile-image]]
   [ethlance.ui.component.icon :refer [c-icon]]))


(defn- c-menu-item
  [{:keys [name label route active?]}]
  [:div.nav-element
   [c-icon {:name name :color :primary :size :small}]
   [:span.label label]])


(defn c-mobile-navigation-menu []
  [:div.mobile-navigation-menu
   [c-menu-item {:name :jobs :label "Jobs"}]
   [c-menu-item {:name :candidates :label "Candidates"}]
   [c-menu-item {:name :arbiters :label "Arbiters"}]
   [c-menu-item {:name :about :label "About"}]
   [c-menu-item {:name :sign-up :label "Sign Up"}]
   [c-menu-item {:name :my-activity :label "My Activity"}]])


(defn c-mobile-account-page []
  [:div.mobile-account-page
   [:div.account-profile
    [c-profile-image {}]
    [:span.name "Brian Curran"]]
   [:div.account-balance
    [:span.token-value "9.20 ETH"]
    [:span.usd-value "$1,337.00"]]])


(defn c-mobile-navigation-bar []
  (let [*open? (r/atom false)]
    (fn []
      [:div.mobile-navigation-bar
       [:div.logo
        [c-ethlance-logo {:color :white :size :small}]]
       [:div.menu-button
        [c-icon {:name (if @*open? :close :list-menu)
                 :color :white
                 :size :large
                 :on-click #(swap! *open? not)}]]
       (when @*open?
         [:div.dropdown
          [c-mobile-navigation-menu]
          [c-mobile-account-page]])])))
