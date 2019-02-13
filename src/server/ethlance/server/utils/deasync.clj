(ns ethlance.server.utils.deasync)


(defmacro go-deasync
  "Deasync the given core.async body."
  [& body]
  `(let [lock# (atom true)]
     (try
       (do
         (clojure.core.async/go
           (try
             (do ~@body)
             (catch :default e1#
               (taoensso.timbre/error (str "Exception in Deasync Go Block: " e1#))
               (throw e1#))
             (finally (reset! lock# false))))
         (try
           (.loopWhile ethlance.server.utils.deasync/deasync-lib (fn [] @lock#))
           (catch :default e2#
             (taoensso.timbre/error (str "Exception in Deasync Loop: " e2#))
             (throw e2#))))
       (catch :default e3#
         (taoensso.timbre/error (str "Exception outside Go Block: " e3#))))))
