(ns ethlance.ui.component.tabular-layout
  (:require
   [reagent.core :as r]))


(defn c-tabular-layout
  [{:keys [default-tab]} & opts-children]
  (let [*active-tab-index (r/atom (or default-tab 0))]
    (fn []
      (let [tab-parts (partition 2 opts-children)
            tab-options (map-indexed #(-> %2 first (assoc :index %1)) tab-parts)
            tab-children (mapv second tab-parts)]
        [:div.tabular-layout
         [:div.tab-listing
          (doall
           (for [{:keys [index label]} tab-options]
             ^{:key (str "tab-" index)}
             [:div.tab
              {:class (when (= @*active-tab-index index) "active")
               :on-click #(reset! *active-tab-index index)}
              [:span.label label]]))]
         [:div.active-page
          (get tab-children @*active-tab-index)]]))))
