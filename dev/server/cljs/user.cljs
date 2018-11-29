(ns cljs.user
  "Development Entrypoint for CLJS-Server."
  (:require
   [cljs-web3.eth :as web3-eth]
   [cljs.tests :as tests]
   [clojure.spec.alpha :as s]
   [expound.alpha :as expound]
   [mount.core :as mount] 
   [orchestra-cljs.spec.test :as st]
   [taoensso.timbre :as log]
   
   [district.server.web3 :refer [web3]]
   [district.server.smart-contracts :as contracts]

   [ethlance.shared.smart-contracts]
   [ethlance.server.core]
   [ethlance.server.deployer :as deployer]
   [ethlance.server.test-utils :as server.test-utils]
   [ethlance.server.test-runner :as server.test-runner]))


(def help-message "
  CLJS-Server Repl Commands:

  (start)                         ;; Starts the state components (reloaded workflow)
  (stop)                          ;; Stops the state components (reloaded workflow)
  (restart)                       ;; Restarts the state components (reloaded workflow)

  (run-tests :reset? [false])     ;; Run the Server Tests (:reset? reset the testnet snapshot)
  (reset-testnet!)                ;; Reset the testnet snapshot
  (redeploy)                      ;; Deploy to the testnet asynchronously

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


#_(defn run-test-sync
    "Run tests with the given namespace"
    [namespace]
    (server.test-runner/run-test namespace))


#_(defn run-test
    "Run a single test asynchronously"
    [namespace]
    (.nextTick js/process #(run-test-sync namespace)))


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
  (help)
  (start))


(set! *main-cli-fn* -dev-main)

;; Better Spec Error Messages by default
(set! s/*explain-out* expound/printer)

;; Turning on system-wide Spec Instrumentation
;; FIXME: turned off until there is a fix for non-public function warnings.
;; (st/instrument)

