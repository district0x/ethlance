(ns ethlance.ui.component.main-navigation-menu
  (:require
    [district.ui.graphql.subs :as gql]
    [district.ui.router.subs :as ui.router.subs]
    [district.ui.web3-accounts.subs :as accounts-subs]
    [ethlance.ui.component.icon :refer [c-icon]]
    [ethlance.ui.subscriptions :as ethlance-subs]
    [ethlance.ui.util.navigation :as util.navigation]
    [re-frame.core :as re]))


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
    (let [active-account @(re/subscribe [::accounts-subs/active-account])
          active-session @(re/subscribe [::ethlance-subs/active-session])
          active-user-id (or (:user/id active-session) active-account)
          query [:user {:user/id active-user-id}
                 [:user/id
                  :user/name
                  :user/profile-image]]
          result (re/subscribe [::gql/query {:queries [query]}])
          active-user (get @result :user)]
      [:div.main-navigation-menu
       [c-menu-item {:name :new-job :label "New Job" :route :route.job/new}]
       [c-menu-item {:name :jobs :label "Jobs" :route :route.job/jobs}]
       [c-menu-item {:name :candidates :label "Candidates" :route :route.user/candidates}]
       [c-menu-item {:name :arbiters :label "Arbiters" :route :route.user/arbiters}]
       [c-menu-item {:name :about :label "About" :route :route.misc/about}]
       (when (and active-account (not active-user))
         [c-menu-item {:name :sign-up :label "Sign Up" :route :route.me/sign-up}])
       (when active-user
         [c-menu-item {:name :my-activity :label "My Activity" :route :route.me/index}])])))
