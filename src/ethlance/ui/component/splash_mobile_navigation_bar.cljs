(ns ethlance.ui.component.splash-mobile-navigation-bar
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]

   ;; Ethlance Components
   [ethlance.ui.component.ethlance-logo :refer [c-ethlance-logo]]
   [ethlance.ui.component.icon :refer [c-icon]]))


(defn c-nav-link [{:keys [name href]}]
  [:a.nav-link {:href href} name])


(defn c-splash-mobile-navigation-bar []
  (let [*open? (r/atom false)]
    (fn []
      [:div.splash-mobile-navigation-bar
       [:div.content
        [c-ethlance-logo {:color :white}]
        [c-icon {:name (if @*open? :close :list-menu)
                 :color :white
                 :on-click #(swap! *open? not)}]]
       (when @*open?
         [:div.drawer
          [c-nav-link {:name "Find Work" :href "#"}]
          [c-nav-link {:name "Find Candidates" :href "#"}]
          [c-nav-link {:name "How It Works" :href "#"}]])])))
