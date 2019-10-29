(ns ethlance.ui.component.scrollable
  (:require
   [reagent.core :as r]
   [simplebar-react]))


(defn c-scrollable
  [opts child]
  [:> simplebar-react opts child])
