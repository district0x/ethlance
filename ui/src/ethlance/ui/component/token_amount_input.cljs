(ns ethlance.ui.component.token-amount-input
  (:require
    [ethlance.ui.component.text-input :as text-input]
    [ethlance.ui.util.tokens :as util-tokens]
    [cljs.math]))


(defn- non-zero-fraction? [num-str]
  (#{"1" "2" "3" "4" "5" "6" "7" "8" "9" "." ","} (last num-str)))

(defn leading-zeroes-preserving-round
  "Returns original value if the val is zero expressing string (e.g. 0 or 0.0)
  When it becomes non-zero (e.g. 0.01) then returns the result (transform-fn val)"
  [val transform-fn]
  (if (non-zero-fraction? val)
    (transform-fn val)
    val))

(defn c-token-amount-input
  [{:keys [decimals on-change] :as opts}]
  (let [text-input-opts (dissoc opts :decimals)
        ;; Even though tokens (including ETH) can have 18 decimals, using so many in the UI isn't practical
        max-ui-decimals 3
        decimals-for-ui (min decimals max-ui-decimals)
        step (/ 1 (cljs.math/pow 10 decimals-for-ui))
        ;; Should use method similar to https://stackoverflow.com/a/10880710/1025412
        human->token-amount (fn [human-amount]
                              (.round js/Math (* (cljs.math/pow 10 decimals) human-amount)))
        token-on-change (fn [human-amount]
                          (on-change {:token-amount (human->token-amount human-amount)
                                      :human-amount human-amount
                                      :decimals decimals}))
        amount-extras {:type :number :step step :on-change token-on-change}
        value-rounded-to-decimals (leading-zeroes-preserving-round
                                    (:value opts)
                                    #(util-tokens/round decimals-for-ui %))]
    [text-input/c-text-input (merge text-input-opts amount-extras {:value value-rounded-to-decimals})]))
