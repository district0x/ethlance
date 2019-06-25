(ns ethlance.ui.component.currency-input
  (:require
   [ethlance.shared.enumeration.currency-type :as enum.currency]))


(defn c-currency-input
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
