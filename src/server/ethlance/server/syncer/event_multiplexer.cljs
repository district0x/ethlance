(ns ethlance.server.syncer.event-multiplexer
  "Used to multiplex event watchers and sort them into their [block
  number / log index] order to be consumed in an in-order fashion."
  (:require
   [clojure.core.async :as async :refer [go go-loop <! >! chan put! close! poll! timeout] :include-macros true]
   [ethlance.shared.async-utils :refer [<!-<log <!-<throw flush!] :include-macros true]
   [cuerdas.core :as str]
   [mount.core :as mount :refer [defstate]]
   [ethlance.server.syncer.event-watcher :as event-watcher]
   [ethlance.server.contract.ethlance-registry :as registry]
   [taoensso.timbre :as log]))


(defn create-watcher [f x]
  (event-watcher/create-event-watcher (f (or x {}) {:from-block 0 :to-block "latest"})))


(def event-watchers
  {:registry-event [registry/ethlance-event]})


(defonce *active-watchers (atom {}))
(defonce *conveyer-belt (atom {}))


(defn move-conveyer!
  "Polls event watchers, and returns the next event from the list of
  filters."
  []
  (let [result-channel (chan 1)]
    (go
      ;; Polls result channels that do not currently have a value and
      ;; places it on the conveyer belt.
      (doseq [[name [result-channel _]] @*active-watchers]
        (when-not (get @*conveyer-belt name)
          (swap! *conveyer-belt assoc name (poll! result-channel))))

      ;; Sorts the values on the conveyer belt by blockNumber
      ;; TODO: also sort by logIndex
      (let [[name value]
            (->> @*conveyer-belt
                 (into [])
                 (remove #(-> % second nil?))
                 (sort-by #(-> % second :blockNumber))
                 first)]
        
        ;; Removes the result from the conveyer that we chose to be
        ;; replaced with the next result from that particular watcher.
        (swap! *conveyer-belt dissoc name)

        ;; Channel is closed if there are no results
        (if value
          (do (<! (timeout 10)) ;; Small time buffer to keep things in sync
              (put! result-channel (assoc value :name name)))
          (do (<! (timeout 1000)) ;; Wait a second before continuing
              (close! result-channel)))))
    result-channel))


(defn start
  []
  
  ;; Place active watchers
  (doseq [[name [f x]] event-watchers]
    (swap! *active-watchers assoc name (create-watcher f x)))

  ;; Start serving results from the conveyer.
  (let [result-channel (chan 1)
        *finished? (atom false)]
    (go-loop [result (<! (move-conveyer!))]
      (when (not @*finished?)
        (when result (>! result-channel result))
        (recur (<! (move-conveyer!))))
      (close! result-channel)
      (log/debug "Sync Muxer has Stopped!"))
    [result-channel *finished?]))


(defn stop
  [[result-channel *finished?]]
  (log/debug "Stopping Sync Muxer...")

  ;; Stop all active watchers
  (doseq [[name [r-channel stop-channel]] @*active-watchers]
    (put! stop-channel ::finished)
    (flush! r-channel)
    (close! r-channel))

  ;; Close the result channel
  (close! result-channel)
  (flush! result-channel)
  (reset! *finished? true)

  ;; Flush out atoms
  (reset! *active-watchers {})
  (reset! *conveyer-belt {}))
