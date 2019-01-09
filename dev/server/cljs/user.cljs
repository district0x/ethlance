(ns cljs.user
  "Development Entrypoint for CLJS-Server."
  (:require
   [cljs-web3.eth :as web3-eth]
   [clojure.pprint :refer [pprint]]
   [cljs.tests :as tests]
   [cljs.instrumentation :as instrumentation]
   [mount.core :as mount] 
   [taoensso.timbre :as log]
   
   [district.graphql-utils :as graphql-utils]
   [district.server.graphql :as graphql]
   [district.server.logging]
   [district.server.web3 :refer [web3]]
   [district.server.smart-contracts :as contracts]
   [district.shared.error-handling :refer [try-catch try-catch-throw]]

   [ethlance.shared.smart-contracts]
   [ethlance.server.core]
   [ethlance.server.db]
   [ethlance.server.syncer]
   [ethlance.server.deployer :as deployer]
   [ethlance.server.generator :as generator]
   [ethlance.server.test-utils :as server.test-utils]
   [ethlance.server.test-runner :as server.test-runner]))
   

(def gql "Shorthand for district.server.graphql/run-query"
  graphql/run-query)


;; More GraphQL Shortcuts
(def gql-name->kw graphql-utils/gql-name->kw)
(def kw->gql-name graphql-utils/kw->gql-name)


(def help-message "
  CLJS-Server Repl Commands:

  -- Development Lifecycle --
  (start)                         ;; Starts the state components (reloaded workflow)
  (stop)                          ;; Stops the state components (reloaded workflow)
  (restart)                       ;; Restarts the state components (reloaded workflow)

  -- Development Helpers --
  (run-tests :reset? [false])     ;; Run the Server Tests (:reset? reset the testnet snapshot)
  (reset-testnet!)                ;; Reset the testnet snapshot
  (repopulate-database!)          ;; Resynchronize Smart Contract Events into the Database
  (restart-graphql!)              ;; Restart/Reload GraphQL Schema and Resolvers

  -- Instrumentation --
  (enable-instrumentation!)       ;; Enable fspec instrumentation
  (disable-instrumentation!)      ;; Disable fspec instrumentation

  -- GraphQL Utilities --
  (gql <query>)                   ;; Run GraphQL Query

  -- Misc --
  (help)                          ;; Display this help message

")


(def dev-graphql-config
  (-> ethlance.server.core/graphql-config
      (assoc :graphiql true)))


(def dev-config
  "Default district development configuration for mount components."
  (-> ethlance.server.core/main-config
      (merge {:logging {:level "debug" :console? true}})
      (merge {:db {:path "./resources/ethlance.db"
                   :opts {:memory false}}})
      (update :smart-contracts merge {:print-gas-usage? true
                                      :auto-mining? true})
      (assoc :graphql dev-graphql-config)))


(defn restart-graphql!
  "Restart the GraphQL State Component with new schema and resolver."
  []
  (graphql/restart dev-graphql-config))


(defn start-sync
  "Start the mount components."
  []
  (mount/start (mount/with-args dev-config)))


(defn start
  "Start the mount components asychronously."
  [& opts]
  (.nextTick
   js/process
   (fn []
     (apply start-sync opts))))


(defn stop-sync
  "Stop the mount components."
  []
  (mount/stop))


(defn stop
  [& opts]
  (.nextTick
   js/process
   (fn []
     (apply stop-sync opts))))
  

(defn restart-sync
  "Restart the mount components."
  []
  (stop-sync)
  (start-sync))


(defn restart
  [& opts]
  (.nextTick
   js/process
   (fn []
     (apply restart-sync opts))))


(defn run-tests-sync
  "Run server tests synchronously on the dev server.

  Optional Arguments

  reset? - Reset the smart-contract deployment snapshot

   Note: This will perform several smart contract redeployments with
  test defaults."
  [& {:keys [reset?]}]
  (log/info "Started Running Tests!")
  (when reset? (server.test-utils/reset-testnet!))
  (server.test-runner/run-tests)
  (log/info "Finished Running Tests!"))


(defn run-tests
  "Runs the server tests asynchronously on the dev server.
  
   Note: This will perform several smart contract redeployments with
  test defaults."
  [& {:keys [reset?]}]
  (log/info "Running Server Tests Asynchronously...")
  (.nextTick js/process #(run-tests-sync :reset? reset?)))


(defn enable-instrumentation!
  "Strict conforms function fspecs for all specs."
  []
  (instrumentation/enable!))


(defn disable-instrumentation!
  "Disables strict conformity of fspecs."
  []
  (instrumentation/disable!))


(defn reset-testnet!
  "Resets the testnet deployment snapshot for server tests."
  []
  (server.test-utils/reset-testnet!))


(defn repopulate-database-sync!
  "Purges the database, re-synchronizes the blockchain events, and
  re-populates the database."
  []
  (log/info "Repopulating database...")
  (log/debug "Stopping syncer and database...")
  (mount/stop
   #'ethlance.server.syncer/syncer
   #'ethlance.server.db/ethlance-db)
  (log/debug "Starting syncer and database...")
  (mount/start
   #'ethlance.server.db/ethlance-db
   #'ethlance.server.syncer/syncer))


(defn repopulate-database!
  "Repopulate database asynchronously."
  []
  (.nextTick js/process repopulate-database-sync!))


(defn help
  "Display a help message on development commands."
  []
  (println help-message))


(defn -dev-main
  "Commandline Entry-point for node dev_server.js"
  [& args]
  (help))


(set! *main-cli-fn* -dev-main)
