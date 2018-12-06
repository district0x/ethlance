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
