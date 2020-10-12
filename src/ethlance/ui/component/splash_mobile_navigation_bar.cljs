(ns ethlance.ui.component.splash-mobile-navigation-bar
  (:require [ethlance.ui.component.ethlance-logo :refer [c-ethlance-logo]]
            [ethlance.ui.component.icon :refer [c-icon]]
            [ethlance.ui.util.navigation :as util.navigation]
            [reagent.core :as r]))

(defn c-nav-link [{:keys [name route]}]
  [:a.nav-link
   {:title name
    :on-click (util.navigation/create-handler {:route route})
    :href (util.navigation/resolve-route {:route route})}
   name])

(defn c-splash-mobile-navigation-bar []
  (let [*open? (r/atom false)]
    (fn []
      [:div.splash-mobile-navigation-bar
       [:div.content
        [c-ethlance-logo {:color :white :inline? false}]
        [c-icon {:name (if @*open? :close :list-menu)
                 :color :white
                 :on-click #(swap! *open? not)}]]
       (when @*open?
         [:div.drawer
          [c-nav-link {:name "Find Work" :route :route.job/jobs}]
          [c-nav-link {:name "Find Candidates" :route :route.user/candidates}]
          [c-nav-link {:name "How It Works" :route :route.misc/how-it-works}]])])))
