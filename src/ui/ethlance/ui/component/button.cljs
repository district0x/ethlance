(ns ethlance.ui.component.button
  "An ethlance button component")


(defn c-button []
  (fn [{:keys [disabled? active? color] :as props
        :or {color :primary disabled? false active? false}}
       & children]
    [:div.ethlance-button
     {:class [(when (= color :secondary) " secondary ")
              (when disabled? " disabled ")
              (when active? " active ")]}
     children]))


(defn c-button-label []
  (fn [_ & children]
    [:div.button-label {} children]))
