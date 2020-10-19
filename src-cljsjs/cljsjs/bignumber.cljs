(ns cljsjs.bignumber
  (:require ["bignumber.js" :as bignum]))

(js/goog.exportSymbol "BigNumber" bignum)
