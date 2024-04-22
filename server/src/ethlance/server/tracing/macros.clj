(ns ethlance.server.tracing.macros
  (:require
    [taoensso.timbre]
    [cljs.core.async.impl.ioc-macros :as ioc]
    [district.shared.error-handling]))

(defmacro go!
  "just like go, just executes immediately until the first put/take"
  [& body]
  `(let [c# (cljs.core.async/chan 1)
         f# ~(ioc/state-machine body 1 &env ioc/async-custom-terminators)
         state# (-> (f#)
                    (ioc/aset-all! cljs.core.async.impl.ioc-helpers/USER-START-IDX c#))]
     (cljs.core.async.impl.ioc-helpers/run-state-machine state#)
     c#))

(defmacro safe-go! [& body]
  `(go!
     (try
       ~@body
       (catch :default e#
         (when-let [span# (ethlance.server.tracing.api/get-active-span)]
           (ethlance.server.tracing.api/set-span-error! span# e#))
         (taoensso.timbre/error "Go block exception"
                                (merge {:error e#}
                                       (ex-data e#)
                                       ~(district.shared.error-handling/compiletime-info &env &form *ns*)))
         e#))))
