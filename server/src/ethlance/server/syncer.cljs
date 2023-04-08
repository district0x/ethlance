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

;; event ArbitersInvited(address[] _arbiters, uint _fee, uint _jobId, JobType _jobType);
(defn handle-arbiters-invited [_ {:keys [args]}]
  ;; We aren't handling this now, we aren't storing invitations in the DB
  ;; we are just storing arbiters who accepted the invitation
  (log/info "Not handling event handle-arbiters-invited" args))

(defn build-ethlance-job-data-from-ipfs-object [ethlance-job-data]
  {:job/title (:job/title ethlance-job-data)
   :job/description (:job/description ethlance-job-data)
   :job/category (:job/category ethlance-job-data)
   :job/required-skills (:job/required-skills ethlance-job-data)
   :job/required-experience-level (:job/required-experience-level ethlance-job-data)
   :job/language-id nil; TODO: where does it come from
   :job/bid-option (:job/bid-option ethlance-job-data)
   :job/estimated-project-length (:job/estimated-project-length ethlance-job-data)
   :job/max-number-of-candidates nil ; TODO: where does it come from
   :job/invitation-only? nil ; TODO: where does it come from
   :job/required-availability (:job/required-availability ethlance-job-data)
   })

;; event JobIssued(uint _jobId, address payable _creator, address payable[] _issuers, address[] _approvers, string _ipfsHash, address _token, uint _tokenVersion);
(defn handle-job-issued [conn _ {:keys [args] :as event}]
  (safe-go
   (log/info (str "Handling event handle-job-issued" args))
   (let [ipfs-data (<? (server-utils/get-ipfs-meta @ipfs/ipfs (:_ipfs-hash args)))]
     (<? (ethlance-db/add-job conn
                              (merge {:job/status  "active" ;; draft -> active -> finished hiring -> closed
                                      :job/date-created (:timestamp event)
                                      :job/date-updated (:timestamp event)
                                      :job/token (:_token args)
                                      :job/token-version (:_token-version args)}
                                      (build-ethlance-job-data-from-ipfs-object ipfs-data)))))))

;; event InvoiceAccepted(uint _jobId, uint  _invoiceId, address _approver, uint _amount);
(defn handle-invoice-accepted [conn _ {:keys [args]}]
  (safe-go
   (log/info (str "Handling event handle-invoice-accepted" args))
   (<? (ethlance-db/set-job-story-invoice-status-for-job conn (:_job-id args) (:_invoice-id args) "payed"))))

;; event JobChanged(uint _jobId, address _changer, address payable[] _issuers, address payable[] _approvers, string _ipfsHash);
(defn handle-job-changed [conn _ {:keys [args] :as event}]
  (safe-go
   (log/info (str "Handling event handle-job-changed" args))
   (let [ipfs-data (<? (server-utils/get-ipfs-meta @ipfs/ipfs (:_ipfs-hash args)))]
     (<? (ethlance-db/update-ethlance-job conn (:_job-id args)
                                          (merge {:job/date-updated (:timestamp event)
                                                  :job/token (:_token args)
                                                  :job/token-version (:_token-version args)}
                                                 (build-ethlance-job-data-from-ipfs-object ipfs-data)))))))

;; event JobDataChanged(uint _jobId, address _changer, string _ipfsHash);
(defn handle-job-data-changed [conn _ {:keys [args]}]
  (log/info (str "Handling event handle-job-data-changed" args))
  (safe-go
   (let [job-ipfs-data (<? (server-utils/get-ipfs-meta @ipfs/ipfs (:_ipfs-hash args)))]
     (<? (ethlance-db/update-ethlance-job conn (:_jobId args) (build-ethlance-job-data-from-ipfs-object job-ipfs-data))))))

;; event CandidateAccepted(uint _jobId, address _candidate);
(defn handle-candidate-accepted [conn _ {:keys [args]}]
  (safe-go
   (log/info (str "Handling event handle-candidate-accepted" args))
   (<? (ethlance-db/update-job-candidate conn (:_job-id args) (:_candidate args)))))

;; event ContributionAdded(uint _jobId, uint _contributionId, address payable _contributor, uint _amount);
(defn handle-job-contribution-added [conn _ {:keys [args]}]
  (safe-go
   (log/info (str "Handling event handle-job-contribution-added" args))
   (let [job-id (<? (ethlance-db/get-job-id-for-ethlance-job conn (:_job-id args)))]
     (<? (ethlance-db/add-contribution conn
                                       job-id
                                       (:_contributor args)
                                       (:_contribution-id args)
                                       (:_amount args))))))

;; event ContributionRefunded(uint _jobId, uint _contributionId);
(defn handle-job-contribution-refunded [conn _ {:keys [args]}]
  (safe-go
   (log/info (str "Handling event handle-contribution-refunded" args))
   (<? (ethlance-db/refund-job-contribution conn
                                            (<? (ethlance-db/get-job-id-for-ethlance-job conn (:_job-id args)))
                                            (:_contribution-id args)))))

;; event ContributionsRefunded(uint _jobId, address _issuer, uint[] _contributionIds);
(defn handle-job-contributions-refunded [conn _ {:keys [args]}]
  (safe-go
   (log/info (str "Handling event handle-contributions-refunded" args))
   (doseq [contribution-id (:_contributions-ids args)]
     (<? (ethlance-db/refund-job-contribution conn (<? (ethlance-db/get-job-id-for-ethlance-job conn (:_job-id args)))
                                              contribution-id)))))

;; event JobDrained(uint _jobId, address _issuer, uint[] _amounts);
(defn handle-job-drained [_ _ {:keys [args]}]
  (log/info (str "Handling event handle-job-drained" args)))

;; event JobIssuersUpdated(uint _jobId, address _changer, address payable[] _issuers);
(defn handle-job-issuers-updated [_ _ {:keys [args]}]
  (log/info (str "Handling event handle-job-issuers-updated" args)))

;; event JobApproversUpdated(uint _jobId, address _changer, address[] _approvers);
(defn handle-job-approvers-updated [conn _ {:keys [args]}]
  (safe-go
   (log/info (str "Handling event handle-job-approvers-updated" args))
   (<? (ethlance-db/update-job-approvers conn
                                         (<? (ethlance-db/get-job-id-for-ethlance-job conn (:_job-id args)))
                                         (:_approvers args)))))

(defn handle-job-created [conn _ {:keys [args] :as event}]
  (safe-go
   (log/info (str ">>> Handling event job-created" args))
   (println ">>> ipfs-data | type ipfs-data" (:ipfs-data args))
   (let [ipfs-hash (shared-utils/hex->base58 (:ipfs-data args))
         ipfs-job-content (<? (server-utils/get-ipfs-meta @ipfs/ipfs ipfs-hash))
         offered-value (offered-vec->flat-map (first (:offered-values args)))
         token-address (:token-address offered-value)
         token-type (enum-val->token-type (:token-type offered-value))
         ; TODO: instead of querying ETH token details via token-utils/get-token-details
         ;       store this hard-coded value
         eth-token-details {:address "0x0000000000000000000000000000000000000000"
                            :name "Ether"
                            :symbol "ETH"
                            :abi []}]
     (<? (ethlance-db/add-job conn
                              (merge {:job/id (:job args)
                                      :job/status  "active" ;; draft -> active -> finished hiring -> closed
                                      :job/creator (:creator args)
                                      :job/date-created (:timestamp event)
                                      :job/date-updated (:timestamp event)

                                      :job/token-type token-type
                                      :job/token-amount (:token-amount offered-value)
                                      :job/token-address token-address
                                      :job/token-id (:token-id offered-value)}
                                     (build-ethlance-job-data-from-ipfs-object ipfs-job-content))))
     (if (and
           (not= :eth token-type)
           (not (<? (ethlance-db/get-token conn token-address))))
       (let [token-details (<! (token-utils/get-token-details token-address))]
         (ethlance-db/store-token-details conn token-details))))))

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
                          :message/date-created (.now js/Date)
                          :invoice/status "created"
                          :invoice/amount-requested (:token-amount offered-value)
                          :invoice/ref-id (:invoice-id args)}]
     (<? (ethlance-db/add-message conn invoice-message)))))

(defn handle-dispute-raised [conn _ {:keys [args] :as dispute-raised-event}]
  (safe-go
   (log/info "Handling event dispute-raised")
   (let [ipfs-data (<? (server-utils/get-ipfs-meta @ipfs/ipfs (shared-utils/hex->base58 (:ipfs-data args))))
         job-story-id (:job-story/id ipfs-data)
         dispute-message {:job-story/id job-story-id
                          :message/type :job-story-message
                          :job-story-message/type :raise-dispute
                          :message/text (:message/text ipfs-data)
                          :message/creator (:message/creator ipfs-data)
                          :message/date-created (.now js/Date)
                          :invoice/status "dispute-raised"}]
     (<? (ethlance-db/add-message conn dispute-message)))))

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
                         :ethlance/test-event handle-test-event
                         :ethlance/invoice-created handle-invoice-created
                         :ethlance/dispute-raised handle-dispute-raised
                         ; :ethlance-issuer/arbiters-invited handle-arbiters-invited
                         ;; EthlanceJobs
                         ; :ethlance-jobs/job-issued handle-job-issued
                         ; :ethlance-jobs/contribution-added handle-job-contribution-added
                         ; :ethlance-jobs/contribution-refunded handle-job-contribution-refunded
                         ; :ethlance-jobs/contributions-refunded handle-job-contributions-refunded
                         ; :ethlance-jobs/job-drained handle-job-drained
                         ; :ethlance-jobs/job-invoice handle-job-invoice
                         ; :ethlance-jobs/invoice-accepted handle-invoice-accepted
                         ; :ethlance-jobs/job-changed handle-job-changed
                         ; :ethlance-jobs/job-issuers-updated handle-job-issuers-updated
                         ; :ethlance-jobs/job-approvers-updated handle-job-approvers-updated
                         ; :ethlance-jobs/job-data-changed handle-job-data-changed
                         ; :ethlance-jobs/candidate-accepted handle-candidate-accepted
                         }

        dispatcher (build-dispatcher (:events @district.server.web3-events/web3-events) event-callbacks)
        _ (identity event-callbacks) ; To silence clj-kondo warning during dev
        ; callback-ids []
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
