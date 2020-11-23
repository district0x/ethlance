(ns ethlance.ui.component.main-navigation-menu
  (:require
   [district.ui.router.subs :as ui.router.subs]
   [district.ui.web3-accounts.subs :as accounts-subs]
   [ethlance.ui.component.icon :refer [c-icon]]
   [ethlance.ui.subscriptions :as ethlance-subs]
   [ethlance.ui.util.navigation :as util.navigation]
   [re-frame.core :as re :refer [subscribe]]
   ))

(defn- c-menu-item
  "Menu Item used within the navigation menu."
  [{:keys [name label route]}]
  (fn []
    (let [*active-page (re/subscribe [::ui.router.subs/active-page])
          {active-route-name :name} @*active-page]
      [:a.nav-element
       {:title label
        :class (when (= route active-route-name) "active")
        :href (util.navigation/resolve-route {:route route})
        :on-click (util.navigation/create-handler {:route route})}
       [c-icon {:name name :color :white :size :small :inline? false}]
       [:span.label label]])))


(defn c-main-navigation-menu
  "Main Navigation Menu seen while the ethlance website is in desktop-mode."
  []
  (fn []
    (let [active-user (subscribe [::ethlance-subs/active-user])
          active-account (subscribe [::accounts-subs/active-account])]
      [:div.main-navigation-menu
       [c-menu-item {:name :jobs :label "Jobs" :route :route.job/jobs}]
       [c-menu-item {:name :candidates :label "Candidates" :route :route.user/candidates}]
       [c-menu-item {:name :arbiters :label "Arbiters" :route :route.user/arbiters}]
       [c-menu-item {:name :about :label "About" :route :route.misc/about}]
       (when (and @active-account (not @active-user))
        [c-menu-item {:name :sign-up :label "Sign Up" :route :route.me/sign-up}])
       (when @active-user
        [c-menu-item {:name :my-activity :label "My Activity" :route :route.me/index}])])))
