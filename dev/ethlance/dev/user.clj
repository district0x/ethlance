(ns ethlance.dev.user
  (:require
   [figwheel-sidecar.repl-api :as fw-repl]
   [figwheel-sidecar.config :as fw-config]))


(defn start-ui! []
  (fw-repl/start-figwheel! (fw-config/fetch-config))
  (fw-repl/cljs-repl))


