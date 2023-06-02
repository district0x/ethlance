(ns ethlance.server.syncer
  (:require [bignumber.core :as bn]
            [camel-snake-kebab.core :as camel-snake-kebab]
            [clojure.core.async :as async :refer [<!] :include-macros true]
            [cljs.core.async.impl.protocols :refer [ReadPort]]
            [district.server.async-db :as db]
            [district.server.smart-contracts :as smart-contracts]
            [district.server.web3-events :as web3-events]
            [district.shared.async-helpers :refer [<? safe-go]]
            [ethlance.server.db :as ethlance-db]
            [ethlance.server.event-replay-queue :as replay-queue]
            [ethlance.server.ipfs :as ipfs]
            [ethlance.server.utils :as server-utils]
            [ethlance.shared.utils :as shared-utils]
            [ethlance.shared.contract-constants :refer [offered-vec->flat-map
                                                        enum-val->token-type]]
            [ethlance.shared.token-utils :as token-utils]
            [mount.core :as mount :refer [defstate]]
            [taoensso.timbre :as log]))

(declare start stop)

(defstate ^{:on-reload :noop} syncer
  :start (start)
  :stop (stop))

(defn get-timestamp
  ([] (get-timestamp {}))
  ([event] (.now js/Date)))

(defn build-ethlance-job-data-from-ipfs-object [ethlance-job-data]
  {:job/title (:job/title ethlance-job-data)
   :job/description (:job/description ethlance-job-data)
   :job/category (:job/category ethlance-job-data)
   :job/required-skills (:job/required-skills ethlance-job-data)
   :job/required-experience-level (:job/required-experience-level ethlance-job-data)
   :job/language-id nil; TODO: where does it come from
   :job/bid-option (:job/bid-option ethlance-job-data)
   :job/estimated-project-length (:job/estimated-project-length ethlance-job-data)
   :job/invitation-only? nil ; TODO: where does it come from
   :job/required-availability (:job/required-availability ethlance-job-data)
   })

(defn ensure-db-token-details [token-type token-address conn]
  (safe-go
    (let [eth-token-details {:address "0x0000000000000000000000000000000000000000"
                             :name "Ether"
                             :symbol "ETH"
                             :abi []}]
      (if (not (<? (ethlance-db/get-token conn token-address)))
        (if (= :eth token-type)
          (ethlance-db/store-token-details conn eth-token-details)
          (ethlance-db/store-token-details conn (<! (token-utils/get-token-details token-address))))))))

(defn handle-job-created [conn _ {:keys [args] :as event}]
  (safe-go
   (log/info (str ">>> Handling event job-created" args))
   (println ">>> ipfs-data | type ipfs-data" {:ipfs-data (:ipfs-data args) :event event})
   (let [ipfs-hash (shared-utils/hex->base58 (:ipfs-data args))
         ipfs-job-content (<? (server-utils/get-ipfs-meta @ipfs/ipfs ipfs-hash))
         offered-value (offered-vec->flat-map (first (:offered-values args)))
         token-address (:token-address offered-value)
         token-type (enum-val->token-type (:token-type offered-value))
         for-the-db (merge {:job/id (:job args)
                                       :job/status  "active" ;; draft -> active -> finished hiring -> closed
                                       :job/creator (:creator args)
                                       :job/date-created (:timestamp event)
                                       :job/date-updated (:timestamp event)

                                       :job/token-type token-type
                                       :job/token-amount (:token-amount offered-value)
                                       :job/token-address token-address
                                       :job/token-id (:token-id offered-value)
                                       :invited-arbiters (get-in args [:invited-arbiters] [])}
                                      (build-ethlance-job-data-from-ipfs-object ipfs-job-content))]
     (<? (ethlance-db/add-job conn
                              (merge {:job/id (:job args)
                                      :job/status  "active" ;; draft -> active -> finished hiring -> closed
                                      :job/creator (:creator args)
                                      :job/date-created (get-timestamp event)
                                      :job/date-updated (get-timestamp event)

                                      :job/token-type token-type
                                      :job/token-amount (:token-amount offered-value)
                                      :job/token-address token-address
                                      :job/token-id (:token-id offered-value)
                                      :invited-arbiters (get-in args [:invited-arbiters] [])}
                                     (build-ethlance-job-data-from-ipfs-object ipfs-job-content))))
     (ensure-db-token-details token-type token-address conn))))

(defn handle-invoice-created [conn _ {:keys [args]}]
  (safe-go
   (log/info "Handling event handle-invoice-created")
   (let [ipfs-data (<? (server-utils/get-ipfs-meta @ipfs/ipfs (shared-utils/hex->base58 (:ipfs-data args))))
         job-story-id (:job-story/id ipfs-data)
         invoicer (:invoicer args)
         offered-value (offered-vec->flat-map (first (:invoiced-value args)))
         invoice-message {:job-story/id job-story-id
                          :message/type :job-story-message
                          :job-story-message/type :invoice
                          :message/text (:message/text ipfs-data)
                          :message/creator invoicer
                          :message/date-created (get-timestamp)
                          :invoice/status "created"
                          :invoice/amount-requested (:token-amount offered-value)
                          :invoice/ref-id (:invoice-id args)}]
     (<? (ethlance-db/add-message conn invoice-message)))))

(defn handle-dispute-raised [conn _ {:keys [args] :as dispute-raised-event}]
  (safe-go
   (log/info "Handling event dispute-raised")
   (println ">>> handle-dispute-raised" {:args args :dispute-raised-event dispute-raised-event})
   (let [ipfs-data (<? (server-utils/get-ipfs-meta @ipfs/ipfs (shared-utils/hex->base58 (:ipfs-data args))))
         job-id (:job args)
         invoice-id (:invoice-id args)
         job-story (<? (db/get conn {:select [:*]
                                     :from [:JobStoryInvoiceMessage]
                                     :join [:JobStory [:= :JobStory.job/id job-id]]
                                     :where [:= :JobStoryInvoiceMessage.invoice/ref-id invoice-id]}))
         _ (println ">>> handle-dispute-raised job-story" job-story)
         dispute-message {:job-story/id (:job-story/id job-story)
                          :message/type :job-story-message
                          :job-story-message/type :raise-dispute
                          :invoice/id invoice-id
                          :message/text (:message/text ipfs-data)
                          :message/creator (:message/creator ipfs-data)
                          :message/date-created (.now js/Date)}]
      (<? (ethlance-db/add-message conn dispute-message)))))

(defn handle-dispute-resolved [conn _ {:keys [args] :as dispute-resolved-event}]
  (safe-go
   (log/info "Handling event dispute-resolved")
   (let [ipfs-data (<? (server-utils/get-ipfs-meta @ipfs/ipfs (shared-utils/hex->base58 (:ipfs-data args))))
         ; job-id (:job args) ; FIXME: after re-deploying the contracts can use this added event field to get the job contract address (instead of relying on IPFS)
         job-id (:job/id ipfs-data)
         invoice-id (:invoice-id args)
         offered-value (offered-vec->flat-map (get-in args [:_value-for-invoicer 0]))
         job-story (<? (db/get conn {:select [:*]
                                     :from [:JobStoryInvoiceMessage]
                                     :join [:JobStory [:= :JobStory.job/id job-id]]
                                     :where [:= :JobStoryInvoiceMessage.invoice/ref-id invoice-id]}))

         resolution-message {:job-story/id (:job-story/id job-story)
                             :message/type :job-story-message
                             :job-story-message/type :resolve-dispute
                             :invoice/id invoice-id
                             :invoice/amount-paid (:token-amount offered-value)
                             :invoice/date-paid (get-timestamp)
                             :message/text (:message/text ipfs-data)
                             :message/creator (:message/creator ipfs-data)
                             :message/date-created (get-timestamp)}]
      (<? (ethlance-db/add-message conn resolution-message)))))

(defn handle-candidate-added [conn _ {:keys [args] :as event}]
  (safe-go
   (log/info "Handling event candidate-added")
   (let [ipfs-data (<? (server-utils/get-ipfs-meta @ipfs/ipfs (shared-utils/hex->base58 (:ipfs-data args))))
         job-id (:job args)
         candidate-id (:candidate args)
         job-story (<? (db/get conn {:select [:JobStory.job-story/id]
                                     :from [:JobStory]
                                     :where [:and
                                             [:= :JobStory.job-story/candidate candidate-id]
                                             [:= :JobStory.job/id job-id]
                                             [:!= :JobStory.job-story/status "completed"]]
                                     :order-by [[:job-story/date-created :desc]]}))
         job-story-message-type (:job-story-message/type ipfs-data)
         message {:job-story/id (:job-story/id ipfs-data)
                  :job/id job-id
                  :message/type :job-story-message
                  :job-story-message/type job-story-message-type
                  :message/text (:text ipfs-data)
                  :message/creator (:message/creator ipfs-data)
                  :message/date-created (get-timestamp)}]
     (<? (ethlance-db/add-message conn message)))))

(defn handle-test-event [& args]
  (println ">>> HANDLE TEST EVENT args: " args))

;;;;;;;;;;;;;;;;;;
;; Syncer Start ;;
;;;;;;;;;;;;;;;;;;

(defn- block-timestamp* [block-number]
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
      (safe-go
       (let [contract-key (-> event :contract :contract-key)
             event-key (-> event :event)
             handler (get contract-ev->handler [contract-key event-key])
             conn (<? (db/get-connection))]
         (try
           (let [block-timestamp (<? (block-timestamp block-number))
                 event (-> event
                           (update :event camel-snake-kebab/->kebab-case)
                           (update-in [:args :version] bn/number)
                           (update-in [:args :timestamp] (fn [timestamp]
                                                           (if timestamp
                                                             (bn/number timestamp)
                                                             block-timestamp))))
                 _ (db/begin-tx conn)
                 res (handler conn err event)
                 _ (db/commit-tx conn)
                 ]
             ;; Calling a handler can throw or return a go block (when using safe-go)
             ;; in the case of async ones, the go block will return the js/Error.
             ;; In either cases push the event to the queue, so it can be replayed later
             (when (satisfies? ReadPort res)
               (let [r (<! res)]
                 (when (instance? js/Error r)
                   (throw r))))
             res)
           (catch js/Error error
             (replay-queue/push-event conn event)
             (db/rollback-tx conn)
             (throw error))
           (finally
             (db/release-connection conn))))))))

(defn start []
  (log/debug "Starting Syncer...")
  (let [event-callbacks {
                         :ethlance/job-created handle-job-created
                         :ethlance/candidate-added handle-candidate-added
                         :ethlance/test-event handle-test-event
                         :ethlance/invoice-created handle-invoice-created
                         :ethlance/dispute-raised handle-dispute-raised
                         :ethlance/dispute-resolved handle-dispute-resolved
                         }

        dispatcher (build-dispatcher (:events @district.server.web3-events/web3-events) event-callbacks)
        callback-ids (doall (for [[event-key] event-callbacks]
                              (web3-events/register-callback! event-key dispatcher)))
        ]
    (log/debug "Syncer started")
    {:callback-ids callback-ids
     :dispatcher dispatcher}))


(defn stop
  "Stop the syncer mount component."
  []
  (log/debug "Stopping Syncer...")
  #_(unregister-callbacks!
     [::EthlanceEvent]))
