(ns ethlance.ui.component.mobile-navigation-bar
  (:require
    [district.format :as format]
    [district.ui.router.subs :as ui.router.subs]
    [district.ui.conversion-rates.subs :as conversion-subs]
    [district.ui.graphql.subs :as gql]
    [district.ui.web3-account-balances.subs :as balances-subs]
    [district.ui.web3-accounts.subs :as accounts-subs]
    [district.web3-utils :as web3-utils]
    ;; Ethlance Components
    [ethlance.ui.component.ethlance-logo :refer [c-ethlance-logo]]
    [ethlance.ui.component.icon :refer [c-icon]]
    [ethlance.ui.component.profile-image :refer [c-profile-image]]
    ;; Ethlance Utils
    [ethlance.ui.util.navigation :as util.navigation]
    [re-frame.core :as re]
    [reagent.core :as r]))


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


(defn c-mobile-navigation-menu
  []
  [:div.mobile-navigation-menu
   [c-menu-item {:name :new-job :label "New Job" :route :route.job/new}]
   [c-menu-item {:name :jobs :label "Jobs" :route :route.job/jobs}]
   [c-menu-item {:name :candidates :label "Candidates" :route :route.user/candidates}]
   [c-menu-item {:name :arbiters :label "Arbiters" :route :route.user/arbiters}]
   [c-menu-item {:name :about :label "About" :route :route.misc/how-it-works}]
   [c-menu-item {:name :sign-up :label "Sign Up" :route :route.me/sign-up}]
   [c-menu-item {:name :my-activity :label "My Activity" :route :route.me/index}]])


(defn c-mobile-account-page
  []
  (let [active-account @(re/subscribe [::accounts-subs/active-account])
        query [:user {:user/id active-account}
               [:user/id
                :user/name
                :user/email
                :user/profile-image]]
        result (re/subscribe [::gql/query {:queries [query]} {:refetch-on #{:ethlance.user-profile-updated}}])
        active-account (re/subscribe [::accounts-subs/active-account])
        balance-eth (re/subscribe [::balances-subs/active-account-balance])
        eth-balance (web3-utils/wei->eth-number (or @balance-eth 0))]
    [:div.mobile-account-page
     (when @active-account
       [:div.account-profile
        [c-profile-image {:size :small :src (get-in @result [:user :user/profile-image])}]
        [:span.name (get-in @result [:user :user/name])]])
     [:div.account-balance
      [:span.token-value (format/format-eth eth-balance)]
      [:span.usd-value (-> @(re/subscribe [::conversion-subs/convert :ETH :USD eth-balance])
                              (format/format-currency {:currency "USD"}))]]]))


(defn c-mobile-navigation-bar
  []
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
