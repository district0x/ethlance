(ns ethlance.server.syncer
  "Service that syncs the ethlance in-memory database with the ethereum
  blockchain by reading events emitted by the ethlance smart contracts."
  (:require
   [mount.core :as mount :refer [defstate]]
   [taoensso.timbre :as log]
   [district.shared.async-helpers :refer [safe-go <?]]
   [ethlance.server.utils :as server-utils]
   [ethlance.server.event-replay-queue :as replay-queue]
   [cljs.core.async :as async]
   [district.server.smart-contracts :as smart-contracts]
   [camel-snake-kebab.core :as camel-snake-kebab]
   [bignumber.core :as bn]

   ;; Mount Components
   [district.server.web3-events :refer [register-callback! unregister-callbacks!] :as web3-events]
   [ethlance.server.syncer.processor :as processor]
   [ethlance.server.db :as ethlance-db]
   [ethlance.server.ipfs :refer [ipfs] :as ipfs]))


(declare start stop)
(defstate ^{:on-reload :noop} syncer
  :start (start)
  :stop (stop))

;;;;;;;;;;;;;;;
;; IPFS DATA ;;
;;;;;;;;;;;;;;;

;; StandardBounties metadata (version 1.0)
;; ---------------------------------------
;; {:payload { ;; standard fields
;;            :title ;; a string representing the title of the bounty
;;            :description ;; a string representing the description of the bounty, including all requirements
;;            :fulfillmentAmount ;; an integer amount that will be paid out to fufillers
;;            :categories ;; an array of strings, representing the categories of tasks which are being requested
;;            :expectedRevisions ;; an integer of how many times the spec is expected to be adjusted during fulfillment
;;            :difficulty ;; a string representing how difficult this bounty is (one of: easy, medium, hard)
;;            :privateFulfillments ;; boolean desrcibing whether fulfillments are only visible to the issuer
;;            :fulfillersNeedApproval ;; boolean that forces users to be approved before fulfilling the bounty

;;            ;; extended fields
;;            :ipfsFilename ;; a string representing the name of the file
;;            :ipfsHash ;; the IPFS hash of the directory which can be used to access the file
;;            :webReferenceURL ;; the link to a relevant web reference (ie github issue)

;;            ;; extended fields ethlance
;;            :majorCategoy
;;            :ethlanceJobStoryId
;;            :ethlanceMessageId

;;            }
;;  :meta {:platform ;; a string representing the original posting platform (ie 'gitcoin')
;;         :schemaVersion ;; a string representing the version number (ie '0.1')
;;         :schemaName ;; a string representing the name of the schema (ie 'standardSchema' or 'gitcoinSchema')
;;         }}


;; Bounty fulfillment data
;; -----------------------
;; {:payload {
;;            :description ;; A string representing the description of the fulfillment, and any necessary links to works
;;            :sourceFileName ;; A string representing the name of the file being submitted
;;            :sourceFileHash ;; A string representing the IPFS hash of the file being submitted
;;            :sourceDirectoryHash ;; A string representing the IPFS hash of the directory which holds the file being submitted
;;            :fulfillers [
;;                         ;; a list of personas for the individuals whose work is being submitted
;;                         ]
;;            :payoutAmounts [
;;                            ;; an array of floats which is equal in length to the fulfillers array, representing the % of tokens which should be paid to each of the fulfillers (ie [50, 50] would represent an equal split of a bounty by 2 fulfillers)
;;                            ]

;;            ;; ------- add optional fields here -------
;;            }
;;  :ethlanceJobStoryId
;;  :ethlanceMessageId
;;  :meta {:platform ;; a string representing the original posting platform (ie 'gitcoin')
;;         :schemaVersion ;; a string representing the version number (ie '0.1')
;;         :schemaName ;; a string representing the name of the schema (ie 'standardSchema' or 'gitcoinSchema')
;;         }}


;; Ethlance Job metadata (EthlanceJob submission)
;; ----------------------------------------------
;; {:payload {:title
;;            :description
;;            :categories
;;            :expertiseLevel
;;            :ipfsFilename
;;            :webReferenceURL
;;            :reward
;;            languageId
;;            :estimatedLenght
;;            :maxNumberOfCandidates
;;            :invitationOnly
;;            :requiredAvailability
;;            :ethlanceMessageId
;;            :ethlanceJobStoryId
;;  }
;;  :meta {:schemaVersion}}

;; Ethlance Job Invoice metadata
;; -----------------------------
;; {:payload {:description
;;            :ethlanceJobStoryId
;;            :ethlanceMessageId}
;;  :meta {:schemaVersion}}

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; EthlanceIssuer events ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; event ArbitersInvited(address[] _arbiters, uint _fee, uint _jobId, JobType _jobType);
(defn handle-arbiters-invited [_ {:keys [args] :as event}]
  ;; We aren't handling this now, we aren't storing invitations in the DB
  ;; we are just storing arbiters who accepted the invitation
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; StandardBounties events ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn build-bounty-data-from-ipfs-object [standard-bunty-data]
  {:job/title (-> standard-bunty-data :payload :title)
   :job/description (-> standard-bunty-data :payload :description)
   :job/category (-> standard-bunty-data :payload :majorCategory)
   :job/web-reference-url (-> standard-bunty-data :payload :webReferenceUrl)
   :job/reward (-> standard-bunty-data :payload :fulfillmentAmount)
   :job/language-id (-> standard-bunty-data :payload :language-id)
   :standard-bounty/platform (-> standard-bunty-data :meta :platform)})

;; event BountyIssued(uint _bountyId, address payable _creator, address payable[] _issuers, address[] _approvers, string _data, uint _deadline, address _token, uint _tokenVersion);
(defn handle-bounty-issued [_ {:keys [args] :as event}]
  (safe-go
   (log/info (str "Handling event handle-bounty-issued" event))
   (let [ipfs-data (<? (server-utils/get-ipfs-meta @ipfs/ipfs (:_data args)))]
     (ethlance-db/add-bounty (merge {:job/status  "active" ;; draft -> active -> finished hiring -> closed
                                     :job/date-created (:timestamp event)
                                     :job/date-published (:timestamp event)
                                     :job/date-updated (:timestamp event)
                                     :job/token (:_token args)
                                     :job/token-version (:_token-version args)
                                     :standard-bounty/id (:_bounty-id args)
                                     :standard-bounty/deadline (:_deadline args)}
                                    (build-bounty-data-from-ipfs-object ipfs-data))))))

;; event BountyDataChanged(uint _bountyId, address _changer, string _data);
(defn handle-bounty-datachanged [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-bounty-datachanged" args))
  (let [job-ipfs-data (<? (server-utils/get-ipfs-meta @ipfs/ipfs (:_data args)))]
    (ethlance-db/update-bounty (:_bounty-id args) (build-bounty-data-from-ipfs-object job-ipfs-data))))

;; event BountyFulfilled(uint _bountyId, uint _fulfillmentId, address payable[] _fulfillers, string _data, address _submitter);
(defn handle-bounty-fulfilled [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-bounty-fulfilled" args))
  (let [ipfs-data (<? (server-utils/get-ipfs-meta @ipfs/ipfs (:_data args)))
        bounty-id (:_bounty-id args)
        job-id (ethlance-db/get-job-id-for-bounty (:_bounty-id args))
        creator (first (:_fulfillers args))]
    (when-not (-> ipfs-data :payload :ethlanceJobStoryId)
      ;; it is a bounty from other systems like gitcoin
      (let [job-story-id (ethlance-db/add-job-story {:job/id job-id
                                                     :job-story/creator creator})]
        (ethlance-db/add-message {:message/creator creator
                                  :message/text (str "Palease send money to "
                                                     (-> ipfs-data :payload :fulfillers)
                                                     "in this amounts"
                                                     (-> ipfs-data :payload :payoutAmounts))
                                  :message/type :job-story-message
                                  :job-story-message/type :invoice
                                  :invoice/ref-id (:_fulfillment-id args)
                                  :job-story/id job-story-id
                                  :job/id job-id})))))

;; event FulfillmentUpdated(uint _bountyId, uint _fulfillmentId, address payable[] _fulfillers, string _data);
(defn handle-fulfillment-updated [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-fulfillment-updated" args))
  (ethlance-db/set-job-story-invoice-status-for-bounties (:_bounty-id args) (:_fulfillment-id args) "cancel")
  (handle-bounty-fulfilled nil event))

;; event FulfillmentAccepted(uint _bountyId, uint  _fulfillmentId, address _approver, uint[] _tokenAmounts);
(defn handle-fulfillment-accepted [_ {:keys [args] :as event}]
  ;; This means that one approver accepted the fulfillment. In our terms that a invoice was payed
  (log/info (str "Handling event handle-fulfillment-accepted" args))
  (ethlance-db/set-job-story-invoice-status-for-bounties (:_bounty-id args) (:_fulfillment-id args) "payed"))

;; event BountyChanged(uint _bountyId, address _changer, address payable[] _issuers, address payable[] _approvers, string _data, uint _deadline);
(defn handle-bounty-changed [_ {:keys [args] :as event}]
  (safe-go
   (log/info (str "Handling event handle-bounty-changed" args))
   (let [ipfs-data (<? (server-utils/get-ipfs-meta @ipfs/ipfs (:_data args)))]
     (ethlance-db/update-bounty (:_bounty-id args)
                                (merge {:job/date-updated (:timestamp event)
                                        :job/token (:_token args)
                                        :job/token-version (:_token-version args)
                                        :standard-bounty/id (:_bounty-id args)
                                        :standard-bounty/deadline (:_deadline args)}
                                       (build-bounty-data-from-ipfs-object ipfs-data))))))

;; event BountyDeadlineChanged(uint _bountyId, address _changer, uint _deadline);
(defn handle-bounty-deadline-changed [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-bounty-deadline-changed" args))
  (ethlance-db/update-bounty (:_bounty-id args)
                             {:job/date-updated (:timestamp event)
                              :standard-bounty/deadline (:_deadline args)}))

;; event BountyIssuersUpdated(uint _bountyId, address _changer, address payable[] _issuers);
(defn handle-bounty-issuers-updated [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-bounty-issuers-updated" args)))

;; event BountyApproversUpdated(uint _bountyId, address _changer, address[] _approvers);
(defn handle-bounty-approvers-updated [_ {:keys [args] :as event}]
  (log/info (str "Handling event  handle-bounty-approvers-updated" args))
  (ethlance-db/update-job-approvers (ethlance-db/get-job-id-for-bounty (:_bounty-id args))
                                    (:_approvers args)))

;; event ContributionAdded(uint _bountyId, uint _contributionId, address payable _contributor, uint _amount);
(defn handle-bounty-contribution-added [_ {:keys [args] :as event}]
  (log/warn (str "Handling event handle-contribution-added." args))
  (ethlance-db/add-contribution (ethlance-db/get-job-id-for-bounty (:_bounty-id args))
                                (:_contributor args)
                                (:_contribution-id args)
                                (:_amount args)))

;; event ContributionRefunded(uint _bountyId, uint _contributionId);
(defn handle-bounty-contribution-refunded [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-contribution-refunded." args))
  (ethlance-db/refund-job-contribution (ethlance-db/get-job-id-for-bounty (:_bounty-id args))
                                       (:_contribution-id args)))

;; event ContributionsRefunded(uint _bountyId, address _issuer, uint[] _contributionIds);
(defn handle-bounty-contributions-refunded [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-contributions-refunded. Not handling it." args))
  (doseq [contribution-id (:_contributions-ids args)]
    (ethlance-db/refund-job-contribution (ethlance-db/get-job-id-for-bounty (:_bounty-id args))
                                         contribution-id)))

;; event BountyDrained(uint _bountyId, address _issuer, uint[] _amounts);
(defn handle-bounty-drained [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-bounty-drained" args)))

;; event ActionPerformed(uint _bountyId, address _fulfiller, string _data);
(defn handle-bounty-action-performed [_ {:keys [args] :as event}]
  ;; This lacks documentation on StandardBounties site. It doesn't contain _data file format.
  (log/info (str "Handling event handle-action-performed. Not handling it." args)))

;;;;;;;;;;;;;;;;;;;;;;;;;
;; EthalnceJobs events ;;
;;;;;;;;;;;;;;;;;;;;;;;;;

(defn build-ethlance-job-data-from-ipfs-object [ethlance-job-data]
  {:job/title (-> ethlance-job-data :payload :title)
   :job/description (-> ethlance-job-data :payload :description)
   :job/category (-> ethlance-job-data :payload :majorCategory)
   :job/web-reference-url (-> ethlance-job-data :payload :webReferenceUrl)
   :job/reward (-> ethlance-job-data :payload :fulfillmentAmount)
   :job/language-id (-> ethlance-job-data :payload :language-id)
   :ethlance-job/estimatedLenght (-> ethlance-job-data :payload :estimatedLenght)
   :ethlance-job/maxNumberOfCandidates (-> ethlance-job-data :payload :maxNumberOfCandidates)
   :ethlance-job/invitationOnly (-> ethlance-job-data :payload :invitationOnly)
   :ethlance-job/requiredAvailability (-> ethlance-job-data :payload :requiredAvailability)
   })

;; event JobIssued(uint _jobId, address payable _creator, address payable[] _issuers, address[] _approvers, string _ipfsHash, address _token, uint _tokenVersion);
(defn handle-job-issued [_ {:keys [args] :as event}]
  (safe-go
   (log/info (str "Handling event handle-job-issued" args))
   (let [ipfs-data (<? (server-utils/get-ipfs-meta @ipfs/ipfs (:_ipfs-hash args)))]
     (ethlance-db/add-ethlance-job (merge {:job/status  "active" ;; draft -> active -> finished hiring -> closed
                                  :job/date-created (:timestamp event)
                                  :job/date-published (:timestamp event)
                                  :job/date-updated (:timestamp event)
                                  :job/token (:_token args)
                                  :job/token-version (:_token-version args)
                                  :ethlance-job/id (:_job-id args)}
                                 (build-ethlance-job-data-from-ipfs-object ipfs-data))))))



;; event JobInvoice(uint _jobId, uint _invoiceId, address payable _invoiceIssuer, string _ipfsHash, address _submitter, uint _amount);
(defn handle-job-invoice [_ {:keys [args] :as event}]
  (safe-go
   (log/info (str "Handling event handle-job-invoice" args))
   (let [ipfs-data (<? (server-utils/get-ipfs-meta @ipfs/ipfs (:_ipfs-hash args)))
         ethlance-job-id (:_jobId args)]
     (let [job-story-id (-> ipfs-data :payload :ethlanceJobStoryId)]
       (ethlance-db/update-job-story-invoice-message {:job-story/id job-story-id
                                                      :message/id (-> ipfs-data :payload :ethlanceMessageId)
                                                      :invoice/ref-id (:_invoice-id args)})))))

;; event InvoiceAccepted(uint _jobId, uint  _invoiceId, address _approver, uint _amount);
(defn handle-invoice-accepted [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-invoice-accepted" args))
  (ethlance-db/set-job-story-invoice-status-for-ethlance-job (:_job-id args) (:_invoice-id args) "payed"))

;; event JobChanged(uint _jobId, address _changer, address payable[] _issuers, address payable[] _approvers, string _ipfsHash);
(defn handle-job-changed [_ {:keys [args] :as event}]
  (safe-go
   (log/info (str "Handling event handle-job-changed" args))
   (let [ipfs-data (<? (server-utils/get-ipfs-meta @ipfs/ipfs (:_ipfs-hash args)))]
     (ethlance-db/update-ethlance-job (:_job-id args)
                                      (merge {:job/date-updated (:timestamp event)
                                              :job/token (:_token args)
                                              :job/token-version (:_token-version args)
                                              :ethlance-job/id (:_job-id args)}
                                             (build-ethlance-job-data-from-ipfs-object ipfs-data))))))

;; event JobDataChanged(uint _jobId, address _changer, string _ipfsHash);
(defn handle-job-data-changed [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-job-data-changed" args))
  (safe-go
   (let [job-ipfs-data (<? (server-utils/get-ipfs-meta @ipfs/ipfs (:_ipfs-hash args)))]
     (ethlance-db/update-ethlance-job (:_jobId args) (build-ethlance-job-data-from-ipfs-object job-ipfs-data)))))

;; event CandidateAccepted(uint _jobId, address _candidate);
(defn handle-candidate-accepted [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-candidate-accepted" args))
  (ethlance-db/update-ethlance-job-candidate (:_job-id args) (:_candidate args)))

;; event ContributionAdded(uint _jobId, uint _contributionId, address payable _contributor, uint _amount);
(defn handle-job-contribution-added [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-contribution-added" args))
  (ethlance-db/add-contribution (ethlance-db/get-job-id-for-ethlance-job (:_job-id args))
                                (:_contributor args)
                                (:_contribution-id args)
                                (:_amount args)))

;; event ContributionRefunded(uint _jobId, uint _contributionId);
(defn handle-job-contribution-refunded [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-contribution-refunded" args))
  (ethlance-db/refund-job-contribution (ethlance-db/get-job-id-for-ethlance-job (:_job-id args))
                                       (:_contribution-id args)))

;; event ContributionsRefunded(uint _jobId, address _issuer, uint[] _contributionIds);
(defn handle-job-contributions-refunded [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-contributions-refunded" args))
  (doseq [contribution-id (:_contributions-ids args)]
    (ethlance-db/refund-job-contribution (ethlance-db/get-job-id-for-ethlance-job (:_job-id args))
                                         contribution-id)))

;; event JobDrained(uint _jobId, address _issuer, uint[] _amounts);
(defn handle-job-drained [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-job-drained" args)))

;; event JobIssuersUpdated(uint _jobId, address _changer, address payable[] _issuers);
(defn handle-job-issuers-updated [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-job-issuers-updated" args)))

;; event JobApproversUpdated(uint _jobId, address _changer, address[] _approvers);
(defn handle-job-approvers-updated [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-job-approvers-updated" args))
  (ethlance-db/update-job-approvers (ethlance-db/get-job-id-for-ethlance-job (:_job-id args))
                                    (:_approvers args)))

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
      (let [contract-key (-> event :contract :contract-key)
            event-key (-> event :event)
            handler (get contract-ev->handler [contract-key event-key])]
        (safe-go
         (try
           (let [block-timestamp (<? (block-timestamp block-number))
                 event (-> event
                           (update :event camel-snake-kebab/->kebab-case)
                           (update-in [:args :version] bn/number)
                           (update-in [:args :timestamp] (fn [timestamp]
                                                           (if timestamp
                                                             (bn/number timestamp)
                                                             block-timestamp))))
                 res (handler err event)]
             ;; Calling a handler can throw or return a go block (when using safe-go)
             ;; in the case of async ones, the go block will return the js/Error.
             ;; In either cases push the event to the queue, so it can be replayed later
             (when (satisfies? cljs.core.async.impl.protocols/ReadPort res)
               (let [r (<! res)]
                 (when (instance? js/Error r)
                   (throw r))))
             res)
           (catch js/Error error
             (replay-queue/push-event event)
             (throw error))))))))

(defn start []
  (log/debug "Starting Syncer...")
  (let [event-callbacks {:ethlance-issuer/arbiters-invited handle-arbiters-invited

                         ;; StandardBounties
                         :standard-bounties/bounty-issued handle-bounty-issued
                         :standard-bounties/bounty-approvers-updated handle-bounty-approvers-updated
                         :standard-bounties/contribution-added handle-bounty-contribution-added
                         :standard-bounties/contribution-refunded handle-bounty-contribution-refunded
                         :standard-bounties/contributions-refunded handle-bounty-contributions-refunded
                         :standard-bounties/bounty-drained handle-bounty-drained
                         :standard-bounties/action-performed handle-bounty-action-performed
                         :standard-bounties/bounty-fulfilled handle-bounty-fulfilled
                         :standard-bounties/fulfillment-updated handle-fulfillment-updated
                         :standard-bounties/fulfillment-accepted handle-fulfillment-accepted
                         :standard-bounties/bounty-changed handle-bounty-changed
                         :standard-bounties/bounty-issuers-updated handle-bounty-issuers-updated
                         :standard-bounties/bounty-data-changed handle-bounty-datachanged
                         :standard-bounties/bounty-deadline-changed handle-bounty-deadline-changed

                         ;; EthlanceJobs
                         :ethlance-jobs/job-issued handle-job-issued
                         :ethlance-jobs/contribution-added handle-job-contribution-added
                         :ethlance-jobs/contribution-refunded handle-job-contribution-refunded
                         :ethlance-jobs/contributions-refunded handle-job-contributions-refunded
                         :ethlance-jobs/job-drained handle-job-drained
                         :ethlance-jobs/job-invoice handle-job-invoice
                         :ethlance-jobs/invoice-accepted handle-invoice-accepted
                         :ethlance-jobs/job-changed handle-job-changed
                         :ethlance-jobs/job-issuers-updated handle-job-issuers-updated
                         :ethlance-jobs/job-approvers-updated handle-job-approvers-updated
                         :ethlance-jobs/job-data-changed handle-job-data-changed
                         :ethlance-jobs/candidate-accepted handle-candidate-accepted}
        dispatcher (build-dispatcher (:events @district.server.web3-events/web3-events) event-callbacks)
        callback-ids (doall (for [[event-key] event-callbacks]
                              (web3-events/register-callback! event-key dispatcher)))]
    {:callback-ids callback-ids
     :dispatcher dispatcher}))


(defn stop
  "Stop the syncer mount component."
  []
  (log/debug "Stopping Syncer...")
  #_(unregister-callbacks!
     [::EthlanceEvent]))

(comment

  (go
    (let [[account] (<! (web3-eth/accounts @web3))
          bounty-issuer-address (bounty-issuer/test-ethlance-bounty-issuer-address)
          token-address "0x0000000000000000000000000000000000000000"
          token-version (bounty-issuer/token-version :eth)
          deposit 2e18]

      (<? (bounty-issuer/issue-and-contribute bounty-issuer-address
                                              ["hash"
                                               123123
                                               token-address
                                               token-version
                                               (hex deposit)]
                                              {:from account
                                               :value deposit}))))
  )
