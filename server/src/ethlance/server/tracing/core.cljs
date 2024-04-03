(ns ethlance.server.tracing.core
  (:require
    [district.server.config :refer [config]]
    [mount.core :as mount :refer [defstate]]
    [ethlance.server.tracing.setup :as setup]))

(defstate opentelemetry
  :start (setup/start (get @config :tracing))
  :stop (setup/stop))
