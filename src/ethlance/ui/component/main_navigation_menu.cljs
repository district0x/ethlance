(ns ethlance.ui.component.main-navigation-menu
  (:require
   [re-frame.core :as re]
   [district.ui.router.subs :as ui.router.subs]

   ;; Ethlance Components
   [ethlance.ui.component.icon :refer [c-icon]]

   ;; Ethlance Utils
   [ethlance.ui.util.navigation :as util.navigation]))


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
  [:div.main-navigation-menu
   [c-menu-item {:name :jobs :label "Jobs" :route :route.job/jobs}]
   [c-menu-item {:name :candidates :label "Candidates" :route :route.user/candidates}]
   [c-menu-item {:name :arbiters :label "Arbiters" :route :route.user/arbiters}]
   [c-menu-item {:name :about :label "About" :route :route.misc/about}]
   [c-menu-item {:name :sign-up :label "Sign Up" :route :route.me/sign-up}]
   [c-menu-item {:name :my-activity :label "My Activity" :route :route.me/index}]])
