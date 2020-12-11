(ns ethlance.ui.effects
  (:require
    [re-frame.core :as re]
    [cljs-web3.core :as web3]))

;; TODO : move this to maybe re-frame-web3-fx
(re/reg-fx
  :web3/personal-sign
  (fn [{:keys [web3 data-str from on-success on-error]}]
    (.sendAsync (web3/current-provider web3)
                (clj->js {:method "personal_sign"
                          :params [data-str from]
                          :from from})
                (fn [err result]
                  (if err
                    (re/dispatch (conj on-error err))
                    (re/dispatch (conj on-success (aget result "result"))))))))
