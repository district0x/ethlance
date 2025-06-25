(ns ethlance.server.syncer
  (:require
    [bignumber.core :as bn]
    [camel-snake-kebab.core :as camel-snake-kebab]
    [cljs.core.async.impl.protocols :refer [ReadPort]]
    [cljs-web3-next.eth :as web3-eth]
    [cljs-web3-next.core :as web3-core]
    [clojure.core.async :as async :refer [<!] :include-macros true]
    [district.server.async-db :as db]
    [district.server.smart-contracts :as smart-contracts]
    [district.time :as time]
    [district.server.config :refer [config]]
    [district.server.web3 :refer [ping-start ping-stop web3]]
    [district.server.web3-events :as web3-events]
    [district.shared.async-helpers :refer [<? safe-go]]
    [ethlance.server.db :as ethlance-db]
    [ethlance.server.event-replay-queue :as replay-queue]
    [ethlance.server.tracing.api :as t-api]
    [ethlance.server.syncer.handlers :as handlers]
    [ethlance.shared.utils :as shared-utils]
    [mount.core :as mount :refer [defstate]]
    [taoensso.timbre :as log]))


(declare start stop)


(defstate ^{:on-reload :noop} syncer
  :start (start (merge (:syncer @config)
                       (:syncer (mount/args))))
  :stop (stop))


;;
;; Syncer Start ;;
;;

(defn- block-timestamp*
  [block-number]
  (let [out-ch (async/promise-chan)]
    (smart-contracts/wait-for-block block-number (fn [error result]
                                                   (if error
                                                     (async/put! out-ch error)
                                                     (let [{:keys [:timestamp]} (js->clj result :keywordize-keys true)]
                                                       (log/debug "cache miss for block-timestamp" {:block-number block-number
                                                                                                    :timestamp timestamp})
                                                       (async/put! out-ch timestamp)))))
    out-ch))


(def block-timestamp
  (memoize block-timestamp*))


(defn- build-dispatcher
  "Dispatcher is a function you can call with a event map and it will process it with syncer."
  [web3-events-map events-callbacks]
  (let [contract-ev->handler (reduce (fn [r [ev-ns-key [contract-key ev-key]]]
                                       (assoc r [contract-key ev-key] (get events-callbacks ev-ns-key)))
                                     {}
                                     web3-events-map)]
    (fn [err {:keys [:block-number] :as event}]
      (async/go
        (let [contract-key (-> event :contract :contract-key)
              event-key (-> event :event)
              event-name (name event-key)
              log-index (-> event :log-index)
              handler (get contract-ev->handler [contract-key event-key])
              span (t-api/start-span (str (name (or contract-key "UnnamedContract")) "." (name (or event-key "UnnamedEvent"))))
              conn (<? (db/get-connection))]
          (try
            (let [block-timestamp (<? (block-timestamp block-number))
                  event (-> event
                            (update ,,, :event camel-snake-kebab/->kebab-case)
                            (update-in ,,, [:args :version] bn/number)
                            (update-in ,,, [:args :timestamp] (fn [timestamp]
                                                                (if timestamp
                                                                  (bn/number timestamp)
                                                                  block-timestamp))))
                  {:keys [:event/last-block-number :event/last-log-index :event/count]
                   :or {last-block-number -1
                        last-log-index -1
                        count 0}} (<? (ethlance-db/get-last-event conn (name contract-key) event-name))]
              (log/debug "Handling event..." event)
              (if (or (> block-number last-block-number)
                      (and (= block-number last-block-number) (> log-index last-log-index)))
                (let [_ (db/begin-tx conn)
                      res (t-api/with-span-context span #(handler conn err event))
                      _ (<? (ethlance-db/upsert-event! conn {:event/last-log-index log-index
                                                             :event/last-block-number block-number
                                                             :event/count (inc count)
                                                             :event/event-name event-name
                                                             :event/contract-key (name contract-key)}))
                      _ (db/commit-tx conn)]
                  (log/info "Handled new event" event)
                  (t-api/set-span-ok! span)
                  ;; Calling a handler can throw or return a go block (when using safe-go)
                  ;; in the case of async ones, the go block will return the js/Error.
                  ;; In either cases push the event to the queue, so it can be replayed later
                  (when (satisfies? ReadPort res)
                    (let [r (<! res)]
                      (when (instance? js/Error r)
                        (throw r))
                      (t-api/set-span-ok! span)
                      (t-api/end-span! span)
                      (log/info "Syncer: OK" r)
                      r)))
                (log/info "Skipping handling of a persisted event" event)))
            (catch js/Error error
              (log/error "Syncer: ERROR" error)
              (replay-queue/push-event conn event)
              (t-api/set-span-error! span error)
              (t-api/end-span! span)
              (db/rollback-tx conn)
              (throw error))
            (finally
              (db/release-connection conn))))))))


(defn- reload-handler [interval]
  (js/setInterval
    (fn []
      (safe-go
        (let [connected? (true? (<! (web3-eth/is-listening? @web3)))]
          (when connected?
            (do
              (log/debug (str "disconnecting from provider to force reload"))
              (web3-core/disconnect @web3))))))
    interval))

(defonce reload-timeout (atom nil))

(defn reload-timeout-start [{:keys [:reload-interval]}]
  (reset! reload-timeout (reload-handler reload-interval)))

(defn reload-timeout-stop []
  (js/clearInterval @reload-timeout))

(defn start
  [opts]
  (log/debug "Starting Syncer...")
  (let [event-callbacks {:ethlance/job-created handlers/handle-job-created
                         :ethlance/candidate-added handlers/handle-candidate-added
                         :ethlance/test-event handlers/handle-test-event
                         :ethlance/invoice-created handlers/handle-invoice-created
                         :ethlance/invoice-paid handlers/handle-invoice-paid
                         :ethlance/dispute-raised handlers/handle-dispute-raised
                         :ethlance/dispute-resolved handlers/handle-dispute-resolved
                         :ethlance/quote-for-arbitration-set handlers/handle-quote-for-arbitration-set
                         :ethlance/quote-for-arbitration-accepted handlers/handle-quote-for-arbitration-accepted
                         :ethlance/job-ended handlers/handle-job-ended
                         :ethlance/arbiters-invited handlers/handle-arbiters-invited
                         :ethlance/funds-in (partial handlers/handle-job-funds-change +)
                         :ethlance/funds-out (partial handlers/handle-job-funds-change -)}
        dispatcher (build-dispatcher (:events @district.server.web3-events/web3-events) event-callbacks)
        callback-ids (doall (for [[event-key] event-callbacks]
                              (web3-events/register-callback! event-key dispatcher)))]
        (web3-events/register-after-past-events-dispatched-callback!
          (fn []
            (log/warn "Syncing past events finished" (time/time-units (- (shared-utils/now) start-time)) ::start)
            (ping-start {:ping-interval 10000})
            (when (> (:reload-interval opts) 0)
              (reload-timeout-start (select-keys opts [:reload-interval])))))
    (log/debug "Syncer started")
    (merge opts
           {:callback-ids callback-ids
            :dispatcher dispatcher})))


(defn stop
  "Stop the syncer mount component."
  []
  (log/debug "Stopping Syncer...")
  (reload-timeout-stop)
  (ping-stop)
  (web3-events/unregister-callbacks! (:callback-ids @syncer)))
