(ns ethlance.ui.component.text-input
  (:require
   [reagent.core :as r]))


(defn c-text-input
  [{:keys [] :as opts}]
  [:input.ethlance-text-input opts])
