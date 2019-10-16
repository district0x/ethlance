(ns ethlance.ui.component.currency-input
  "Currency Component, for handling correct currency inputs"
  (:require
   [ethlance.shared.enumeration.currency-type :as enum.currency]))


(defn c-currency-input
  "Currency Component based on the 'text' input component.

   # Notes

   - TODO: ensure the values are numeric"
  [{:keys [placeholder on-change currency-type color] :as opts}]
  (let [currency-symbol (case currency-type
                          ::enum.currency/eth "ETH"
                          ::enum.currency/usd "$"
                          "$")
        color-class (case color
                     :primary "primary"
                     :secondary "secondary"
                     "primary")]
    [:div.currency-input
     {:class color-class}
     [:input {:type "text" :placeholder placeholder}]
     [:span currency-symbol]]))
