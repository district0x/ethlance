(ns ethlance.components.languages-chip-input
  (:require [ethlance.components.chip-input :refer [chip-input]]
            [ethlance.constants :as constants]
            [ethlance.utils :as u]
            [goog.string :as gstring]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]))

(defn languages-chip-input []
  (let [eth-config (subscribe [:eth/config])]
    (fn [{:keys [:form-key :value] :as props}]
      (let [{:keys [:max-user-languages :min-user-languages]} @eth-config
            languages-count (count value)
            validator #(<= min-user-languages (count %) max-user-languages)]
        [chip-input
         (r/merge-props
           {:all-items constants/languages
            :on-change #(dispatch [:form/set-value form-key :user/languages %1 validator])
            :error-text (cond
                          (or (empty? value) (< languages-count min-user-languages))
                          (gstring/format "Select at least %s %s"
                                          min-user-languages
                                          (u/pluralize "language" min-user-languages))

                          (> languages-count max-user-languages)
                          (gstring/format "You can select up to %s languages" max-user-languages)

                          :else nil)}
           (dissoc props :form-key))]))))
