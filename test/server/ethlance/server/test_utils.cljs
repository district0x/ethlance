(ns ethlance.server.test-utils
  "Includes fixtures and additional utilities to improve the process of
  testing ethereum contracts."
  (:require
   [clojure.test :refer [deftest is are testing use-fixtures async]]

   [taoensso.timbre :as log]
   [cljs-web3.eth :as web3-eth]
   [cljs-web3.evm :as web3-evm]
   [mount.core :as mount]
   
   [ethlance.server.core]
   [ethlance.server.deployer :as deployer]

   ;; Mount Components
   [district.server.web3 :refer [web3]]
   [district.server.smart-contracts :as contracts]))

;;
;; Node Modules
;;

(def deasync (js/require "deasync"))


;; The snapshot of the testnet after the first deployment. This saves
;; time in between smart contract tests by simply reverting the
;; testnet instead of performing a redeployment."
(defonce *deployment-testnet-snapshot (atom nil))


(def *snapshot-lock (atom false)) ; Lock to force deasync
(defn snapshot-testnet!
  "Retrieves the current blockchain snapshot, and places it in
  `*deployment-testnet-snapshot`."
  []
  (log/debug "Saving blockchain snapshot!")
  (reset! *snapshot-lock true)
  (web3-evm/snapshot! @web3
   (fn [error result]
     (if result
       (reset! *deployment-testnet-snapshot result)
       (log/error "Failed to retrieve the blockchain snapshot!" error))
     (reset! *snapshot-lock false)))
  (.loopWhile deasync (fn [] @*snapshot-lock)))

  ;; Make our async callback synchronous.


(def *revert-lock (atom false)) ; Lock to force deasync
(defn revert-testnet!
  "Reverts the testnet blockchain to the most recent
  `*deployment-testnet-snapshot`."
  []
  (if @*deployment-testnet-snapshot
    (do
      (log/debug "Reverting Testnet Blockchain...")
      (reset! *revert-lock true)
      (web3-evm/revert!
       @web3 @*deployment-testnet-snapshot
       (fn [error result]
         (if result
           (log/debug "Successfully Reverted Testnet!")
           (log/error "Failed to Revert Testnet!" error))
         (reset! *revert-lock false))))
    (log/warn "Snapshot Not Available, Testnet will not be reverted."))

  ;; Make our async callback synchronous.
  (.loopWhile deasync (fn [] @*revert-lock)))


(defn reset-testnet!
  "Reset the testnet snapshot"
  []
  (reset! *deployment-testnet-snapshot nil))


(def test-config
  "Test configuration for districts."
  (-> ethlance.server.core/main-config
      (merge {:logging {:level "debug" :console? true}})
      (update :smart-contracts merge {:print-gas-usage? true
                                      :auto-mining? true})))


(def default-deployer-config
  "Default Configuration for Smart Contract Deployments."
  {})


(defn prepare-testnet!
  "Performs a deployment, or reverts the testnet if a deployment
  snapshot is available.
  
  Keyword Arguments:

  deployer-options - Deployment Options passed for a deployment

  force-deployment? - If true, will force a deployment, without using
  a blockchain snapshot.
  
  Note:

  - Works on Ganache CLI v6.1.8 (ganache-core: 2.2.1)"
  [deployer-options force-deployment?]
  (if-not (or @*deployment-testnet-snapshot force-deployment?)
    (do
      (deployer/deploy-all!
       (merge default-deployer-config deployer-options))
      (snapshot-testnet!))
    (revert-testnet!)))


(defn fixture-start
  "Test Fixture Setup."
  [{:keys [deployer-options force-deployment?]}]
  (-> (mount/with-args test-config)
      (mount/only
       [#'district.server.web3/web3
        #'district.server.smart-contracts/smart-contracts])
      mount/start)
  (prepare-testnet! deployer-options force-deployment?))


(defn fixture-stop
  "Test Fixture Teardown."
  []
  (mount/stop)
  (async done (js/setTimeout #(done) 1000)))


(defn with-smart-contract
  "A test fixture for performing a fresh smart contract deployment
  before the tests.
  
  Optional Arguments
  
  :deployer-options - Additional Deployment Options to provide the
  deployer."
  [& [opts]]
  (fn [f]
    (fixture-start opts)
    (f)
    (fixture-stop)))
