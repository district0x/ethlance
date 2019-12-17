(ns ethlance.ui.component.text-input
  (:require
   [reagent.core :as r]))


(defn c-text-input
  "Default Text Input Component

  # Keyword Arguments
  
  opts - React Props
  "
  [{:keys [color] :as opts}]
  (let [class-color (case color
                      :primary "primary"
                      :secondary "secondary"
                      " primary ")]
    (fn []
      [:input.ethlance-text-input
       (merge opts {:class [class-color]})])))
