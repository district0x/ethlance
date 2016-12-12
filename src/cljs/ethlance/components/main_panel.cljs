(ns ethlance.components.main-panel
  (:require
    [cljs-react-material-ui.core :refer [get-mui-theme]]
    [cljs-react-material-ui.icons :as icons]
    [cljs-react-material-ui.reagent :as ui]
    [ethlance.pages.contract-detail-page :refer [contract-detail-page]]
    [ethlance.pages.contract-invoices-page :refer [contract-invoices-page]]
    [ethlance.pages.employer-create-page :refer [employer-create-page]]
    [ethlance.pages.employer-invoices-page :refer [employer-invoices-page]]
    [ethlance.pages.employer-jobs-page :refer [employer-jobs-page]]
    [ethlance.pages.employer-profile :refer [employer-profile]]
    [ethlance.pages.freelancer-contracts-page :refer [freelancer-contracts-page]]
    [ethlance.pages.freelancer-create-page :refer [freelancer-create-page]]
    [ethlance.pages.freelancer-invoices-page :refer [freelancer-invoices-page]]
    [ethlance.pages.freelancer-profile :refer [freelancer-profile]]
    [ethlance.pages.home-page :refer [home-page]]
    [ethlance.pages.invoice-create-page :refer [invoice-create-page]]
    [ethlance.pages.invoice-detail-page :refer [invoice-detail-page]]
    [ethlance.pages.job-create-page :refer [job-create-page]]
    [ethlance.pages.job-detail-page :refer [job-detail-page]]
    [ethlance.pages.my-profile-page :refer [my-profile-page]]
    [ethlance.pages.search-freelancers-page :refer [search-freelancers-page]]
    [ethlance.pages.search-jobs-page :refer [search-jobs-page]]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]
    [clojure.set :as set]))

(def route->component
  {:employer/create employer-create-page
   :contract/detail contract-detail-page
   :contract/invoices contract-invoices-page
   :employer/detail employer-profile
   :employer/invoices employer-invoices-page
   :employer/jobs employer-jobs-page
   :freelancer/create freelancer-create-page
   :freelancer/detail freelancer-profile
   :freelancer/invoices freelancer-invoices-page
   :freelancer/contracts freelancer-contracts-page
   :home home-page
   :invoice/create invoice-create-page
   :invoice/detail invoice-detail-page
   :job/create job-create-page
   :job/detail job-detail-page
   :my-profile my-profile-page
   :search/freelancers search-freelancers-page
   :search/jobs search-jobs-page})

(def nav-items
  [["Find Work" :search/jobs (icons/action-work)]
   ["Find People" :search/freelancers (icons/social-people)]
   ["My Profile" :my-profile (icons/social-person)]])

(def nav-items-freelancers
  [["My Contracts" :freelancer/contracts (icons/hardware-laptop-mac)]
   ["My Invoices" :freelancer/invoices (icons/action-assignment)]])

(def nav-items-employer
  [["My Jobs" :employer/jobs (icons/hardware-laptop-mac)]
   ["Invoices" :employer/invoices (icons/action-assignment)]])

(defn create-menu-items [items]
  (for [[label handler icon] items]
    [ui/list-item
     {:primary-text label
      :left-icon icon
      :value (u/ns+name handler)
      :href (u/path-for handler)
      :key handler}]))

(defn my-addresses-select-field []
  (let [my-addresses (subscribe [:db/my-addresses])
        active-address (subscribe [:db/active-address])]
    (fn []
      [ui/select-field
       {:value @active-address
        :on-change #(dispatch [:set-active-address %3])
        :label-style styles/address-select-field-label}
       (for [address @my-addresses]
         [ui/menu-item
          {:value address
           :primary-text (u/truncate address 25)
           :key address}])])))

(defn main-panel []
  (let [current-page (subscribe [:db/current-page])
        drawer-open? (subscribe [:db/drawer-open?])
        user (subscribe [:db/active-user])
        snackbar (subscribe [:db/snackbar])]
    (fn []
      (let [{:keys [:user/freelancer? :user/employer?]} @user]
        [ui/mui-theme-provider
         {:mui-theme styles/mui-theme}
         [:div
          [ui/drawer
           {:docked true
            :open @drawer-open?}
           [ui/app-bar
            {:title "ethlance"
             :show-menu-icon-button false
             :style styles/app-bar-left}]
           [ui/selectable-list
            {:value (u/ns+name (:handler @current-page))
             :style styles/nav-list
             :on-change (fn [])}
            (create-menu-items nav-items)
            (when (and freelancer? employer?)
              [ui/subheader "Freelancer"])
            (when freelancer?
              (create-menu-items nav-items-freelancers))
            (when (and freelancer? employer?)
              [ui/subheader "Employer"])
            (when employer?
              (create-menu-items nav-items-employer))]]
          [ui/app-bar
           {:show-menu-icon-button false
            :icon-element-right (r/as-element [my-addresses-select-field])
            :style styles/app-bar-right}]
          [ui/snackbar (-> @snackbar
                         (set/rename-keys {:open? :open})
                         (update :message #(r/as-element %))
                         (update :action #(if % (r/as-element %) nil)))]
          (when-let [page (route->component (:handler @current-page))]
            [:div {:style styles/content-wrap}
             [page]])]]))))