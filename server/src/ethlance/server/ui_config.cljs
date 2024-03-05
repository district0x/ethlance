(ns ethlance.server.ui-config
  (:require
    [cljs-node-io.core :as io]
    [cljs-node-io.fs :refer [file?]]
    [district.server.async-db :as db :include-macros true]
    [district.shared.async-helpers :refer [<? safe-go]]
    [ethlance.shared.utils :refer [deep-merge]]))


(def env js/process.env)


(defn fetch-config
  "Returns EDN contents from `config-path` (relative to the folder where node process was started)"
  [{:keys [default env-name]}]
  (let [config-path (aget env env-name)
        config-from-file (if (file? config-path)
                           (cljs.reader/read-string (io/slurp config-path))
                           {})
        final-config (deep-merge default config-from-file)]
    (.resolve js/Promise final-config)))
