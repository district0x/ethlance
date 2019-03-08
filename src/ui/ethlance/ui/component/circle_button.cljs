(ns ethlance.ui.component.circle-button
  "Circle button, which usually displays an icon"
  (:require
   [ethlance.ui.component.icon :refer [c-icon]]))


(defn c-circle-icon-button
  []
  (fn [{:keys [name color size] 
        :or {name :about color :primary size :normal}
        :as props}]
    (let [class-color (case color
                        :primary " primary "
                        :secondary " secondary "
                        :none "")]
      [:div.ethlance-circle-button.ethlance-circle-icon-button
       {:class [class-color]}
       [c-icon {:name name :color color}]])))

