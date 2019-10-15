(ns ethlance.server.syncer
  "Service that syncs the ethlance in-memory database with the ethereum
  blockchain by reading events emitted by the ethlance smart contracts."
  (:require
   [mount.core :as mount :refer [defstate]]
   [taoensso.timbre :as log]

   ;; Mount Components
   [district.server.web3-events :refer [register-callback! unregister-callbacks!]]
   [ethlance.server.syncer.processor :as processor]))


(declare start stop)
(defstate ^{:on-reload :noop} syncer
  :start (start)
  :stop (stop))


(defn handle-event-test [_ {:keys [args] :as event}]
  (log/info (str "Event Handled " (pr-str event))))


(defn start []
  (log/debug "Starting Syncer...")
  (register-callback! :ethlance-registry/ethlance-event handle-event-test ::EthlanceEvent))


(defn stop
  "Stop the syncer mount component."
  []
  (log/debug "Stopping Syncer...")
  (unregister-callbacks!
   [::EthlanceEvent]))
