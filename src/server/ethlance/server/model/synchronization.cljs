(ns ethlance.server.model.synchronization
  "For inserting and getting data from the :EthlanceSynchronizationLog
  table in the ethlance database."
  (:require
   [clojure.spec.alpha :as s]
   [bignumber.core :as bn]
   [cuerdas.core :as str]
   [district.server.config]
   [district.server.db :as district.db]
   [taoensso.timbre :as log]
   [ethlance.server.db :as ethlance.db]

   ;; Includes additional spec namespaces
   [ethlance.shared.spec :as espec]))


(defn log!
  "Log a synchronization event"
  [log-data]
  (ethlance.db/insert-row! :EthlanceSynchronizationLog log-data))


(defn log-event!
  "Logs an event returned by an Event Watcher with the given `status`."
  [event]
  (let [log-data
        {:sync/name (-> event :name str)
         :sync/event-name (-> event :args :event_name)
         :sync/event-version (-> event :args :event_version bn/number)
         :sync/event-data (-> event :args :event_data pr-str)
         :sync/timestamp (-> event :args :timestamp bn/number)
         :sync/transaction-hash (-> event :transactionHash)
         :sync/block-hash (-> event :blockHash)
         :sync/block-number (-> event :blockNumber)
         :sync/log-index (-> event :logIndex)}]
    (log! log-data)))


(defn get-log-listing
  "Gets all of the logging data"
  []
  (ethlance.db/get-list :EthlanceSynchronizationLog {}))
