(ns ethlance.dev.user
  (:require
   [figwheel-sidecar.repl-api :as fig-repl]
   [figwheel-sidecar.config :as fig-config]))
 


(def help-msg "
CLJ Repl Commands:

  (start-server!)        ;; Build and Watch the Node Development Server.
                         ;; Open the CLJS-Server Repl

  (start-ui!)            ;; Build and Watch the Browser Development UI
                         ;; Open the CLJS-UI Repl

  (start-tests!)         ;; Build and Watch the Browser Tests

  (help)                 ;; Display this help message
")


(defn server-port []
  (get-in (fig-config/fetch-config) [:data :figwheel-options :server-port]))


(defn set-server-port [config port]
  (assoc-in config [:data :figwheel-options :server-port] port))


(defn nrepl-port []
  (get-in (fig-config/fetch-config) [:data :figwheel-options :nrepl-port]))


(defn set-repl-port [config port]
  (assoc-in config [:data :figwheel-options :nrepl-port] port))


(defn start-server! []
  (let [;; Ports are +1 of default figwheel config values
        server-port (+ (server-port) 1)
        nrepl-port (+ (nrepl-port) 1)

        config (-> (fig-config/fetch-config)
                   (set-server-port server-port)
                   (set-repl-port nrepl-port))]
    (fig-repl/start-figwheel! config "dev-server")
    (fig-repl/cljs-repl "dev-server")))


(defn start-ui! []
  (let [;; Ports are default figwheel config values
        config (fig-config/fetch-config)]
    (fig-repl/start-figwheel! config "dev-ui")
    (fig-repl/cljs-repl "dev-ui")))


(defn start-tests! []
  (let [;; Ports are +2 of default figwheel config values
        server-port (+ (server-port) 2)
        nrepl-port (+ (nrepl-port) 2)

        config (-> (fig-config/fetch-config)
                   (set-server-port server-port)
                   (set-repl-port nrepl-port))]
    (fig-repl/start-figwheel! config "test-server")
    (fig-repl/cljs-repl "test-server")))


(defn help [] (println help-msg))
