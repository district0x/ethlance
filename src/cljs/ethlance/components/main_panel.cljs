(ns ethlance.components.main-panel
  (:require
    [cljs-react-material-ui.core :refer [get-mui-theme]]
    [cljs-react-material-ui.reagent :as ui]
    [cljsjs.material-ui-chip-input]
    [clojure.set :as set]
    [district.ui.mobile.subs :as mobile-subs]
    [ethlance.components.currency-select-field :refer [currency-select-field]]
    [ethlance.components.icons :as icons]
    [ethlance.components.misc :as misc :refer [row-plain col a center-layout row paper centered-rows currency]]
    [ethlance.constants :as constants]
    [ethlance.pages.about-page :refer [about-page]]
    [ethlance.pages.contract-detail-page :refer [contract-detail-page]]
    [ethlance.pages.contract-invoices-page :refer [contract-invoices-page]]
    [ethlance.pages.employer-contracts-page :refer [employer-contracts-page]]
    [ethlance.pages.employer-create-page :refer [employer-create-page]]
    [ethlance.pages.employer-detail-page :refer [employer-detail-page]]
    [ethlance.pages.employer-invoices-page :refer [employer-invoices-page]]
    [ethlance.pages.employer-jobs-page :refer [employer-jobs-page]]
    [ethlance.pages.freelancer-contracts-page :refer [freelancer-contracts-page]]
    [ethlance.pages.freelancer-create-page :refer [freelancer-create-page]]
    [ethlance.pages.freelancer-detail-page :refer [freelancer-detail-page]]
    [ethlance.pages.freelancer-invoices-page :refer [freelancer-invoices-page]]
    [ethlance.pages.home-page :refer [home-page]]
    [ethlance.pages.how-it-works-page :refer [how-it-works-page]]
    [ethlance.pages.invoice-create-page :refer [invoice-create-page]]
    [ethlance.pages.invoice-detail-page :refer [invoice-detail-page]]
    [ethlance.pages.job-create-page :refer [job-create-page]]
    [ethlance.pages.job-detail-page :refer [job-detail-page]]
    [ethlance.pages.job-edit-page :refer [job-edit-page]]
    [ethlance.pages.search-freelancers-page :refer [search-freelancers-page]]
    [ethlance.pages.search-jobs-page :refer [search-jobs-page]]
    [ethlance.pages.search-sponsorable-jobs-page :refer [search-sponsorable-jobs-page]]
    [ethlance.pages.sponsor-detail-page :refer [sponsor-detail-page]]
    [ethlance.pages.user-edit-page :refer [user-edit-page]]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(def route->component
  {:about about-page
   :contract/detail contract-detail-page
   :contract/invoices contract-invoices-page
   :employer/create employer-create-page
   :employer/detail employer-detail-page
   :employer/invoices employer-invoices-page
   :employer/contracts employer-contracts-page
   :employer/jobs employer-jobs-page
   :freelancer/contracts freelancer-contracts-page
   :freelancer/create freelancer-create-page
   :freelancer/detail freelancer-detail-page
   :freelancer/invoices freelancer-invoices-page
   :sponsor/detail sponsor-detail-page
   :home home-page
   :how-it-works how-it-works-page
   :invoice/create invoice-create-page
   :invoice/detail invoice-detail-page
   :job/create job-create-page
   :job/detail job-detail-page
   :job/edit job-edit-page
   :search/freelancers search-freelancers-page
   :user/edit user-edit-page
   :search/jobs search-jobs-page
   :search/sponsorable-jobs search-sponsorable-jobs-page})

(def search-nav-items
  [["Find Work" :search/jobs (icons/magnify)]
   ["Find Candidates" :search/freelancers (icons/account-search)]
   ["Find Jobs to Sponsor" :search/sponsorable-jobs (icons/magnify)]])

(def nav-items-freelancers
  [["My Contracts" :freelancer/contracts (icons/file-document)]
   ["My Invoices" :freelancer/invoices (icons/clipboard-text)]])

(def nav-items-employer
  [["My Jobs" :employer/jobs (icons/work)]
   ["My Contracts" :employer/contracts (icons/file-document)]
   ["My Invoices" :employer/invoices (icons/clipboard-text)]])

(def nav-items-registered
  [["My Profile" :user/edit (icons/account)]])

(def nav-items-unregistered
  [["Become Freelancer" :freelancer/create (icons/account-plus)]
   ["Become Employer" :employer/create (icons/account-plus)]])

(defn create-menu-items [items]
  (for [[label handler icon query-string] items]
    [ui/list-item
     {:primary-text label
      :left-icon icon
      :value (u/ns+name handler)
      :href (str (u/path-for handler) query-string)
      :key handler}]))

(defn my-addresses-select-field []
  (let [my-addresses (subscribe [:db/my-addresses])
        active-address (subscribe [:db/active-address])]
    (fn []
      (when (< 1 (count @my-addresses))
        [ui/select-field
         {:value @active-address
          :on-change #(dispatch [:set-active-address %3])
          :style (merge styles/app-bar-user
                        {:width 170})
          :auto-width true
          :label-style styles/app-bar-select-field-label}
         (doall
           (for [address @my-addresses]
             [ui/menu-item
              {:value address
               :primary-text (u/truncate address 25)
               :key address}]))]))))

(defn user-anchor [{:keys [:user]} body]
  (let [{:keys [:user/freelancer? :user/id]} user]
    [a
     {:route (if freelancer? :freelancer/detail :employer/detail)
      :route-params {:user/id id}}
     body]))

(defn app-bar-right-elements []
  (let [active-address-balance (subscribe [:db/active-address-balance])
        active-address-registered? (subscribe [:db/active-address-registered?])
        active-user (subscribe [:db/active-user])
        connection-error? (subscribe [:blockchain/connection-error?])
        contracts-not-found? (subscribe [:db/contracts-not-found?])
        mobile-coinbase-compatible? @(subscribe [::mobile-subs/coinbase-compatible?])
        my-addresses (subscribe [:db/my-addresses])
        my-users-loading? (subscribe [:db/my-users-loading?])
        selected-currency (subscribe [:db/selected-currency])]
    (fn []
      (if-not @connection-error?
        [row-plain
         {:middle "xs"
          :end "xs"}
         (when (and (or @my-users-loading?
                        (and (not @active-user)
                             @active-address-registered?))
                    (not @contracts-not-found?))
           [row-plain
            {:middle "xs"
             :style styles/app-bar-user}
            [ui/circular-progress
             {:size 30
              :color "#FFF"
              :thickness 2}]])
         (when (seq (:user/name @active-user))
           [misc/call-on-change
            {:load-on-mount? true
             :args (:user/id @active-user)
             :on-change #(dispatch [:contracts/listen-active-user-events @active-user])}
            [row-plain
             {:middle "xs"
              :style styles/app-bar-user}
             [user-anchor
              {:user @active-user}
              [:h3.bolder {:style styles/app-bar-user}
               (u/butlast-word (:user/name @active-user))]]
             [user-anchor
              {:user @active-user}
              [ui/avatar
               {:size 35
                :src (u/gravatar-url (:user/gravatar @active-user) (:user/id @active-user))
                :style {:margin-top 5}}]]]])
         (when (and (seq @my-addresses)
                    @active-address-balance)
           [:h2.bolder {:style styles/app-bar-balance}
            [currency @active-address-balance]])
         (cond
           (or (seq @my-addresses) @my-users-loading?) [my-addresses-select-field]
           mobile-coinbase-compatible? [misc/mobile-coinbase-app-bar-link]
           :else
           [misc/how-it-works-app-bar-link
            {:style {:margin-top 0}}
            [row-plain
             {:middle "xs"}
             [:span
              {:style {:margin-right 5}}
              "No accounts connected"]
             (icons/help-circle-outline {:color "#EEE"
                                         :style {:margin-right styles/desktop-gutter-less}})]])
         [currency-select-field
          {:value @selected-currency
           :label-style styles/app-bar-select-field-label
           :style styles/app-bar-user
           :on-change #(dispatch [:selected-currency/set %3])}]]
        [row-plain
         {:middle "xs"
          :end "xs"}
         [misc/how-it-works-app-bar-link "Can't connect to a blockchain. How it works?"]]))))

(defn contracts-not-found-page []
  [centered-rows
   [:h3 "Looks like we couldn't find Ethlance smart contracts. Are you sure you are connected to Ethereum Mainnet?"]
   [ui/raised-button
    {:style styles/margin-top-gutter
     :primary true
     :href (u/path-for :how-it-works)
     :label "How it works?"}]])

(defn setters-not-active-page []
  [centered-rows
   [:h3 "Ethlance smart contracts are currently disabled. It is because we are trying to fix something
   very important. Stay tuned :)"]])

(defn last-transaction-info []
  (let [gas-used-percent (subscribe [:db/last-transaction-gas-used])]
    (fn []
      (when @gas-used-percent
        [:div {:style styles/last-transaction-info}
         (str "Your last transaction used " @gas-used-percent "% of gas")]))))

(def socials
  [["https://www.facebook.com/ethlance/" icons/facebook "#3b5998"]
   ["https://github.com/madvas/ethlance" icons/github "#000"]
   ["https://district0x-slack.herokuapp.com/" icons/slack "#E01765"]
   ["https://twitter.com/ethlance" icons/twitter "#00aced"]])

(defn social-buttons []
  [row-plain
   {:center "xs"}
   [row-plain
    {:center "xs"
     :style (merge styles/full-width
                   {:margin-bottom styles/desktop-gutter-mini})}
    [:div
     {:style styles/district0x-logo-small-text}
     "Participate in Ethlance's governance at"]
    [:a
     {:href "https://district0x.io"
      :target :_blank}
     [:img {:alt "district0x"
            :src "./images/district0x-logo-small.png"
            :style styles/district0x-logo-small}]]]
   (doall
     (for [[href icon color] socials]
       [ui/icon-button
        {:href href
         :target :_blank
         :key href
         :style styles/social-button}
        (icon {:color color})]))])

(defn main-panel []
  (let [current-page (subscribe [:db/current-page])
        drawer-open? (subscribe [:db/drawer-open?])
        active-user (subscribe [:db/active-user])
        active-address (subscribe [:db/active-address])
        snackbar (subscribe [:db/snackbar])
        dialog (subscribe [:db/dialog])
        active-address-registered? (subscribe [:db/active-address-registered?])
        my-users-loading? (subscribe [:db/my-users-loading?])
        contracts-not-found? (subscribe [:db/contracts-not-found?])
        search-freelancers-query (subscribe [:location/form-query-string :form/search-freelancers])
        search-jobs-query (subscribe [:location/form-query-string :form/search-jobs])
        lg-width? (subscribe [:window/lg-width?])
        xs-width? (subscribe [:window/xs-width?])
        active-setters? (subscribe [:db/active-setters?])]
    (fn []
      (let [{:keys [:user/freelancer? :user/employer?]} @active-user
            {:keys [:handler]} @current-page]
        [ui/mui-theme-provider
         {:mui-theme styles/mui-theme}
         (if (= handler :home)
           [(route->component handler)]
           [:div
            [ui/drawer
             {:docked @lg-width?
              :open (or @drawer-open? @lg-width?)
              :on-request-change #(dispatch [:drawer/set %])}
             [:div
              {:style styles/navigation-drawer}
              [:div
               [ui/app-bar
                {:title (r/as-element [misc/logo])
                 :show-menu-icon-button false
                 :style styles/app-bar-left}]
               [ui/selectable-list
                {:value (u/ns+name handler)
                 :style styles/nav-list
                 :on-change (fn [])}
                (create-menu-items (u/conj-colls search-nav-items [@search-jobs-query @search-freelancers-query nil]))
                (when @active-address
                  [ui/list-item
                   {:primary-text "My Sponsorships"
                    :left-icon (icons/coin)
                    :value (u/ns+name :sponsor/detail)
                    :href (u/path-for :sponsor/detail :user/id @active-address)
                    :key :sponsor/detail}])
                (if @active-address-registered?
                  (create-menu-items nav-items-registered)
                  (when (and (not @my-users-loading?)
                             @active-address)
                    (create-menu-items nav-items-unregistered)))
                (when (and freelancer? employer?)
                  [ui/subheader "Job Seeker"])
                (when freelancer?
                  (create-menu-items nav-items-freelancers))
                (when (and freelancer? employer?)
                  [ui/subheader "Employer"])
                (when employer?
                  (create-menu-items nav-items-employer))]]
              [social-buttons]]]
            [ui/app-bar
             {:show-menu-icon-button (not @lg-width?)
              :icon-element-right (r/as-element [app-bar-right-elements])
              ;:icon-element-left (r/as-element [app-bar-left-elements])
              :on-left-icon-button-touch-tap #(dispatch [:drawer/set true])
              :style styles/app-bar-right}]
            [ui/snackbar (-> @snackbar
                           (set/rename-keys {:open? :open}))]
            [ui/dialog (-> @dialog
                         (set/rename-keys {:open? :open}))
             (r/as-element (:body @dialog))]
            (when-let [page (route->component handler)]
              [:div {:style (merge styles/content-wrap
                                   (when @lg-width?
                                     {:padding-left (+ 256 styles/desktop-gutter)})
                                   (when @xs-width?
                                     (styles/padding-all styles/desktop-gutter-mini)))}
               (if @contracts-not-found?
                 [contracts-not-found-page]
                 (if (or @active-setters?
                         (contains? #{:about :how-it-works} handler))
                   [page]
                   [setters-not-active-page]))])])]))))
