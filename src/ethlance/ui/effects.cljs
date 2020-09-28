(ns ethlance.ui.effects
  (:require [cljs-web3.core :as web3.core]
            [cljs-web3.eth :as web3.eth]
            [re-frame.core :as re]))

;; TODO : move this to maybe re-frame-web3-fx
(re/reg-fx
 :web3/personal-sign
 (fn [{:keys [web3 data-str from on-success on-error]}]
   (let [data (web3.core/to-hex data-str)]
     (.sendAsync (web3.core/current-provider web3)
                (clj->js {:method "personal_sign"
                          :params [data-str from]
                          :from from})
                (fn [err result]
                  (if err
                    (re/dispatch (conj on-error err))
                    (re/dispatch (conj on-success (.-result result)))))))))

(def abi-erc20-symbol (js/JSON.parse "[{\"constant\": true, \"inputs\": [], \"name\": \"symbol\", \"outputs\": [{\"name\": \"\", \"type\": \"string\"}], \"payable\": false, \"stateMutability\": \"view\", \"type\": \"function\"}]"))

(re/reg-fx
 :web3.erc20/fetch-token-symbol
 (fn [{:keys [web3 token-address]}]
   (when-not (empty? token-address)
     (let [token-contract (web3.eth/contract-at web3 abi-erc20-symbol token-address)]
       (.symbol token-contract (fn [_ symb]
                                 (re/dispatch [:page.new-job/set-token-symbol symb])))))))