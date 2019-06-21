(ns ethlance.ui.component.button
  "An ethlance button component"
  (:require
   [ethlance.ui.component.icon :refer [c-icon]]))


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


(defn c-button-icon-label []
  (fn [{:keys [icon-name label-text] :as opts}]
    [:div.button-icon-label
     [:div.icon
      [c-icon {:name icon-name}]]
     [:span.label label-text]]))
