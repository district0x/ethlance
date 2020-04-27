(ns ethlance.server.events-replay-system
  (:require [district.server.web3-events :as web3-events]
            [mount.core :as mount :refer [defstate]]
            [taoensso.timbre :as log]
            [cljs.nodejs :as nodejs]

            ;; it needs to depend syncer and graphql server so
            ;; we know those are already started when we run our start
            [ethlance.server.syncer]
            [ethlance.server.graphql.server]

            [ethlance.server.db :as ethlance-db]
            [cljs.reader :refer [read-string]]))

(declare start stop)

(def axios (nodejs/require "axios"))

(defstate ^{:on-reload :noop} events-replay-system
  :start (start)
  :stop (stop))

(defn start []
  (log/debug "Starting Events replay system...")
  (let [all-events (ethlance-db/load-replay-system-events)]
    (log/info "[ERS] about to relpalay " {:events-count (count all-events)})
    (doseq [e all-events]
      ;; TODO: synchronize this, web3-events/dispatch and axios returns a promise
      ;; so don't continue until the promise is resolved
      (case (ethlance-db/event-type-key (:event/type e))
        :ethereum-log (let [evt (read-string (:event/body e))]
                        (web3-events/dispatch nil (assoc evt :replay true) ))
        :graphql-mutation (let [{:keys [headers body]} (read-string (:event/body e))]
                            (.then (axios (clj->js {:method "post"
                                                    :url "http://localhost:4000/graphql"
                                                    :headers (assoc headers :replay true)
                                                    :data body}))
                                   (fn [response]
                                     (log/info "Responded with " {:response response}))))))))

(defn stop []
  (log/debug "Stopping Events replay system...")

  )
