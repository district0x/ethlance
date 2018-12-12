(ns ethlance.shared.async-utils
  (:require
   [taoensso.timbre :as log]
   [clojure.core.async :as async :refer [go go-loop <! >! chan close!] :include-macros true]))


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
     (log/error err)
     (throw (ex-info "Error on async error channel" {:error-object err}))))
  success-channel)


(defn flush!
  "Takes a channel, and flushes the remaining values from the channel."
  [channel]
  (go-loop [result (<! channel)]
    (when result (recur (<! channel)))))
   
