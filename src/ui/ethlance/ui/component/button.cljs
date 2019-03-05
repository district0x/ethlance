(ns ethlance.ui.component.button
  "An ethlance button component")


(defn c-button []
  (fn [{:keys [disabled? color] :as props
        :or {color :primary disabled? false}}
       & children]
    [:div.ethlance-button
     {:class [(when (= color :secondary) " secondary ")
              (when disabled? " disabled ")]}
     children]))


(defn c-button-label []
  (fn [_ & children]
    [:div.button-label {} children]))
