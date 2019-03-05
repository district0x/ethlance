(ns ethlance.ui.component.button
  "An ethlance button component")


(defn c-button []
  (fn [{:keys [disabled? active? color size on-click] :as props
        :or {color :primary disabled? false active? false size :normal}}
       & children]
    [:div.ethlance-button
     {:class [(when (= color :secondary) " secondary ")
              (when disabled? " disabled ")
              (when active? " active ")
              (condp = size
                :small " small "
                :normal " "
                :large " large ")]
      :on-click on-click}
     children]))


(defn c-button-label []
  (fn [_ & children]
    [:div.button-label {} children]))
