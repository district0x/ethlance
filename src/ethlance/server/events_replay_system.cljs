(ns ethlance.server.events-replay-system
  (:require [district.server.web3-events :as web3-events]
            [mount.core :as mount :refer [defstate]]
            [taoensso.timbre :as log]
            [cljs.nodejs :as nodejs]
            [district.shared.async-helpers :as async-helpers]

            ;; it needs to depend syncer and graphql server so
            ;; we know those are already started when we run our start
            [ethlance.server.syncer]
            [ethlance.server.graphql.server]

            [ethlance.server.db :as ethlance-db]
            [cljs.reader :refer [read-string]]
            [ethlance.server.event-store :as event-store]
            [district.shared.async-helpers :refer [safe-go <?]]))

(declare start stop)

(def axios (nodejs/require "axios"))

(defstate ^{:on-reload :noop} events-replay-system
  :start (start)
  :stop (stop))

(defn start []
  (log/debug "Starting Events replay system...")
  (safe-go
   (let [all-events (event-store/load-replay-system-events)]
     (log/info "[ERS] about to relpalay " {:events-count (count all-events)})
     (doseq [e all-events]
       (case (event-store/event-type-key (:event/type e))
         :ethereum-log (let [evt (read-string (:event/body e))]
                         (doseq [res (web3-events/dispatch nil (assoc evt :replay true) )]
                           (when res
                             (cond
                               (satisfies? cljs.core.async.impl.protocols/ReadPort res)
                               (<! res)

                               (async-helpers/promise? res)
                               (<! (async-helpers/promise->chan res))))))
         :graphql-mutation (let [{:keys [headers body]} (read-string (:event/body e))]
                             (<? (axios (clj->js {:method "post"
                                                  ;; TODO: take this from config
                                                  :url "http://localhost:4000/graphql"
                                                  :headers (assoc headers
                                                                  :replay true
                                                                  :timestamp (:event/timestamp e))
                                                  :data body})))))))))

(defn stop []
  (log/debug "Stopping Events replay system...")

  )
