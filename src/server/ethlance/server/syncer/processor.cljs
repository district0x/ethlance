(ns ethlance.server.syncer.processor
  "Processes events that have been passed through the "
  (:require
   [cuerdas.core :as str]
   [clojure.pprint :refer [pprint]]
   [bignumber.core :as bn]
   [ethlance.server.db :as db]
   [taoensso.timbre :as log]
   [district.server.config :refer [config]]
   [district.shared.error-handling :refer [try-catch]]
   [district.server.web3 :refer [web3]]
   [clojure.core.async :as async :refer [go go-loop <! >! chan] :include-macros true]

   ;; Ethlance Models
   [ethlance.server.model.job :as job]
   [ethlance.server.model.user :as user]
   [ethlance.server.model.arbiter :as arbiter]
   [ethlance.server.model.candidate :as candidate]
   [ethlance.server.model.employer :as employer]))


(defn pp-str [x]
  (with-out-str (pprint x)))


(defmulti process-event
  "Process an emitted event based on the `event-multiplexer/event-watchers` key.

  # Notes

  - Implementations are expected to return a single value async channel."
  :name)


(defmethod process-event :default
  [{:keys [name args] :as event}]
  (go (log/warn (str/format "Unprocessed Event: %s\n%s" (pr-str name) (pp-str event)))))


(declare process-registry-event)
(defmethod process-event :registry-event
  [event]
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
  [{:keys [args]}]
  (go (log/warn (str/format "Unprocessed Registry Event: %s\n%s"
                            (-> args :event_name str/keyword pr-str)
                            (pp-str args)))))


(defmethod process-registry-event :user-registered
  [{:keys [args]}]
  (go (log/debug "Processing User Registered!")))
