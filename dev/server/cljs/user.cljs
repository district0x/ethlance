(ns cljs.user
  "Development Entrypoint for CLJS-Server."
  (:require
   [mount.core :as mount]
   [cljs-web3.eth :as web3-eth]
   
   [district.server.web3 :refer [web3]]
   [district.server.smart-contracts :as contracts]

   [ethlance.shared.smart-contracts]
   [ethlance.server.core]
   [ethlance.server.deployer :as deployer]))


(def help-message "
  CLJS-Server Repl Commands:

  (start)         ;; Starts the state components (reloaded workflow)
  (stop)          ;; Stops the state components (reloaded workflow)
  (restart)       ;; Restarts the state components (reloaded workflow)

  (redeploy)      ;; Deploy to the testnet

  (help)          ;; Display this help message

")


(def dev-config
  (-> ethlance.server.core/main-config
      (merge {:logging {:level "debug" :console? true}})
      (update :smart-contracts merge {:print-gas-usage? true
                                      :auto-mining? true})))


(defn start []
  (mount/start (mount/with-args dev-config)))


(defn stop []
  (mount/stop))


(defn restart []
  (stop)
  (start))


(defn redeploy []
  (deployer/deploy-all! {} :write? true))


(defn help []
  (println help-message))


(defn -dev-main [& args]
  (help)
  (start))


(set! *main-cli-fn* -dev-main)
