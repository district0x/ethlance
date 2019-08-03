(ns ethlance.ui.component.checkbox
  (:require
   [reagent.core :as r]
   [ethlance.ui.component.inline-svg :refer [c-inline-svg]]))


(defn c-labeled-checkbox
  [{:keys [label on-change default-checked?] :as opts}]
  (let [opts (dissoc opts :label :on-change :default-checked?)
        *checked? (r/atom default-checked?)]
    (fn [opts]
      [:div.ethlance-checkbox
       (merge
        opts
        {:on-click
         (fn []
           (when on-change
             (on-change @*checked?))
           (swap! *checked? not))

         :class (when @*checked? "checked")})
       [c-inline-svg {:src "images/svg/checkbox.svg"
                      :width 24
                      :height 24}]
       [:span.label label]])))
       
