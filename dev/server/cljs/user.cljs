(ns cljs.user
  "Development Entrypoint for CLJS-Server."
  (:require [cljs-web3.eth :as web3-eth]
            [cljs.instrumentation :as instrumentation]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as string]
            [district.server.db]
            [district.server.logging]
            [district.server.smart-contracts :as contracts]
            [district.server.web3 :refer [web3]]
            [district.server.web3-events]
            [district.shared.error-handling :refer [try-catch try-catch-throw]]
            [ethlance.server.core]
            [ethlance.server.db :as ethlance-db]
            [ethlance.server.syncer]
            [ethlance.server.test-runner :as server.test-runner]
            [ethlance.server.test-utils :as server.test-utils]
            [ethlance.shared.smart-contracts-dev :as smart-contracts-dev]
            [honeysql.core :as sql]
            [mount.core :as mount]
            [taoensso.timbre :as log]))

(def sql-format
  "Shorthand for honeysql.core/format"
  sql/format)

(def help-message "
  CLJS-Server Repl Commands:

  -- Development Lifecycle --
  (start)                         ;; Starts the state components (reloaded workflow)
  (stop)                          ;; Stops the state components (reloaded workflow)
  (restart)                       ;; Restarts the state components (reloaded workflow)

  -- Development Helpers --
  (run-tests)                     ;; Run the Server Tests (:reset? reset the testnet snapshot)
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
  (-> ethlance.server.core/default-config
      (merge {:logging {:level "debug" :console? true}})
      (merge {:db {:path "./resources/ethlance.db"
                   :opts {:memory false}}})
      (update :smart-contracts merge {:contracts-var #'smart-contracts-dev/smart-contracts
                                      :print-gas-usage? true
                                      :auto-mining? true})
      (assoc :graphql dev-graphql-config)))


(defn start-sync
  "Start the mount components."
  []
  (-> (mount/with-args dev-config)
      mount/start))


(defn enable-instrumentation!
  "Strict conforms function fspecs for all specs."
  []
  (instrumentation/enable!))


(defn disable-instrumentation!
  "Disables strict conformity of fspecs."
  []
  (instrumentation/disable!))


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
  []
  (log/info "Started Running Tests!")
  (server.test-runner/run-all-tests)
  (log/info "Finished Running Tests!"))


(defn run-tests
  "Runs the server tests asynchronously on the dev server.
   Note: This will perform several smart contract redeployments with
  test defaults."
  []
  (log/info "Running Server Tests Asynchronously...")
  (.nextTick js/process run-tests-sync))

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
   #'district.server.web3-events/web3-events
   #'ethlance.server.db/ethlance-db)
  (log/debug "Starting syncer and database...")
  (mount/start
   #'ethlance.server.db/ethlance-db
   #'district.server.web3-events/web3-events
   #'ethlance.server.syncer/syncer))


(defn repopulate-database!
  "Repopulate database asynchronously."
  []
  (.nextTick js/process repopulate-database-sync!))


(defn help
  "Display a help message on development commands."
  []
  (println help-message))

(defn generate-dev-data []
  (for [id (range 0 101)]
    (let [[country-code _] (shuffle ["US" "BE" "UA" "CA" "SLO" "PL"])
          [first-name _] (shuffle ["Filip" "Juan" "Ben" "Matus"])
          [second-name _] (shuffle ["Fu" "Bar" "Smith" "Doe" "Hoe"])
          [extension _] (shuffle ["io" "com" "gov"])
          [profile-id _] (shuffle (range 0 10))
          [candidate? _] (shuffle [true false])
          [currency _] (shuffle ["EUR" "USD"])]
      (ethlance-db/insert-row! :User {:user/address (str id)
                                      :user/country-code country-code
                                      :user/user-name (str "@" first-name)
                                      :user/full-name (str first-name " " second-name)
                                      :user/email (string/lower-case (str first-name "@" second-name "." extension))
                                      :user/profile-image (str "https://randomuser.me/api/portraits/lego/" profile-id ".jpg")})
      (when candidate?
        (ethlance-db/insert-row! :Candidate {:user/address (str id)
                                             :candidate/rate (rand-int 200)
                                             :candidate/rate-currency-id currency})))))

(defn -dev-main
  "Commandline Entry-point for node dev_server.js"
  [& args]
  (help))


(set! *main-cli-fn* -dev-main)
