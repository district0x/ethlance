(ns ethlance.shared.async-utils
  "Async functions for pulling from a [success-channel error-channel] pair
  
  # Notes:

  - functions are named after how values are pulled from the
  success-channel - error-channel.")


(defmacro <!-<log
  "Pulls from success channel, logs error channel."
  [form]
  `(cljs.core.async/<!
    (ethlance.shared.async-utils/log-error-channel ~form)))


(defmacro <!-<throw
  "Pulls from success channel, throws on error channel."
  [form]
  `(cljs.core.async/<!
    (ethlance.shared.async-utils/throw-error-channel ~form)))


(defmacro <ignore-<!
  "Pulls from the error channel. Useful for testing failure.

   ex. (is (<log-<! (my-func))) ;; test expects an error object"
  [form]
  `(cljs.core.async/<!
    (ethlance.shared.async-utils/pull-error-channel ~form)))


(defmacro go-try
  "Wraps the `go` channel in a try-catch, and logs any errors

  # Notes

  - Also ensures that the channel returned by `go` returns `:done`"
  [& body]
  `(cljs.core.async/go
     (try
       ~@body
       (catch :default e#
         (taoensso.timbre/error "Exception in Async Go Block")
         ;; Try and break down the error for better formatting of spec conform
         (cond
           (aget e# "message") (taoensso.timbre/error (aget e# "message"))
           :else (taoensso.timbre/error e#)))
     :done)))
