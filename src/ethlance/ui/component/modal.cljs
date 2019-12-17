(ns ethlance.ui.component.modal
  "Component for displaying a modal container"
  (:require
   [ethlance.ui.component.icon :refer [c-icon]]))


(defn c-modal
  ""
  [{:keys [] :as opts} & children]
  [:div.ethlance-modal
   children])
