(ns ethlance.ui.component.button
  "An ethlance button component")


(defn c-button []
  (fn [{:keys [disabled? active? color size] :as props
        :or {color :primary disabled? false active? false size :normal}}
       & children]
    (let [props (dissoc props :disabled? :active? :color :size)]
      [:div.button
       (merge
        {:class [(when (= color :secondary) " secondary ")
                 (when disabled? " disabled ")
                 (when active? " active ")
                 (condp = size
                   :small " small "
                   :normal " "
                   :large " large "
                   :auto " auto ")]}
        props)
       children])))


(defn c-button-label []
  (fn [props & children]
    [:div.button-label props children]))
