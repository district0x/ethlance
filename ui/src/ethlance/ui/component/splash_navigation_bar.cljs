(ns ethlance.ui.component.splash-navigation-bar
  (:require [ethlance.ui.component.ethlance-logo :refer [c-ethlance-logo]]
            [ethlance.ui.util.navigation :as util.navigation]
            [reagent.core :as r]))

(defn c-splash-navigation-link
  [{:keys [name route *hover]}]
  [:div.splash-navigation-link
   {:on-mouse-enter #(reset! *hover name)
    :on-mouse-leave #(reset! *hover nil)}
   [:a {:href (util.navigation/resolve-route {:route route})
        :title name
        :on-click (util.navigation/create-handler {:route route})
        :class [(when (or (not @*hover) (= @*hover name)) "show-underline")]} name]])

(defn c-splash-navigation-bar []
  (let [*hover (r/atom nil)]
    (fn []
      [:div.splash-navigation-bar
       [:div.logo
        [c-ethlance-logo]]
       [:div.links
        [c-splash-navigation-link
         {:*hover *hover
          :name "Find Work"
          :route :route.job/jobs}]
        [c-splash-navigation-link
         {:*hover *hover
          :name "Find Candidates"
          :route :route.user/candidates}]
        [c-splash-navigation-link
         {:*hover *hover
          :name "How it Works"
          :route :route.misc/how-it-works}]]])))
