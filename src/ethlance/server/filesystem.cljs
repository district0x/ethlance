(ns ethlance.server.filesystem
  (:require [clojure.core.async
             :as
             async
             :refer
             [>! chan close! go]
             :include-macros
             true]))

(def fs (js/require "fs"))

(defn read-file
  [file-path]
  (let [success-chan (chan 1) error-chan (chan 1)]
    (go
      (.readFile
       fs file-path
       (fn [error result]
         (when error
           (>! error-chan error)
           (close! success-chan))
         (when result
           (>! success-chan result)
           (close! error-chan)))))
    [success-chan error-chan]))
