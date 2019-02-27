(ns ethlance.ui.component.button
  "An ethlance button component")


(defn c-button []
  (fn [{:keys [disabled? on-click]} & children]
    [:div.ethlance-button
     {}
     children]))
