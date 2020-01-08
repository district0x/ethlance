(ns ethlance.ui.component.modal
  "Component for displaying a modal container"
  (:require
   [re-frame.core :as re]
   [ethlance.ui.component.icon :refer [c-icon]]
   [ethlance.ui.subscriptions :as ui.subs]))


(defn c-modal
  ""
  [{:keys [] :as opts} & children]
  (into [:div.ethlance-modal opts] children))
