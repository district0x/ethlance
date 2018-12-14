(ns ethlance.shared.async-utils)


(defmacro <!-<log
  "Pulls from success channel, logs error channel."
  [form]
  `(clojure.core.async/<!
    (ethlance.shared.async-utils/log-error-channel ~form)))


(defmacro <!-<throw
  "Pulls from success channel, throws on error channel."
  [form]
  `(clojure.core.async/<!
    (ethlance.shared.async-utils/throw-error-channel ~form)))


(defmacro go-try
  "Wraps the `go` channel in a try-catch, and logs any errors

  # Notes

  - Also ensures that the channel returned by `go` returns `:done`"
  [& body]
  `(clojure.core.async/go
     (try
       ~@body
       (catch :default e#
         (taoensso.timbre/error "Exception in Async Go Block")
         ;; Try and break down the error for better formatting of spec conform
         (cond
           (.-message e#) (taoensso.timbre/error (.-message e#))
           :else (taoensso.timbre/error e#)))
     :done)))
