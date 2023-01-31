(ns ethlance.server.syncer.processor
  "Processes events that have been passed through the "
  (:require [clojure.core.async :as async :refer [<! go] :include-macros true]
            [clojure.pprint :refer [pprint]]
            [cuerdas.core :as str]
            [taoensso.timbre :as log]))

(declare process-registry-event)

(defn pp-str [x]
  (with-out-str (pprint x)))

(defmulti process-event
  "Process an emitted event based on the `event-multiplexer/event-watchers` key.

  # Notes

  - Implementations are expected to return a single value async channel."
  (fn [_ event] [(-> event :contract :contract-key) (-> event :event)]))

(defmethod process-event :default
  [_ {:keys [contract] :as event}]
  (go (log/warn (str/format "Unprocessed Event: %s\n%s" (pr-str (:contract-key contract)) (pp-str event)))))

(defmethod process-event [:ethlance-registry :EthlanceEvent]
  [_ event]
  (log/debug "Processing Ethlance Event")
  (log/debug event)
  (go (<! (process-registry-event event))))

(defmulti process-registry-event
  "Process a :registry-event. Each registry event has a unique :event_name

  # Notes

  - Similar to `process-event`, implementations must return a channel
  which places a value on completion.

  - The event name is a kebab-cased keyword from the
  original :event_name

    ex. UserRegistered --> :user-registered
  "
  (fn [{:keys [args]}] (-> args :event_name str/keyword)))

(defmethod process-registry-event :default
  [{:keys [args] :as event}]
  (go (log/warn (str/format "Unprocessed Registry Event: %s\n%s"
                            (-> args :event_name str/keyword pr-str)
                            (pp-str event)))))
