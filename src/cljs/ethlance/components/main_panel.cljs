(ns ethlance.components.main-panel
  (:require
    [cljs-react-material-ui.core :refer [get-mui-theme]]
    [cljs-react-material-ui.reagent :as ui]
    [cljsjs.material-ui-chip-input]
    [clojure.set :as set]
    [ethlance.components.icons :as icons]
    [ethlance.components.misc :as misc :refer [row-plain col a center-layout row paper centered-rows currency]]
    [ethlance.constants :as constants]
    [ethlance.pages.about-page :refer [about-page]]
    [ethlance.pages.contract-detail-page :refer [contract-detail-page]]
    [ethlance.pages.contract-invoices-page :refer [contract-invoices-page]]
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
    [ethlance.pages.search-freelancers-page :refer [search-freelancers-page]]
    [ethlance.pages.search-jobs-page :refer [search-jobs-page]]
    [ethlance.pages.skills-create-page :refer [skills-create-page]]
    [ethlance.pages.user-edit-page :refer [user-edit-page]]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    ))

(def route->component
  {:about about-page
   :contract/detail contract-detail-page
   :contract/invoices contract-invoices-page
   :employer/create employer-create-page
   :employer/detail employer-detail-page
   :employer/invoices employer-invoices-page
   :employer/jobs employer-jobs-page
   :freelancer/contracts freelancer-contracts-page
   :freelancer/create freelancer-create-page
   :freelancer/detail freelancer-detail-page
   :freelancer/invoices freelancer-invoices-page
   :home home-page
   :how-it-works how-it-works-page
   :invoice/create invoice-create-page
   :invoice/detail invoice-detail-page
   :job/create job-create-page
   :job/detail job-detail-page
   :search/freelancers search-freelancers-page
   :skills/create skills-create-page
   :user/edit user-edit-page
   :search/jobs search-jobs-page})

(def search-nav-items
  [["Find Work" :search/jobs (icons/magnify)]
   ["Find Freelancers" :search/freelancers (icons/account-search)]])

(def nav-items-freelancers
  [["My Contracts" :freelancer/contracts (icons/file-document)]
   ["My Invoices" :freelancer/invoices (icons/clipboard-text)]])

(def nav-items-employer
  [["My Jobs" :employer/jobs (icons/work)]
   ["Invoices" :employer/invoices (icons/clipboard-text)]])

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
      [ui/select-field
       {:value @active-address
        :on-change #(dispatch [:set-active-address %3])
        :style styles/app-bar-user
        :label-style styles/app-bar-select-field-label}
       (for [address @my-addresses]
         [ui/menu-item
          {:value address
           :primary-text (u/truncate address 25)
           :key address}])])))

(defn user-anchor [{:keys [:user]} body]
  (let [{:keys [:user/freelancer? :user/id]} user]
    [a
     {:route (if freelancer? :freelancer/detail :employer/detail)
      :route-params {:user/id id}}
     body]))

(defn currency-select-field []
  (let [selected-currency (subscribe [:db/selected-currency])]
    (fn []
      [ui/select-field
       {:value @selected-currency
        :label-style styles/app-bar-select-field-label
        :auto-width true
        :on-change #(dispatch [:selected-currency/set %3])
        :style (merge
                 styles/app-bar-user
                 {:width 40})}
       (for [[key text] constants/currencies]
         [ui/menu-item
          {:value key
           :primary-text text
           :key key}])])))

(defn app-bar-right-elements []
  (let [active-address-balance (subscribe [:db/active-address-balance])
        active-user (subscribe [:db/active-user])
        my-users-loading? (subscribe [:db/my-users-loading?])
        active-address-registered? (subscribe [:db/active-address-registered?])
        connection-error? (subscribe [:blockchain/connection-error?])
        my-addresses (subscribe [:db/my-addresses])
        contracts-not-found? (subscribe [:db/contracts-not-found?])]
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
         (when @active-user
           [misc/call-on-change
            {:load-on-mount? true
             :args (:user/id @active-user)
             :on-change #(dispatch [:contracts/listen-active-user-events (:user/id @active-user)])}
            [row-plain
             {:middle "xs"
              :style styles/app-bar-user}
             [user-anchor
              {:user @active-user}
              [:h3.bolder {:style styles/app-bar-user}
               (u/first-word (:user/name @active-user))]]
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
         (if (or (seq @my-addresses) @my-users-loading?)
           [my-addresses-select-field]
           [misc/how-it-works-app-bar-link
            {:style {:margin-top 0}}
            "No accounts connected. How to join?"])
         [currency-select-field]]
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

(defn main-panel []
  (let [current-page (subscribe [:db/current-page])
        drawer-open? (subscribe [:db/drawer-open?])
        active-user (subscribe [:db/active-user])
        active-address (subscribe [:db/active-address])
        snackbar (subscribe [:db/snackbar])
        active-address-registered? (subscribe [:db/active-address-registered?])
        my-users-loading? (subscribe [:db/my-users-loading?])
        contracts-not-found? (subscribe [:db/contracts-not-found?])
        search-freelancers-query (subscribe [:location/form-query-string :form/search-freelancers])
        search-jobs-query (subscribe [:location/form-query-string :form/search-jobs])
        lg-width? (subscribe [:window/lg-width?])
        xs-width? (subscribe [:window/xs-width?])]
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
             [ui/app-bar
              {:title (r/as-element [misc/logo])
               :show-menu-icon-button false
               :style styles/app-bar-left}]
             [ui/selectable-list
              {:value (u/ns+name handler)
               :style styles/nav-list
               :on-change (fn [])}
              (create-menu-items (u/conj-colls search-nav-items [@search-jobs-query @search-freelancers-query]))
              (if @active-address-registered?
                (create-menu-items nav-items-registered)
                (when (and (not @my-users-loading?)
                           @active-address)
                  (create-menu-items nav-items-unregistered)))
              (when (and freelancer? employer?)
                [ui/subheader "Freelancer"])
              (when freelancer?
                (create-menu-items nav-items-freelancers))
              (when (and freelancer? employer?)
                [ui/subheader "Employer"])
              (when employer?
                (create-menu-items nav-items-employer))]]
            [ui/app-bar
             {:show-menu-icon-button (not @lg-width?)
              :icon-element-right (r/as-element [app-bar-right-elements])
              ;:icon-element-left (r/as-element [app-bar-left-elements])
              :on-left-icon-button-touch-tap #(dispatch [:drawer/set true])
              :style styles/app-bar-right}]
            [ui/snackbar (-> @snackbar
                           (set/rename-keys {:open? :open}))]
            (when-let [page (route->component handler)]
              [:div {:style (merge styles/content-wrap
                                   (when @lg-width?
                                     {:padding-left (+ 256 styles/desktop-gutter)})
                                   (when @xs-width?
                                     (styles/padding-all styles/desktop-gutter-mini)))}
               (if @contracts-not-found?
                 [contracts-not-found-page]
                 [page])])])]))))