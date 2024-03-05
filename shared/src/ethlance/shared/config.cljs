(ns ethlance.shared.config
  (:require
    [cljs.reader]
    [ethlance.shared.utils :as shared-utils])
  (:require-macros
    [ethlance.shared.utils :refer [read-from-env-path]]))


(def config
  (cljs.reader/read-string (read-from-env-path "ETHLANCE_CONFIG_PATH")))
