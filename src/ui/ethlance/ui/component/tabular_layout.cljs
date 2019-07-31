(ns ethlance.ui.component.tabular-layout
  (:require
   [reagent.core :as r]

   ;; Ethlance Components
   [ethlance.ui.component.select-input :refer [c-select-input]]))


(defn c-tabular-layout
  [{:keys [default-tab] :as opts} & opts-children]
  (let [opts (dissoc opts :default-tab)
        *active-tab-index (r/atom (or default-tab 0))]
    (fn []
      (let [tab-parts (partition 2 opts-children)
            tab-options (map-indexed #(-> %2 first (assoc :index %1)) tab-parts)
            tab-children (mapv second tab-parts)]
        [:div.tabular-layout opts
         [:div.tab-listing
          (doall
           (for [{:keys [index label]} tab-options]
             ^{:key (str "tab-" index)}
             [:div.tab
              {:class (when (= @*active-tab-index index) "active")
               :on-click #(reset! *active-tab-index index)}
              [:span.label label]]))]

         [:div.mobile-tab-listing
          [c-select-input
           {:default-selection (-> tab-options (get @*active-tab-index) :label)
            :selections (mapv :label tab-options)
            :color :secondary
            :on-select
            (fn [selection]
             (let [selections (map :label tab-options)
                   new-index (.indexOf selections selection)]
               (reset! *active-tab-index new-index)))}]]
            
         [:div.active-page {:key "active-page"}
          (get tab-children @*active-tab-index)]]))))
