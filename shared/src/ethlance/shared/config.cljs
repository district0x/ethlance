(ns ethlance.shared.config
  (:require-macros [ethlance.shared.utils :refer [read-from-env-path]])
  (:require [ethlance.shared.utils :as shared-utils]
            [cljs.reader]))

(def config
  (cljs.reader/read-string (read-from-env-path "ETHLANCE_CONFIG_PATH")))
