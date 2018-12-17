(ns ethlance.server.syncer.event-watcher
  "Event watching functionality."
  (:require
   [bignumber.core :as bn]
   [cljs-web3.core :as web3]
   [cljs-web3.eth :as web3-eth]
   [clojure.core.async :as async :refer [go go-loop <! >! chan close! put! poll!] :include-macros true]
   [district.server.web3 :refer [web3]]
   [district.shared.error-handling :refer [try-catch try-catch-throw]]
   [district.server.smart-contracts :as contracts]
   [mount.core :as mount :refer [defstate]]
   [taoensso.timbre :as log]))


(defn get-events
  "Gets the event logs from the `event-filter`, results are placed on the `result-channel`.

  # Return Value

  Returns a channel which contains the resulting logs. Closes the
  channel when all results have been obtained."
  [event-filter]
  (let [result-channel (chan 10)]
    (.get event-filter
          (fn [error logs]
            (when error
              (log/error error)
              (throw (ex-info "Failed to get past eth event logs" {:error-object error})))
            (go
              (doseq [log logs]
                (>! result-channel (js->clj log :keywordize-keys true)))
              (close! result-channel))))
    result-channel))


(defn watch-events
  "Watches the event logs and places the results on the
  `result-channel`.

  # Return Value

  The `result-channel`, which contains the most recent events retrieved on the blockchain."
  [event-filter]
  (let [result-channel (chan 1)]
    (.watch event-filter
            (fn [error log]
              (when error
                (log/error error)
                (throw (ex-info "Failed to get watched eth event logs" {:error-object error})))
              (put! result-channel (js->clj log :keywordize-keys true))))
    result-channel))


(defn stop-watching!
  [event-filter]
  (web3-eth/stop-watching! event-filter (fn [err] (when err (log/error err)))))


(defn create-event-watcher
  [event-filter]
  (let [event-channel (chan 1)
        stop-channel (chan 1)
        past-events-channel (get-events event-filter)
        watched-events-channel (watch-events event-filter)
        finished? (atom false)]
    (go
      ;; (log/debug "Starting Event Watcher...")
      
      ;; First, retrieve all of the previous events on the blockchain
      ;; (log/debug " - Retrieving Past Events...")
      ;; (loop [event (<! past-events-channel)]
      ;;   (when (and (not @finished?) event)
      ;;     (>! event-channel event)
      ;;     ;; Check to see if the user wants the event watcher stopped.
      ;;     (when (poll! stop-channel)
      ;;       (reset! finished? true)
      ;;       (close! past-events-channel))
      ;;     (recur (<! past-events-channel))))
      
      ;; Clean up past event watcher.
      ;; (log/debug " - Finished Retrieving Past Events!")

      ;; After processing past events, process current events through a watcher.
      (log/debug " - Watching Current Events...")
      (while (not @finished?)
        (when-let [event (<! watched-events-channel)]
          (>! event-channel event)
          
          ;; Check to see if the user wants the event watcher stopped.
          (when (poll! stop-channel)
            (reset! finished? true)
            (close! watched-events-channel))))

      ;; Clean up.
      (log/debug "Stopping Event Watcher!")
      (close! event-channel)
      (stop-watching! event-filter))
    [event-channel stop-channel]))
