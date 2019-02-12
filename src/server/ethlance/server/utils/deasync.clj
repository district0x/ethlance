(ns ethlance.server.utils.deasync)


(defmacro go-deasync
  "Deasync the given core.async body."
  [& body]
  `(let [lock# (atom true)]
     (clojure.core.async/go
      (try
        ~@body
        (finally (reset! lock# false))))
     (.loopWhile ethlance.server.utils.deasync/deasync-lib (fn [] @lock#))))

