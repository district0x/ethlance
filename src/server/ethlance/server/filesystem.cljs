(ns ethlance.server.filesystem
  "Functions wrapping nodejs 'fs' package with core.async"
  (:require
   [clojure.core.async :as async :refer [go go-loop <! >! chan close!] :include-macros true]))


(def fs (js/require "fs"))


(defn read-file
  [file-path]
  (let [success-chan (chan 1) error-chan (chan 1)]
    (go
      (.readFile
       fs file-path
       (fn [error result]
         (when error
           (>! error-chan)
           (close! success-chan))
         (when result
           (>! success-chan result)
           (close! error-chan)))))
    [success-chan error-chan]))
