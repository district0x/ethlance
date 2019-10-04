(ns ethlance.ui.component.email-input
  (:require
   [reagent.core :as r]))


(defn c-email-input
  "Email Input Component

   # Notes

   - TODO: determine whether a valid email was entered."
  [{:keys [] :as opts}]
  [:input.ethlance-email-input opts])
