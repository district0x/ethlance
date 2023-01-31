(ns ethlance.shared.async-utils
  (:require [clojure.core.async
             :as
             async
             :refer
             [<! close! go go-loop]
             :include-macros
             true]
            [taoensso.timbre :as log]))

(defn log-error-channel
  "Logs the error output and only returns the success-channel."
  [[success-channel error-channel]]
  (go
   (when-let [err (<! error-channel)]
     (log/error (str err))
     (close! success-channel)))
  success-channel)


(defn throw-error-channel
  "Throws if there is error output on the error-channel.

  Return Value:

  The success-channel
  "
  [[success-channel error-channel]]
  (go
   (when-let [err (<! error-channel)]
     (close! success-channel)
     (log/error (str err))
     (throw (ex-info "Error on Async Error Channel" {:error-object err}))))
  success-channel)


(defn pull-error-channel
  "Pulls the error object from the channel. If there is no error, the
  channel will return nil (closed channel)."
  [[success-channel error-channel]]
  (go
   (when-let [result (<! success-channel)]
     (close! error-channel)
     (log/debug (str result))))
  error-channel)


(defn flush!
  "Takes a channel, and flushes the remaining values from the channel."
  [channel]
  (go-loop [result (<! channel)]
    (when result (recur (<! channel)))))
