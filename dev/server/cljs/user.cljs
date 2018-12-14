(ns cljs.user
  "Development Entrypoint for CLJS-Server."
  (:require
   [cljs-web3.eth :as web3-eth]
   [clojure.pprint :refer [pprint]]
   [cljs.tests :as tests]
   [cljs.instrumentation :as instrumentation]
   [mount.core :as mount] 
   [taoensso.timbre :as log]
   
   [district.server.logging]
   [district.server.web3 :refer [web3]]
   [district.server.smart-contracts :as contracts]
   [district.shared.error-handling :refer [try-catch try-catch-throw]]

   [ethlance.shared.smart-contracts]
   [ethlance.server.core]
   [ethlance.server.deployer :as deployer]
   [ethlance.server.generator :as generator]
   [ethlance.server.test-utils :as server.test-utils]
   [ethlance.server.test-runner :as server.test-runner]))


(def help-message "
  CLJS-Server Repl Commands:

  (start)                         ;; Starts the state components (reloaded workflow)
  (stop)                          ;; Stops the state components (reloaded workflow)
  (restart)                       ;; Restarts the state components (reloaded workflow)

  (run-tests :reset? [false])     ;; Run the Server Tests (:reset? reset the testnet snapshot)
  (reset-testnet!)                ;; Reset the testnet snapshot
  (redeploy :generate? [false])   ;; Deploy to the testnet asynchronously
                                  ;; (:generate? generate users and scenarios)

  (enable-instrumentation!)       ;; Enable fspec instrumentation
  (disable-instrumentation!)      ;; Disable fspec instrumentation

  (help)                          ;; Display this help message

")


(def dev-config
  "Default district development configuration for mount components."
  (-> ethlance.server.core/main-config
      (merge {:logging {:level "debug" :console? true}})
      (merge {:db {:path "./resources/ethlance.db"
                   :opts {:memory false}}})
      (update :smart-contracts merge {:print-gas-usage? true
                                      :auto-mining? true})))


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


(defn redeploy-sync
  "Redeploy the smart contracts for development.

  Optional Arguments:

  :generate? - If generate is true, the testnet will generate several
  employers, candidates and arbiters undergoing particular work scenarios.

  Notes:

  - please read the docs for `ethlance.server.deployer/deploy-all!`
  "
  [& {:keys [generate?] :as opts}]
  (try-catch-throw
   (log/info "Starting Contract Deployment!")
   (apply deployer/deploy-all! opts)
   (log/info "Finished Contract Deployment!")
   (when generate? (generator/generate!))))


(defn redeploy
  "Performs a redeployment asynchronously"
  [& opts]
  (.nextTick
   js/process
   (fn []
     (apply redeploy-sync opts))))


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


(defn help
  "Display a help message on development commands."
  []
  (println help-message))


(defn -dev-main
  "Commandline Entry-point for node dev_server.js"
  [& args]
  (help))


(set! *main-cli-fn* -dev-main)
