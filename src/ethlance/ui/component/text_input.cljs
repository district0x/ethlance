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
                      :primary ""
                      :secondary " secondary "
                      "")]
    (fn []
      [:input.ethlance-text-input
       (merge opts {:class class-color})])))
