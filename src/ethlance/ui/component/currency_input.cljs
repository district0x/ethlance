(ns ethlance.ui.component.currency-input
  "Currency Component, for handling correct currency inputs"
  (:require
   [reagent.core :as r]
   [ethlance.shared.enumeration.currency-type :as enum.currency]))


(defn c-currency-input
  "Currency Component based on the react 'number' input component.

   # Notes

   - TODO: ensure the values are numeric"
  [{:keys [default-value value placeholder on-change currency-type color min] :as opts}]
  (let [*current-value (r/atom default-value)
        currency-symbol (case currency-type
                          ::enum.currency/eth "ETH"
                          ::enum.currency/usd "$"
                          "$")
        color-class (case color
                      :primary "primary"
                      :secondary "secondary"
                      "primary")]
    (fn [{:keys [default-value value placeholder on-change currency-type color min] :as opts}]
      (assert (not (and value default-value))
              "Component has both controlled `value` and uncontrolled `default-value` attributes set.")
      (let [current-value (if (contains? opts :default-value) @*current-value value)]
        [:div.currency-input
         {:class color-class}
         [:input {:type "number"
                  :min min
                  :placeholder placeholder
                  :value current-value
                  :on-change (fn [e] 
                               (reset! *current-value (-> e .-target .-value))
                               (when on-change (on-change (-> e .-target .-value))))}]
         [:span currency-symbol]]))))
