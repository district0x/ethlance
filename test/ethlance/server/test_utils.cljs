(ns ethlance.server.test-utils
  "Includes fixtures and additional utilities to improve the process of
  testing ethereum contracts."
  (:require
   [clojure.test :refer [deftest is are testing use-fixtures]]
   [clojure.core.async :as async :refer [go go-loop <! >! chan close! put!] :include-macros true]

   [taoensso.timbre :as log]
   [cljs-web3.eth :as web3-eth]
   [cljs-web3.evm :as web3-evm]
   [mount.core :as mount :refer [defstate]]

   [ethlance.server.core]
   [ethlance.shared.smart-contracts-dev :as smart-contracts-dev]

   ;; Mount Components
   [district.server.logging]
   [district.server.web3 :refer [web3]]
   [district.server.smart-contracts :as contracts]))


;; Fixture State Module. This serves as a global set up and tear down
;; when alternative fixtures come into play.
(declare reset-testnet! revert-testnet!)
(defstate testnet-fixture
  :start (reset-testnet!))


;; The snapshot of the testnet after the first deployment. This saves
;; time in between smart contract tests by simply reverting the
;; testnet instead of performing a redeployment."
(defonce *deployment-testnet-snapshot (atom nil))


(defn snapshot-testnet!
  "Retrieves the current blockchain snapshot, and places it in
  `*deployment-testnet-snapshot`."
  []
  (go
    (let [done-channel (chan 1)]
      (log/debug "Saving Testnet Blockchain Snapshot...")
      (web3-evm/snapshot!
       @web3
       (fn [error result]
         (if result
           (do
             (reset! *deployment-testnet-snapshot (:result result))
             (log/debug "Snapshot Saved!"))
           (log/error "Failed to retrieve the blockchain snapshot!" error))
         (put! done-channel ::done)))
      (<! done-channel))))


(defn revert-testnet!
  "Reverts the testnet blockchain to the most recent
  `*deployment-testnet-snapshot`.

  Notes:

  - Can only revert to the previous snapshot 'once'."
  []
  (go
    (let [done-channel (chan 1)]
      (if @*deployment-testnet-snapshot
        (do
          (log/debug "Reverting to Testnet Blockchain Snapshot..." @*deployment-testnet-snapshot)
          (web3-evm/revert!
           @web3 @*deployment-testnet-snapshot
           (fn [error result]
             (if result
               (log/debug "Successfully Reverted Testnet!")
               (log/error "Failed to Revert Testnet!" error))
             (reset! *deployment-testnet-snapshot nil)
             (put! done-channel ::done))))
        (do
          (log/warn "Snapshot Not Available, Testnet will not be reverted.")
          (put! done-channel ::done)))
      (<! done-channel))))


(defn reset-testnet!
  "Reset the testnet snapshot"
  []
  (reset! *deployment-testnet-snapshot nil))


(def test-config
  "Test configuration for districts."
  (-> ethlance.server.core/default-config
      (merge {:logging {:level "debug" :console? true}})
      (update :smart-contracts merge {:contracts-var #'smart-contracts-dev/smart-contracts
                                      :print-gas-usage? true
                                      :auto-mining? true})))


(defn prepare-testnet!
  "Performs a deployment, or reverts the testnet if a deployment
  snapshot is available.

  Note:

  - Works on Ganache CLI v6.1.8 (ganache-core: 2.2.1)"
  []
  (go
    (if-not @*deployment-testnet-snapshot
      (<! (snapshot-testnet!))
      (do
        (<! (revert-testnet!))
        ;; Snapshot is 'used up' after reversion, so take another snapshot.
        (<! (snapshot-testnet!))))))


(defn fixture-start
  "Test Fixture Setup."
  [{:keys [deployer-options force-deployment?]}]
  (-> (mount/with-args test-config)
      (mount/only
       [#'district.server.logging/logging
        #'ethlance.server.test-utils/testnet-fixture
        #'district.server.web3/web3
        #'district.server.smart-contracts/smart-contracts])
      mount/start)
  ;; commenting this out for now, we don't have snapshoting system in new cljs-web3-next
  (go true #_(<! (prepare-testnet!))))
