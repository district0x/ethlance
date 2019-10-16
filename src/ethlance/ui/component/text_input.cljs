(ns ethlance.ui.component.text-input
  (:require
   [reagent.core :as r]))


(defn c-text-input
  "Default Text Input Component

  # Keyword Arguments
  
  opts - React Props
  "
  [{:keys [] :as opts}]
  [:input.ethlance-text-input opts])
