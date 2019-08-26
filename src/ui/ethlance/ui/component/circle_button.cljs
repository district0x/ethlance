(ns ethlance.ui.component.circle-button
  "Circle button, which usually displays an icon"
  (:require
   [ethlance.ui.component.icon :refer [c-icon]]))


(defn c-circle-icon-button
  []
  (fn [{:keys [name color size disabled? hide? on-click]
        :or {name :about color :primary size :normal}
        :as opts}]
    (let [opts (dissoc opts :name :color :size :disabled? :hide? :on-click)
          class-color (case color
                        :primary " primary "
                        :secondary " secondary "
                        :none "")
          class-size  (case size
                        :small " small "
                        :normal ""
                        :large " large ")
          class-disabled (when disabled? " disabled ")
          class-hide (when hide? " hide ")]
      [:div.ethlance-circle-button.ethlance-circle-icon-button
       (merge
        opts
        {:class [class-color class-size class-disabled class-hide]
         :on-click
         (fn [e]
           (when (and on-click (not disabled?))
             (on-click e)))})
       [c-icon {:name name :color color :size size}]])))
