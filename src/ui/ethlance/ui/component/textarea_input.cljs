(ns ethlance.ui.component.textarea-input
  (:require
   [reagent.core :as r]))


(defn c-textarea-input
  [{:keys [] :as opts}]
  [:textarea.ethlance-textarea-input opts])
