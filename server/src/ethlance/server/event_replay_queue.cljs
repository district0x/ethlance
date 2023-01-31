(ns ethlance.server.event-replay-queue
  (:require [ethlance.server.db :as ethlance-db]
            [district.server.async-db :as db]
            [cljs.reader :refer [read-string]]
            [honeysql.core :as sql]
            [district.shared.async-helpers :refer [safe-go <?]]))

(def serialize-event pr-str)
(def deserialize-event read-string)

(defn push-event
  "Push a event into the events priority queue"
  [conn evt]
  (safe-go
   (let [comparable-id (js/parseInt (str (:block-number evt) (:transaction-index evt) (:log-index evt)))]
     (<? (ethlance-db/insert-row! conn :ReplayEventQueue
                                  {:event/comparable-id comparable-id
                                   :event/string (serialize-event evt)})))))

(defn oldest-event-id [conn]
  (safe-go
   (-> (<? (db/get conn {:select [[(sql/call :min :event/comparable-id) :oldest]]
                         :from [:ReplayEventQueue]}))
       :oldest)))

(defn get-event [conn event-id]
  (safe-go
   (-> (<? (db/get conn {:select [:event/string]
                         :from [:ReplayEventQueue]
                         :where [:= :event/comparable-id event-id]}))
       :event/string
       deserialize-event)))

(defn peek-event
  "Returns the event with highest priority from the priority events queue.
  Returns nil is the queue is empty"
  [conn]
  (safe-go
   (<? (get-event conn (oldest-event-id conn)))))

(defn pop-event
  "Returns and remove the event with highest priority from the priority events queue.
  Returns nil is the queue is empty"
  [conn]
  (safe-go
   (let [ev-id (oldest-event-id conn)
         evt (get-event conn ev-id)]
     (<? (db/run! conn {:delete-from :ReplayEventQueue
                        :where [:= :event/comparable-id ev-id]}))
     evt)))
