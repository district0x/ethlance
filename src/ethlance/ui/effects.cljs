(ns ethlance.ui.effects
  (:require [re-frame.core :as re]))

;; TODO : move this to maybe re-frame-web3-fx
(re/reg-fx
 :web3/personal-sign
 (fn [#_{:keys [web3 data-str from on-success on-error]}]
   #_(let [data (web3.core/to-hex data-str)]
     (.sendAsync (web3.core/current-provider web3)
                 (clj->js {:method "personal_sign"
                           :params [data-str from]
                           :from from})
                 (fn [err result]
                   (if err
                     (re/dispatch (conj on-error err))
                     (re/dispatch (conj on-success (.-result result)))))))))
