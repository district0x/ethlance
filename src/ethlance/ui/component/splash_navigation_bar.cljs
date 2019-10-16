(ns ethlance.ui.component.splash-navigation-bar
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]

   ;; Ethlance Components
   [ethlance.ui.component.ethlance-logo :refer [c-ethlance-logo]]))


(defn c-splash-navigation-link
  [{:keys [name link *hover]}]
  [:div.splash-navigation-link
   {:on-mouse-over #(swap! *hover name)}
   [:a {:href link
        :title name
        :class (when (= @*hover name) "hover")} name]])


(defn c-splash-navigation-bar []
  (let [*hover (r/atom nil)]
    (fn []
      [:div.splash-navigation-bar
       [:div.logo
        [c-ethlance-logo]]
       [:div.links
        [c-splash-navigation-link {:*hover *hover :name "Find Work" :link "/employees"}]
        [c-splash-navigation-link {:*hover *hover :name "Find Candidates" :link "/candidates"}]
        [c-splash-navigation-link {:*hover *hover :name "How it Works" :link "/how-it-works"}]]])))
