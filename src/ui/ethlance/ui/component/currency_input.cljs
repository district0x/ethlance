(ns ethlance.ui.component.currency-input
  (:require
   [ethlance.shared.enumeration.currency-type :as enum.currency]))


(defn c-currency-input
  [{:keys [placeholder on-change currency-type] :as opts}]
  (let [currency-symbol (case currency-type
                          ::enum.currency/eth "ETH"
                          ::enum.currency/usd "$"
                          "$")]
    [:div.currency-input
     [:input {:type "text" :placeholder placeholder}]
     [:span currency-symbol]]))
