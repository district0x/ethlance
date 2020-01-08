(ns ethlance.ui.component.mobile-sidebar
  ""
  (:require
   [reagent.core :as r]
   
   ;; Ethlance Components
   [ethlance.ui.component.icon :refer [c-icon]]))


(defn c-mobile-sidebar
  [& children]
  (let [*open? (r/atom false)]
    (fn []  
      [:div.mobile-sidebar
       {:class (when @*open? "open")}
       [:div.nav-button
        {:on-click #(swap! *open? not)}
        [:span "Navigate"]
        [c-icon {:name (if @*open? :ic-arrow-down  :ic-arrow-up)
                 :size :small}]]
       (into [:div.content] children)])))
