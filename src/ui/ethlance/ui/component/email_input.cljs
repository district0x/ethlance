(ns ethlance.ui.component.email-input
  (:require
   [reagent.core :as r]))


(defn c-email-input
  [{:keys [] :as opts}]
  [:input.ethlance-email-input opts])
