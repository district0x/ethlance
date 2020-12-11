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
    [print.foo :include-macros true]
    [re-frame.core :refer [subscribe]]))

(defn c-main-navigation-bar
  "Main Navigation bar seen while the site is in desktop-mode."
  []
  (let [active-account (subscribe [::accounts-subs/active-account])
        balance-eth (subscribe [::balances-subs/active-account-balance])
        active-user (subscribe [::ethlance-subs/active-user])
        active-account-has-session? (subscribe [::ethlance-subs/active-account-has-session?])]
    (fn []
      (print.foo/look @active-account-has-session?)
      [:div.main-navigation-bar
       [c-ethlance-logo
        {:color :white
         :size :small
         :title "Go to Home Page"
         :on-click (util.navigation/create-handler {:route :route/home})
         :href (util.navigation/resolve-route {:route :route/home})
         :inline? false}]
       [:div.profile
        (when @active-user
          [c-profile-image {:size :small}])
        [:div.name
         (cond
           @active-user (:user/name @active-user)
           @active-account (format/truncate @active-account 12)
           :else "Wallet not connected")]]
       (let [eth-balance (web3-utils/wei->eth-number (or @balance-eth 0))]
         [:div.account-balances
          [:div.token-value (format/format-eth eth-balance)]
          [:div.usd-value (-> @(subscribe [::conversion-subs/convert :ETH :USD eth-balance])
                            (format/format-currency {:currency "USD"}))]])])))
