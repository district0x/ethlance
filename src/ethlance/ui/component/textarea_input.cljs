(ns ethlance.ui.component.textarea-input
  (:require
   [reagent.core :as r]))


(defn c-textarea-input
  "Default TextArea Input Component

  # Keyword Arguments
  
  opts - React Props
  "
  [{:keys [] :as opts}]
  [:textarea.ethlance-textarea-input opts])
