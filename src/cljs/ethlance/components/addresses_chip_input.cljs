(ns ethlance.components.addresses-chip-input
  (:require
    [cljs-web3.core :as web3]
    [ethlance.components.chip-input :refer [chip-input]]
    [ethlance.components.validated-chip-input :refer [validated-chip-input]]
    [ethlance.constants :as constants]
    [ethlance.styles :as styles]
    [ethlance.utils :as u]
    [re-frame.core :refer [subscribe dispatch]]
    [reagent.core :as r]))

(defn addresses-chip-input [{:keys [:form-key]}]
  (let [form (subscribe [form-key])]
    (fn [{:keys [:form-key :field-key :value] :as props}]
      (let [errors (:errors @form)]
        [validated-chip-input
         (r/merge-props
           (merge
             {:on-request-add (fn [address validator]
                                (if (web3/address? address)
                                  (let [new-value (into [] (conj value address))]
                                    (dispatch [:form/set-value form-key field-key new-value validator])
                                    (dispatch [:form/remove-error form-key :invalid-address]))
                                  (dispatch [:form/add-error form-key :invalid-address])))
              :on-request-delete (fn [address validator]
                                   (let [new-value (into [] (remove (partial = address) value))]
                                     (dispatch [:form/set-value form-key field-key new-value validator])
                                     (dispatch [:form/remove-error form-key :invalid-address])))
              :full-width-input true}
             (when (contains? errors :invalid-address)
               {:error-text "You entered invalid address"}))
           props)]))))
