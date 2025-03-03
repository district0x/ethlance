(ns ethlance.server.core
  (:require
    ["fs" :as fs]
    [district.server.async-db]
    [district.server.config :as district-config]
    [district.server.db]
    [district.server.db.honeysql-extensions]
    [district.server.logging]
    [district.server.smart-contracts]
    [ethlance.server.tracing.core]
    [district.server.web3]
    [district.server.web3-events]
    [district.shared.async-helpers :as async-helpers :refer [safe-go]]
    [ethlance.server.config :as server-config]
    [ethlance.server.db]
    [ethlance.server.graphql.server]
    [ethlance.server.ipfs]
    [ethlance.server.syncer]
    ; [ethlance.server.new-syncer]
    [mount.core :as mount]
    [taoensso.timbre :refer [merge-config!] :as log]))


(defn -main
  [& _]
  (log/info "Initializing Server...")
  (async-helpers/extend-promises-as-channels!)
  (merge-config! {:ns-blacklist ["district.server.smart-contracts"]})
  (safe-go
    (try
      (let [start-result (-> (mount/with-args
                               {:config
                                {:env-name "SERVER_CONFIG_PATH"
                                 :default (server-config/env-config server-config/environment)}})
                             (mount/start))]
        (log/warn "Started" {:components start-result :config @district-config/config}))
      (catch js/Error e
        (log/error "Something went wrong when starting the application" {:error e})))))


;; When compiled for a command-line target, whatever function *main-cli-fn* is
;; set to will be called with the command-line argv as arguments.
;;   See: https://cljs.github.io/api/cljs.core/STARmain-cli-fnSTAR
(set! *main-cli-fn* -main)
