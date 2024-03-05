(ns ethlance.ui.effects
  (:require
    [cljs-web3.core :as web3]
    [re-frame.core :as re]))


;; TODO : move this to maybe re-frame-web3-fx
(re/reg-fx
  :web3/personal-sign
  (fn [{:keys [web3 data-str from on-success on-error]}]
    (.sendAsync ^js (web3/current-provider web3)
                (clj->js {:method "personal_sign"
                          :params [data-str from]
                          :from from})
                (fn [err result]
                  (if err
                    (re/dispatch (conj on-error err))
                    (re/dispatch (conj on-success (aget result "result"))))))))
