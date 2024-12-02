(ns ethlance.server.syncer
  (:require
    [bignumber.core :as bn]
    [camel-snake-kebab.core :as camel-snake-kebab]
    [cljs.core.async.impl.protocols :refer [ReadPort]]
    [clojure.core.async :as async :refer [<!] :include-macros true]
    [district.server.async-db :as db]
    [district.server.smart-contracts :as smart-contracts]
    [district.server.web3-events :as web3-events]
    [district.shared.async-helpers :refer [<? safe-go]]
    [ethlance.server.event-replay-queue :as replay-queue]
    [ethlance.server.tracing.api :as t-api]
    [ethlance.server.syncer.handlers :as handlers]
    [mount.core :as mount :refer [defstate]]
    [taoensso.timbre :as log]))


(declare start stop)


(defstate ^{:on-reload :noop} syncer
  :start (start)
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
                  _ (db/begin-tx conn)
                  res (t-api/with-span-context span #(handler conn err event))
                  _ (db/commit-tx conn)]
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
            (catch js/Error error
              (log/error "Syncer: ERROR" error)
              (replay-queue/push-event conn event)
              (t-api/set-span-error! span error)
              (t-api/end-span! span)
              (db/rollback-tx conn)
              (throw error))
            (finally
              (db/release-connection conn))))))))


(defn start
  []
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
    (log/debug "Syncer started")
    {:callback-ids callback-ids
     :dispatcher dispatcher}))


(defn stop
  "Stop the syncer mount component."
  []
  (log/debug "Stopping Syncer...")
  #_(unregister-callbacks!
     [::EthlanceEvent]))
