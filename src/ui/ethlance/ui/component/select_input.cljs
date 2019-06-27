(ns ethlance.ui.component.select-input
  (:require
   [reagent.core :as r]
   
   [ethlance.ui.component.inline-svg :refer [c-inline-svg]]))


(defn c-select-input
  [{:keys [label selections on-select default-selection] :as opts}]
  (let [*open? (r/atom false)
        *current-selection (r/atom default-selection)]
    (fn [{:keys [label selections on-select default-selection] :as opts}]
      [:div.ethlance-select-input
       [:div.main
        {:on-click #(swap! *open? not)}
        [:span.label (or @*current-selection label)]
        [:span.icon "x"]]
       (when @*open?
         [:div.dropdown
          (doall
           (for [selection (set selections)]
             ^{:key (str "selection-" selection)}
             [:div.selection
              {:on-click
               (fn []
                 (reset! *current-selection selection)
                 (reset! *open? false)
                 (when on-select (on-select selection)))}
              selection]))])])))
