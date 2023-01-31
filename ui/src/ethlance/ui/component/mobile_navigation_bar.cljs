(ns ethlance.ui.component.mobile-navigation-bar
  (:require
   [reagent.core :as r]
   [re-frame.core :as re]
   [district.ui.router.subs :as ui.router.subs]

   ;; Ethlance Components
   [ethlance.ui.component.ethlance-logo :refer [c-ethlance-logo]]
   [ethlance.ui.component.profile-image :refer [c-profile-image]]
   [ethlance.ui.component.icon :refer [c-icon]]

   ;; Ethlance Utils
   [ethlance.ui.util.navigation :as util.navigation]))


(defn- c-menu-item
  [{:keys [name label route]}]
  (fn []
    (let [*active-page (re/subscribe [::ui.router.subs/active-page])
          {active-route-name :name} @*active-page]
      [:a.nav-element
       {:title label
        :class (when (= route active-route-name) "active")
        :href (util.navigation/resolve-route {:route route})
        :on-click (util.navigation/create-handler {:route route})}
       [c-icon {:name name :color :primary :size :small}]
       [:span.label label]])))


(defn c-mobile-navigation-menu []
  [:div.mobile-navigation-menu
   [c-menu-item {:name :jobs :label "Jobs" :route :route.job/jobs}]
   [c-menu-item {:name :candidates :label "Candidates" :route :route.user/candidates}]
   [c-menu-item {:name :arbiters :label "Arbiters" :route :route.user/arbiters}]
   [c-menu-item {:name :about :label "About" :route :route.misc/how-it-works}]
   [c-menu-item {:name :sign-up :label "Sign Up" :route :route.me/sign-up}]
   [c-menu-item {:name :my-activity :label "My Activity" :route :route.me/index}]])


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
        [c-ethlance-logo
         {:color :white
          :size :small
          :title "Go to Home Page"
          :on-click (util.navigation/create-handler {:route :route/home})
          :href (util.navigation/resolve-route {:route :route/home})
          :inline? false}]]
       [:div.menu-button
        [c-icon {:name (if @*open? :close :list-menu)
                 :color :white
                 :size :large
                 :on-click #(swap! *open? not)}]]
       (when @*open?
         [:div.dropdown
          [c-mobile-navigation-menu]
          [c-mobile-account-page]])])))
