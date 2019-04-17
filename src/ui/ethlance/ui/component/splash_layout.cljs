(ns ethlance.ui.component.splash-layout
  (:require
   [re-frame.core :as rf]

   ;; Ethlance Components
   [ethlance.ui.component.ethlance-logo :refer [c-ethlance-logo]]
   [ethlance.ui.component.button :refer [c-button c-button-label]]
   [ethlance.ui.component.inline-svg :refer [c-inline-svg]]
   [ethlance.ui.component.icon :refer [c-icon]]
   [ethlance.ui.component.circle-button :refer [c-circle-icon-button]]
   [ethlance.ui.component.splash-navigation-bar :refer [c-splash-navigation-bar]]))


(defn c-splash-layout
  []
  [:div.splash-layout
   [:div.header
    [c-splash-navigation-bar]]
   [:div.main-content
    ;; BEGIN BANNER
    [:div.banner
     [:div.part-text
      [:h2 "The Future of Work is Now"]
      [:span "Hire or Work for Ether cryptocurrency"]
      [:div.button-listing
       [c-button
        {:color :primary :size :auto}
        [c-button-label [:span "Become a " [:b "Freelancer"]]]]
       [c-button
        {:color :primary :size :auto}
        [c-button-label [:span "Become an " [:b "Arbiter"]]]]
       [c-button
        {:color :primary :size :auto}
        [c-button-label [:span "Become an " [:b "Employer"]]]]]
      [:div.district]]
     [:div.part-figure
      [:figure
       [:img {:src "./images/img-top-banner.png"}]]]]

    ;; BEGIN WELCOME TO
    [:div.welcome
     [:h3 "Welcome to"]
     [:h2 "A Smarter Way to Work"]
     [:div.box-listing
      [:div.box-ball
       [:div.vertical-box
        [:figure
         [:img {:src "./images/icon-service.png"}]]]]

      [:div.box-ball
       [:div.vertical-box
        [:figure
         [:img {:src "./images/icon-decentralised.png"}]]]]

      [:div.box-ball
       [:div.vertical-box
        [:figure
         [:img {:src "./images/icon-membership.png"}]]]]]]
         
    [:div.created]
    [:div.checkout]]
   [:div.footer]])
   

     
