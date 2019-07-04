(ns ethlance.ui.component.select-input
  (:require
   [reagent.core :as r]
   
   [ethlance.ui.component.icon :refer [c-icon]]))


(defn c-select-input
  [{:keys [label selections on-select default-selection color] :as opts}]
  (let [*open? (r/atom false)
        *current-selection (r/atom default-selection)
        color (or color :primary)
        color-class (case color
                     :primary " primary "
                     :secondary " secondary ")
        icon-color (case color
                     :primary :black
                     :secondary :white)]
    (fn [{:keys [label selections on-select default-selection color] :as opts}]
      [:div.ethlance-select-input {:class color-class}
       [:div.main
        {:on-click #(swap! *open? not)}
        [:span.label (or @*current-selection label)]
        [c-icon {:class "icon" :name :ic-arrow-up :color icon-color}]]
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
