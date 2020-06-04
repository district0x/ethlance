(ns ethlance.server.event-replay-queue
  (:require [ethlance.server.db :as ethlance-db]
            [district.server.db :as db]
            [cljs.reader :refer [read-string]]
            [honeysql.core :as sql]))

(def serialize-event pr-str)
(def deserialize-event read-string)

(defn push-event
  "Push a event into the events priority queue"
  [evt]

  (let [comparable-id (js/parseInt (str (:block-number evt) (:transaction-index evt) (:log-index evt)))]
    (ethlance-db/insert-row! :ReplayEventQueue
                             {:event/comparable-id comparable-id
                              :event/string (serialize-event evt)})))

(defn oldest-event-id []
  (-> (db/get {:select [[(sql/call :min :event/comparable-id) :oldest]]
               :from [:ReplayEventQueue]})
      :oldest))

(defn get-event [event-id]
  (-> (db/get {:select [:event/string]
               :from [:ReplayEventQueue]
               :where [:= :event/comparable-id event-id]})
      :event/string
      deserialize-event))

(defn peek-event
  "Returns the event with highest priority from the priority events queue.
  Returns nil is the queue is empty"
  []
  (get-event (oldest-event-id)))

(defn pop-event
  "Returns and remove the event with highest priority from the priority events queue.
  Returns nil is the queue is empty"
  []
  (let [ev-id (oldest-event-id)
        evt (get-event ev-id)]
    (db/run! {:delete-from :ReplayEventQueue
              :where [:= :event/comparable-id ev-id]})
    evt))
