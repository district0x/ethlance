(ns ethlance.server.utils.deasync)


(defmacro deasync
  "Deasync the given core.async body."
  [& body]
  `(let [lock# (atom false)
         release-fn# (fn [] (reset! lock# true))]
     (clojure.core.async/go
      (try
        ~@body
        (finally (release-fn#))))
     (.loopWhile ethlance.server.utils.deasync/deasync-lib (fn [] @lock#))))
         
    
