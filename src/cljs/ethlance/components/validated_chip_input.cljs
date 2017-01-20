(ns ethlance.components.validated-chip-input
  (:require [ethlance.components.chip-input :refer [chip-input]]
            [ethlance.constants :as constants]
            [ethlance.utils :as u]
            [goog.string :as gstring]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]))

(defn validated-chip-input []
  (let [eth-config (subscribe [:eth/config])]
    (fn [{:keys [:form-key :field-key :value :min-length-key :max-length-key] :as props}]
      (let [min-count (get @eth-config min-length-key)
            max-count (get @eth-config max-length-key)
            value-count (count value)
            validator #(<= min-count (count %) max-count)]
        [chip-input
         (r/merge-props
           {:on-change #(dispatch [:form/set-value form-key field-key %1 validator])
            :error-text (cond
                          (or (empty? value) (< value-count min-count))
                          (gstring/format "Select at least %s %s"
                                          min-count
                                          (u/pluralize "item" min-count))

                          (> value-count max-count)
                          (gstring/format "You can select up to %s items" max-count)

                          :else nil)}
           (dissoc props :form-key :field-key :min-length-key :max-length-key))]))))
