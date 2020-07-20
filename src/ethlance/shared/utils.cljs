(ns ethlance.shared.utils
  (:require-macros [ethlance.shared.utils]))

(defn now []
  (.getTime (js/Date.)))
