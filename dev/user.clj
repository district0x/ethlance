(ns user
  (:require [figwheel-sidecar.repl-api :as repl-api]
            [figwheel-sidecar.config :as config]))
 
(defn start-figwheel []
  (repl-api/start-figwheel! (config/fetch-config))
  (repl-api/cljs-repl)) 
