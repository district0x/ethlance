(ns cljs.user
  "Development Entrypoint for CLJS-Server."
  (:require
   [mount.core :as mount]
   [cljs-web3.eth :as web3-eth]
   [taoensso.timbre :as log]
   
   [district.server.web3 :refer [web3]]
   [district.server.smart-contracts :as contracts]

   [ethlance.shared.smart-contracts]
   [ethlance.server.core]
   [ethlance.server.deployer :as deployer]

   [ethlance.server.test-runner :as server.test-runner]))


(def help-message "
  CLJS-Server Repl Commands:

  (start)         ;; Starts the state components (reloaded workflow)
  (stop)          ;; Stops the state components (reloaded workflow)
  (restart)       ;; Restarts the state components (reloaded workflow)

  (run-tests)     ;; Run the Server Tests
  (redeploy)      ;; Deploy to the testnet asynchronously
  (redeploy-sync) ;; Deploy to the testnet synchronously

  (help)          ;; Display this help message

")


(def dev-config
  "Default district development configuration for mount components."
  (-> ethlance.server.core/main-config
      (merge {:logging {:level "debug" :console? true}})
      (update :smart-contracts merge {:print-gas-usage? true
                                      :auto-mining? true})))


(defn start
  "Start the mount components."
  []
  (mount/start (mount/with-args dev-config)))


(defn stop
  "Stop the mount components."
  []
  (mount/stop))


(defn restart
  "Restart the mount components."
  []
  (stop)
  (start))


(defn redeploy-sync
  "Redeploy the smart contracts for development.

   Notes:

   - please read the docs for `ethlance.server.deployer/deploy-all!`"
  [& opts]
  (log/info "Starting Deployment!")
  (apply deployer/deploy-all! opts)
  (log/info "Finished Deployment!"))


(defn redeploy
  "Performs a redeployment asynchronously"
  [& opts]
  (.nextTick
   js/process
   (fn []
     (apply redeploy-sync opts))))


(defn run-tests-sync
  "Run server tests synchronously on the dev server.

   Note: This will perform several smart contract redeployments with
  test defaults."
  []
  (log/info "Started Running Tests!")
  (server.test-runner/run-tests)
  (log/info "Finished Running Tests!"))


(defn run-tests
  "Runs the server tests asynchronously on the dev server.
  
   Note: This will perform several smart contract redeployments with
  test defaults."
  []
  (log/info "Running Server Tests Asynchronously...")
  (.nextTick js/process run-tests-sync))
  


(defn help
  "Display a help message on development commands."
  []
  (println help-message))


(defn -dev-main
  "Commandline Entry-point for node dev_server.js"
  [& args]
  (help)
  (start))


(set! *main-cli-fn* -dev-main)
