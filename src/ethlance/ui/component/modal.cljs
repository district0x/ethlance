(ns ethlance.ui.component.modal)

(defn c-modal
  [{:keys [] :as opts} & children]
  (into [:div.ethlance-modal opts] children))
