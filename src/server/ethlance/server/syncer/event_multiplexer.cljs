(ns ethlance.server.syncer.event-multiplexer
  "Used to multiplex event watchers and sort them into their [block
  number / log index] order to be consumed in an in-order fashion."
  (:require
   [clojure.core.async :as async :refer [go go-loop <! >! chan put! close! poll!] :include-macros true]
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


(def *active-watchers (atom {}))
(def *conveyer-belt (atom {}))


(declare start stop)
(defstate syncer-muxer
  :start (start)
  :stop (stop))


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
        (swap! *conveyer-belt dissoc name)
        (if value
          (do
            (put! result-channel (assoc value :name name))
            (log/debug (pr-str "Received Event!" value)))
          (close! result-channel))))
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
      (if (and (not @*finished?))
        (do
          (when result (>! result-channel result))
          (recur (<! (move-conveyer!))))
        (close! result-channel)))
    [result-channel *finished?]))


(defn stop
  []
  (let [[result-channel *finished?] @syncer-muxer]

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
    (reset! *conveyer-belt {})))
