(ns district.server.web3-events
  (:require [cljs-web3-next.eth :as web3-eth]
            [cljs-web3-next.core :as web3-core]
            [cljs.reader :as reader]
            [district.server.config :refer [config]]
            [district.server.smart-contracts :as smart-contracts]
            [district.server.web3 :refer [web3]]
            [medley.core :as medley]
            [mount.core :as mount :refer [defstate]]
            [taoensso.timbre :as log]
            [cljs-node-io.core :as io :refer [slurp spit]]))

(declare start)
(declare stop)

(defstate ^{:on-reload :noop} web3-events
  :start (start (merge (:web3-events @config)
                       (:web3-events (mount/args))))
  :stop (stop web3-events))

(defn update-checkpoint-atom! [block-number tx log-index]
  (swap! (:checkpoint @web3-events)
         (fn [{:keys [last-processed-block processed-log-indexes] :as checkpoint-info}]
           (-> checkpoint-info
               (assoc :last-processed-block block-number
                      :processed-log-indexes (if (= block-number last-processed-block)
                                               (conj processed-log-indexes [tx log-index])
                                               #{[tx log-index]}))))))

(defn save-checkpoint-to-file [file-path new-state]
  ; For some reason when compiling with :local-deps the new-state (although
  ; being HashMap) causes error, claiming that
  ; The same doesn't occur when compiling without :local-deps and
  ; cljs-node-io.core/spit should handle clojure hashes without problems
  ; Leaving this workaround until better solution is found
  (spit file-path (str new-state)))

(defn load-checkpoint-from-file [file-path callback]
  (if file-path
    (try
      (let [{:keys [last-processed-block] :as checkpoint-info} (reader/read-string (slurp file-path))]
        (callback
          nil
          (if last-processed-block
            {:last-processed-block last-processed-block}
            checkpoint-info)))
      (catch js/Error e (do
                          (log/info "Checkpoint file not loaded")
                          (callback e nil))))
    (log/info "web3-events checkpoint file disabled")))

(defn wrap-callback-checkpoint-middleware [callback]
  (fn [err {:keys [block-number transaction-index log-index] :as ev}]
    (callback err ev)
    ;; if callback throws we don't save the checkpoint
    (when-not err
      (update-checkpoint-atom! block-number transaction-index log-index))))

(defn register-callback! [event-key callback & [callback-id]]
  (log/info ">>> register-callback" {:event-key event-key :callback-id callback-id})
  (let [[contract-key event] (if-not (= event-key ::after-past-events-dummy-key)
                               (get (:events @web3-events) event-key)
                               [::after-past-events-dummy-contract ::after-past-events-dummy-event])
        callback-id (or callback-id (str (random-uuid)))]
    (when-not contract-key
      (throw (js/Error. (str "Trying to register callback for non existing event " event-key))))

    (swap! (:callbacks @web3-events) (fn [callbacks]
                                       (-> callbacks
                                           (assoc-in [contract-key event callback-id] callback)
                                           (assoc-in [:callback-id->path callback-id] [contract-key event]))))
    callback-id))

(defn register-after-past-events-dispatched-callback! [callback]
  (register-callback! ::after-past-events-dummy-key callback))

(defn unregister-callbacks! [callback-ids]
  (doseq [callback-id callback-ids]
    (let [path (get-in @(:callbacks @web3-events) [:callback-id->path callback-id])]
      (swap! (:callbacks @web3-events) (fn [callbacks]

                                         (log/info ">>> UN-register-callback" {:path (into path [callback-id])})
                                         (-> callbacks
                                             (medley/dissoc-in (into path [callback-id]))
                                             (medley/dissoc-in [:callback-id->path callback-id]))))))
  callback-ids)

(defn dispatch [err {:keys [:contract :event] :as evt}]
  (if err
    (log/error "Error Dispatching" {:err err :event evt} ::event-dispatch)
    (when (:dispatch-logging? @web3-events)
      (log/info "Dispatching event" {:err err :event evt} ::event-dispatch)))

  (when (and err
             (fn? (:on-error @web3-events)))
    ((:on-error @web3-events) err evt))

  (when (or (not err)
            (and err (:dispatch-on-error? @web3-events)))
    (doall
      (for [callback (vals (get-in @(:callbacks @web3-events) [(:contract-key contract) event]))]
        (callback err evt)))))

(defn- start-dispatching-latest-events! [events last-block-number]
  (log/info "start-dispatching-latest-events!" {:event events :last-block-number last-block-number})
  (let [event-filters (doall (for [[contract event->callbacks] (dissoc @(:callbacks @web3-events) :callback-id->path)
                                   [event callbacks] event->callbacks]
                               (do
                                 (log/info "will subscribe:" {:contract contract :event event :from-block last-block-number})
                                 (smart-contracts/subscribe-events contract
                                                                   event
                                                                   {:from-block last-block-number
                                                                    :latest-event? true}
                                                                   (map wrap-callback-checkpoint-middleware (vals callbacks))))))]
  (log/info "Subscribed to future events" {:events (keys events)
                                          :from-block last-block-number})
  (swap! (:event-filters @web3-events) (fn [_ new] new) event-filters)))

(defn- dispatch-after-past-events-callbacks! []
  (let [callbacks (get-in @(:callbacks @web3-events)
                          [::after-past-events-dummy-contract ::after-past-events-dummy-event])
        callback-fns (vals callbacks)
        callback-ids (keys callbacks)]
    (doseq [callback callback-fns]
      (callback))
    (log/info ">>> unregistering" callback-ids)
    (unregister-callbacks! callback-ids)))

(defn start
  "When :skip-past-events-replay? is false, will obtain the last processed block
   via fn provided by load-checkpoint
   When stopped will save last processed block using save-checkpoint

   If :checkpoint-file is provided (and no function passed via <load/save>-checkpoint),
   will default to writing to the file specified by :checkpoint-file

   Arguments:
     (save-checkpoint {:last-processed-block 123
                       :processed-log-indexes [1, 2, 3]}
                      callback-fn-when-done)
     (load-checkpoint callback-fn-when-with-checkpoint-data)
  "
  [{:keys [:events
           :skip-past-events-replay?
           :from-block
           :block-step
           :checkpoint-file
           :crash-on-event-fail?
           :save-checkpoint ; Optional function to save checkpoint info (e.g. to the DB)
           :load-checkpoint ; Optional function to load checkpoint info (e.g. from the DB)
           :callback-after-past-events
           :backtrack]
    :as opts
    :or {block-step 1
         backtrack 0
         save-checkpoint (partial save-checkpoint-to-file (:checkpoint-file opts))
         load-checkpoint (partial load-checkpoint-from-file (:checkpoint-file opts))
         callback-after-past-events start-dispatching-latest-events!}}]
  (log/info "district.server.web3-events/start OPTS" opts)
  (web3-eth/connected?
    @web3
    (fn [_err-connected listening?]
      (if-not listening?
        (throw (js/Error. "Can't connect to Ethereum node"))
        (load-checkpoint
          (fn [err checkpoint]
            (web3-eth/get-block-number
              @web3
              (fn [_err-block last-block-number]
                (let [{:keys [last-processed-block processed-log-indexes]} checkpoint
                      next-block-to-process (max 0 (- last-processed-block backtrack))]
                  (if skip-past-events-replay?
                    (do
                      (dispatch-after-past-events-callbacks!)
                      (start-dispatching-latest-events! events (inc last-block-number)))
                    (smart-contracts/replay-past-events-in-order
                      events
                      dispatch
                      {:from-block (max next-block-to-process from-block 0)
                       :crash-on-event-fail? crash-on-event-fail?
                       :skip-log-indexes processed-log-indexes
                       :to-block last-block-number
                       :block-step block-step
                       :on-finish (fn []
                                    (dispatch-after-past-events-callbacks!)
                                    (log/info "Done replaying past events " {:block-number last-block-number})
                                    ;; since we are replaying all past events until current(latest)
                                    ;; it should be safe to set a checkpoint in current block number
                                    ;; in case of a restart after a full sync it should start immediately
                                    (add-watch (:checkpoint @web3-events) :file-flusher
                                               (fn [_key _ref _old-state new-state]
                                                 (save-checkpoint new-state)))
                                    (reset! (:checkpoint @web3-events) {:last-processed-block last-block-number})
                                    (callback-after-past-events events (inc last-block-number)))}))))))))))
  (merge opts {:callbacks (atom {})
               ;; Keeps the latest checkpoint (events processed so far).
               ;; Contains a map with
               ;; :last-processed-block, a block number
               ;; :processed-log-indexes, a set of tuples like tx log-index
               :checkpoint (atom nil)
               :event-filters (atom nil)}))

(defn stop [web3-events]
  (log/info "Stopping web3-events" (:events @web3-events))
  (remove-watch (:checkpoint @web3-events) :file-flusher)
  (doseq [subscription @(:event-filters @web3-events)]
    (when (web3-core/connected? @web3)
      (web3-eth/unsubscribe subscription))))
