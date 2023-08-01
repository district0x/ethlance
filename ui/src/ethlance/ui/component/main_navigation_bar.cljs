(ns ethlance.ui.component.main-navigation-bar
  (:require
    [district.format :as format]
    [district.ui.conversion-rates.subs :as conversion-subs]
    [district.ui.web3-account-balances.subs :as balances-subs]
    [district.ui.web3-accounts.subs :as accounts-subs]
    [district.web3-utils :as web3-utils]
    [ethlance.ui.component.ethlance-logo :refer [c-ethlance-logo]]
    [ethlance.ui.component.profile-image :refer [c-profile-image]]
    [ethlance.ui.event.sign-in]
    [ethlance.ui.subscriptions :as ethlance-subs]
    [ethlance.ui.util.navigation :as util.navigation]
    [district.ui.graphql.subs :as gql]
    [print.foo :include-macros true]
    [re-frame.core :as re]))

(defn c-main-navigation-bar
  "Main Navigation bar seen while the site is in desktop-mode."
  []
  (let [active-account (re/subscribe [::accounts-subs/active-account])
        active-session (re/subscribe [::ethlance-subs/active-session])
        balance-eth (re/subscribe [::balances-subs/active-account-balance])]
    (fn []
      (let [active-user-id (or (:user/id @active-session) @active-account)
            query [:user {:user/id active-user-id}
                   [:user/id
                    :user/name
                    :user/profile-image]]
            result (re/subscribe [::gql/query {:queries [query]}])
            profile-image (get-in @result [:user :user/profile-image])
            eth-balance (web3-utils/wei->eth-number (or @balance-eth 0))]
        [:div.main-navigation-bar
         [c-ethlance-logo
          {:color :white
           :size :small
           :title "Go to Home Page"
           :on-click (util.navigation/create-handler {:route :route/home})
           :href (util.navigation/resolve-route {:route :route/home})
           :inline? false}]
         [:div.profile
          (when (not (:graphql/loading? @result))
            [c-profile-image {:size :small :src (get-in @result [:user :user/profile-image])}])
          [:div.name
           (cond
             (not (nil? (:user @result))) (get-in @result [:user :user/name])
             active-user-id (format/truncate @active-account 12)
             :else "Wallet not connected")]]
          [:div.account-balances
            [:div.token-value (format/format-eth eth-balance)]
            [:div.usd-value (-> @(re/subscribe [::conversion-subs/convert :ETH :USD eth-balance])
                              (format/format-currency {:currency "USD"}))]]]))))
