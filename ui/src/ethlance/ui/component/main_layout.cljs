(ns ethlance.ui.component.main-layout
  (:require [ethlance.ui.component.circle-button :refer [c-circle-icon-button]]
            [district.ui.component.notification :as component.notification]
            [ethlance.ui.component.main-navigation-bar
             :refer
             [c-main-navigation-bar]]
            [ethlance.ui.component.main-navigation-menu
             :refer
             [c-main-navigation-menu]]
            [ethlance.ui.component.mobile-navigation-bar
             :refer
             [c-mobile-navigation-bar]]
            [district.ui.router.subs :as router.subs]
            [re-frame.core :as re]
            [akiroz.re-frame.storage]
            [ethlance.ui.component.sign-in-dialog :refer [c-sign-in-dialog] :as sidi]))

(defn page-title-from-route-name
  "Example: :route.job/detail => Ethlance: Job Detail"
  [route-name]
  (let [app-name "Ethlance"
        name-parts-from-route-ns (rest (clojure.string/split (namespace route-name) "."))
        name-part-from-route-name(name route-name)
        name-parts (flatten [app-name ":" name-parts-from-route-ns name-part-from-route-name])]
    (clojure.string/join " " (map clojure.string/capitalize name-parts))))

(defn has-active-session? []
  (not (nil? (akiroz.re-frame.storage/<-store :ethlance))))

(defn c-main-layout
  "The main layout of each page in the ethlance ui.

  # Keyword Arguments

  opts - React Props and additional Optional Arguments

  # Optional Arguments (opts)

  :container-opts - React Props to be applied to the main container
  that contains the child component.
  "
  []
  (let [active-page (re/subscribe [::router.subs/active-page-name])
        has-active-session? (re/subscribe [:ethlance.ui.subscriptions/active-account-has-session?])]
    (fn [{:keys [container-opts] :as opts} & children]
      (if @has-active-session?
        (re/dispatch [:modal/close :ethlance.ui.component.sign-in-dialog/sign-in])
        (re/dispatch [:modal/open :ethlance.ui.component.sign-in-dialog/sign-in]))
      (set! (.-title js/document) (page-title-from-route-name @active-page))
      (let [opts (dissoc opts :container-opts)]
        [:div.main-layout
         opts
         [c-main-navigation-bar]
         [c-mobile-navigation-bar]
         [:div.main-margin
          [c-main-navigation-menu]
          (into [:div.main-container container-opts] children)]
         [:div.footer
          [:div.copyright
           [:span "Copyright © 2019 Ethlance.com"]
           [:div.spacer "|"]
           [:span "All rights reserved."]]
          [:div.social
           [c-circle-icon-button {:name :facebook :title "District0x Facebook"
                                  :size :small :href "https://www.facebook.com/district0x/"}]
           [c-circle-icon-button {:name :twitter :title "District0x Twitter"
                                  :size :small :href "https://twitter.com/district0x?lang=en"}]
           [c-circle-icon-button {:name :github :title "District0x Github"
                                  :size :small :href "https://github.com/district0x"}]
           [c-circle-icon-button {:name :slack :title "District0x Slack"
                                  :size :small :href "https://district0x-slack.herokuapp.com/"}]]]

         [:div.modals
          [c-sign-in-dialog]]
         [component.notification/notification]]))))
