(ns ethlance.ui.effects
  (:require [cljs-web3.core :as web3.core]
            [re-frame.core :as re]))

;; TODO : move this to maybe re-frame-web3-fx
(re/reg-fx
 :web3/personal-sign
 (fn [{:keys [web3 data-str from on-success on-error]}]
   (let [data (web3.core/to-hex data-str)]
#_     (.sendAsync (web3.core/current-provider web3)
                 (clj->js {:method "personal_sign"
                           :params [data-str from]
                           :from from})
                 (fn [err result]
                   (if err
                     (re/dispatch (conj on-error err))
                     (re/dispatch (conj on-success (.-result result)))))))))
