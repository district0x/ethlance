(ns ethlance.ui.component.tag
  "An ethlance tag component")


(defn c-tag []
  (fn [opts & children]
    [:div.ethlance-tag opts children]))


(defn c-tag-label []
  (fn [label]
    [:span.ethlance-tag-label label]))
