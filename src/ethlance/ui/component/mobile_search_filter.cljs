(ns ethlance.ui.component.mobile-search-filter
  (:require
   [reagent.core :as r]
   
   ;; Ethlance Components
   [ethlance.ui.component.icon :refer [c-icon]]))


(defn c-mobile-search-filter
  [& children]
  (let [*open? (r/atom false)]
    (fn []  
      [:div.mobile-search-filter
       {:class (when @*open? "open")}
       [:div.filter-button
        {:on-click #(swap! *open? not)}
        [:span "Filter"]
        [c-icon {:name (if @*open? :ic-arrow-down  :ic-arrow-up)
                 :size :small}]]
       (into [:div.content] children)])))
