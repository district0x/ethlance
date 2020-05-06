(ns ethlance.server.event-store
  (:require [mount.core :as mount :refer [defstate]]
            [taoensso.timbre :as log]
            [cljs.nodejs :as nodejs]
            [district.server.config :refer [config]]
            [cljs.reader :refer [read-string]]))

(declare start stop)

(def fs (nodejs/require "fs"))

(defstate ^{:on-reload :noop} event-store
  :start (start)
  :stop (stop))

(def event-type {:ethereum-log 0
                 :graphql-mutation 1})

(def event-type-key (->> event-type
                         (map (fn [[k v]] [v k]))
                         (into {})))

(defn save-log-event [timestamp event-t data]
  (when-let [esfs (:event-store-file-stream @event-store)]
    (let [str-ev (pr-str {:event/timestamp timestamp
                          :event/type (event-type event-t)
                          :event/body (pr-str data)})]
      (.write esfs
              (str str-ev "\n")))))

(defn save-ethereum-log-event [timestamp event-body-map]
  (save-log-event timestamp :ethereum-log event-body-map))

(defn save-graphql-mutation-event [timestamp mutation-body-map]
  (save-log-event timestamp :graphql-mutation mutation-body-map))

(defn load-replay-system-events []
  (let [store-file (-> @config :event-store :store-file)
        file-content (.readFileSync fs store-file #js {:encoding "utf8"
                                                       :flag "r"})
        file-str-edn (str "[" file-content "]")]
    (read-string file-str-edn)))

(defn start []
  (log/debug "Starting Events store...")
  (let [file-name (-> @config :event-store :store-file)]
    {:event-store-file-stream (when file-name (.createWriteStream fs file-name #js {:flags "a"}))})
  )

(defn stop []
  (log/debug "Stopping Events store...")

  )
