(ns cljsjs.axios
  (:require ["axios" :as axios]))

(js/goog.exportSymbol "axios" axios)
