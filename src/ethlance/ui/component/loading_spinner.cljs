(ns ethlance.ui.component.loading-spinner
  (:require
   [reagent.core :as r]))


(defn c-loading-spinner []
  [:div.loading-spinner
   [:img {:src "/images/svg/ethlance_spinner.svg"}]])
