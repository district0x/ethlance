(ns ethlance.ui.component.button
  "An ethlance button component")


(defn c-button []
  (fn [{:keys [disabled?] :as props} & children]
    [:div.ethlance-button
     (merge props {})
     children]))
