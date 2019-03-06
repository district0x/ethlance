(ns ethlance.ui.component.icon)


(defn c-icon []
  (fn [{:keys [color size] :as props}]
    (let [props (dissoc props :color :size)]
      [:div.ethlance-icon])))
