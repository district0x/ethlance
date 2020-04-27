(ns ethlance.server.syncer
  "Service that syncs the ethlance in-memory database with the ethereum
  blockchain by reading events emitted by the ethlance smart contracts."
  (:require
   [mount.core :as mount :refer [defstate]]
   [taoensso.timbre :as log]
   [district.shared.async-helpers :refer [safe-go <?]]
   [ethlance.server.utils :as server-utils]
   [ethlance.server.db :as ethlance-db]
   [ethlance.server.ipfs :refer [ipfs]]

   ;; Mount Components
   [district.server.web3-events :refer [register-callback! unregister-callbacks!] :as web3-events]
   [ethlance.server.syncer.processor :as processor]
   [ethlance.server.ipfs :as ipfs]))


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
                                     :job/date-created (:block-timestamp event)
                                     :job/date-published (:block-timestamp event)
                                     :job/date-updated (:block-timestamp event)
                                     :job/token (:_token args)
                                     :job/token-version (:_tokenVersion args)
                                     :standard-bounty/id (:_bountyId args)
                                     :standard-bounty/deadline (:_deadline args)}
                                    (build-bounty-data-from-ipfs-object ipfs-data))))))

;; event BountyDataChanged(uint _bountyId, address _changer, string _data);
(defn handle-bounty-datachanged [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-bounty-datachanged" args))
  (let [job-ipfs-data (<? (server-utils/get-ipfs-meta @ipfs/ipfs (:_data args)))]
    (ethlance-db/update-bounty (:_bountyId args) (build-bounty-data-from-ipfs-object job-ipfs-data))))

;; event BountyFulfilled(uint _bountyId, uint _fulfillmentId, address payable[] _fulfillers, string _data, address _submitter);
(defn handle-bounty-fulfilled [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-bounty-fulfilled" args))
  (let [ipfs-data (<? (server-utils/get-ipfs-meta @ipfs/ipfs (:_data args)))
        bounty-id (:_bountyId args)
        job-id (ethlance-db/get-job-id-for-bounty (:_bountyId args))
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
                                  :invoice/ref-id (:_fulfillmentId args)
                                  :job-story/id job-story-id
                                  :job/id job-id})))))

;; event FulfillmentUpdated(uint _bountyId, uint _fulfillmentId, address payable[] _fulfillers, string _data);
(defn handle-fulfillment-updated [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-fulfillment-updated" args))
  (ethlance-db/set-job-story-invoice-status-for-bounties (:_bountyId args) (:_fulfillmentId args) "cancel")
  (handle-bounty-fulfilled nil event))

;; event FulfillmentAccepted(uint _bountyId, uint  _fulfillmentId, address _approver, uint[] _tokenAmounts);
(defn handle-fulfillment-accepted [_ {:keys [args] :as event}]
  ;; This means that one approver accepted the fulfillment. In our terms that a invoice was payed
  (log/info (str "Handling event handle-fulfillment-accepted" args))
  (ethlance-db/set-job-story-invoice-status-for-bounties (:_bountyId args) (:_fulfillmentId args) "payed"))

;; event BountyChanged(uint _bountyId, address _changer, address payable[] _issuers, address payable[] _approvers, string _data, uint _deadline);
(defn handle-bounty-changed [_ {:keys [args] :as event}]
  (safe-go
   (log/info (str "Handling event handle-bounty-changed" args))
   (let [ipfs-data (<? (server-utils/get-ipfs-meta @ipfs/ipfs (:_data args)))]
     (ethlance-db/update-bounty (:_bountyId args)
                                (merge {:job/date-updated (:block-timestamp event)
                                        :job/token (:_token args)
                                        :job/token-version (:_tokenVersion args)
                                        :standard-bounty/id (:_bountyId args)
                                        :standard-bounty/deadline (:_deadline args)}
                                       (build-bounty-data-from-ipfs-object ipfs-data))))))

;; event BountyDeadlineChanged(uint _bountyId, address _changer, uint _deadline);
(defn handle-bounty-deadline-changed [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-bounty-deadline-changed" args))
  (ethlance-db/update-bounty (:_bountyId args)
                             {:job/date-updated (:block-timestamp event)
                              :standard-bounty/deadline (:_deadline args)}))

;; event BountyIssuersUpdated(uint _bountyId, address _changer, address payable[] _issuers);
(defn handle-bounty-issuers-updated [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-bounty-issuers-updated" args)))

;; event BountyApproversUpdated(uint _bountyId, address _changer, address[] _approvers);
(defn handle-bounty-approvers-updated [_ {:keys [args] :as event}]
  ;; We aren't doing anything here since arbiters handling is being done thru mutations.
  (log/info (str "Handling event  handle-bounty-approvers-updated" args)))

;; event ContributionAdded(uint _bountyId, uint _contributionId, address payable _contributor, uint _amount);
(defn handle-bounty-contribution-added [_ {:keys [args] :as event}]
  (log/warn (str "Handling event handle-contribution-added. Not handling it." args)))

;; event ContributionRefunded(uint _bountyId, uint _contributionId);
(defn handle-bounty-contribution-refunded [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-contribution-refunded. Not handling it." args)))

;; event ContributionsRefunded(uint _bountyId, address _issuer, uint[] _contributionIds);
(defn handle-bounty-contributions-refunded [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-contributions-refunded. Not handling it." args)))

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
                                  :job/date-created (:block-timestamp event)
                                  :job/date-published (:block-timestamp event)
                                  :job/date-updated (:block-timestamp event)
                                  :job/token (:_token args)
                                  :job/token-version (:_tokenVersion args)
                                  :ethlance-job/id (:_jobId args)}
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
                                                      :invoice/ref-id (:_invoiceId args)})))))

;; event InvoiceAccepted(uint _jobId, uint  _invoiceId, address _approver, uint _amount);
(defn handle-invoice-accepted [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-invoice-accepted" args))
  (ethlance-db/set-job-story-invoice-status-for-ethlance-job (:_jobId args) (:_invoiceId args) "payed"))

;; event JobChanged(uint _jobId, address _changer, address payable[] _issuers, address payable[] _approvers, string _ipfsHash);
(defn handle-job-changed [_ {:keys [args] :as event}]
  (safe-go
   (log/info (str "Handling event handle-job-changed" args))
   (let [ipfs-data (<? (server-utils/get-ipfs-meta @ipfs/ipfs (:_ipfsHash args)))]
     (ethlance-db/update-ethlance-job (:_jobId args)
                                      (merge {:job/date-updated (:block-timestamp event)
                                              :job/token (:_token args)
                                              :job/token-version (:_tokenVersion args)
                                              :ethlance-job/id (:_jobId args)}
                                             (build-ethlance-job-data-from-ipfs-object ipfs-data))))))

;; event JobDataChanged(uint _jobId, address _changer, string _ipfsHash);
(defn handle-job-data-changed [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-job-data-changed" args))
  (safe-go
   (let [job-ipfs-data (<? (server-utils/get-ipfs-meta @ipfs/ipfs (:_ipfsHash args)))]
     (ethlance-db/update-ethlance-job (:_jobId args) (build-ethlance-job-data-from-ipfs-object job-ipfs-data)))))

;; event CandidateAccepted(uint jobId, address candidate);
(defn handle-candidate-accepted [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-candidate-accepted" args)))

;; event CandidateApplied(uint jobId, address candidate);
(defn handle-candidate-applied [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-candidate-applied" args)))

;; event ContributionAdded(uint _jobId, uint _contributionId, address payable _contributor, uint _amount);
(defn handle-job-contribution-added [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-contribution-added" args)))

;; event ContributionRefunded(uint _jobId, uint _contributionId);
(defn handle-job-contribution-refunded [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-contribution-refunded" args)))

;; event ContributionsRefunded(uint _jobId, address _issuer, uint[] _contributionIds);
(defn handle-job-contributions-refunded [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-contributions-refunded" args)))

;; event JobDrained(uint _jobId, address _issuer, uint[] _amounts);
(defn handle-job-drained [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-job-drained" args)))

;; event JobIssuersUpdated(uint _jobId, address _changer, address payable[] _issuers);
(defn handle-job-issuers-updated [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-job-issuers-updated" args)))

;; event JobApproversUpdated(uint _jobId, address _changer, address[] _approvers);
(defn handle-job-approvers-updated [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-job-approvers-updated" args)))

(defn wrap-save-event [handler]
  (fn [err event]
    ;; TODO: how should we handle errors?
    (when-not (:replay event)
      (ethlance-db/save-ethereum-log-event event))
    (handler err event)))

;;;;;;;;;;;;;;;;;;
;; Syncer Start ;;
;;;;;;;;;;;;;;;;;;

(defn start []
  (log/debug "Starting Syncer...")

  ;; StandardBounties
  (register-callback! :standard-bounties/bounty-issued (wrap-save-event handle-bounty-issued) :BountyIssued)
  (register-callback! :standard-bounties/bounty-approvers-updated (wrap-save-event handle-bounty-approvers-updated) :BountyApproversUpdated)
  (register-callback! :standard-bounties/bounty-issued (wrap-save-event handle-bounty-issued) :BountyIssued)
  (register-callback! :standard-bounties/contribution-added (wrap-save-event handle-bounty-contribution-added) :ContributionAdded)
  (register-callback! :standard-bounties/contribution-refunded (wrap-save-event handle-bounty-contribution-refunded) :ContributionRefunded)
  (register-callback! :standard-bounties/contributions-refunded (wrap-save-event handle-bounty-contributions-refunded) :ContributionsRefunded)
  (register-callback! :standard-bounties/bounty-drained (wrap-save-event handle-bounty-drained) :BountyDrained)
  (register-callback! :standard-bounties/action-performed (wrap-save-event handle-bounty-action-performed) :ActionPerformed)
  (register-callback! :standard-bounties/bounty-fulfilled (wrap-save-event handle-bounty-fulfilled) :BountyFulfilled)
  (register-callback! :standard-bounties/fulfillment-updated (wrap-save-event handle-fulfillment-updated) :FulfillmentUpdated)
  (register-callback! :standard-bounties/fulfillment-accepted (wrap-save-event handle-fulfillment-accepted) :FulfillmentAccepted)
  (register-callback! :standard-bounties/bounty-changed (wrap-save-event handle-bounty-changed) :BountyChanged)
  (register-callback! :standard-bounties/bounty-issuers-updated (wrap-save-event handle-bounty-issuers-updated) :BountyIssuersUpdated)
  (register-callback! :standard-bounties/bounty-data-changed (wrap-save-event handle-bounty-datachanged) :BountyDataChanged)
  (register-callback! :standard-bounties/bounty-deadline-changed (wrap-save-event handle-bounty-deadline-changed) :BountyDeadlineChanged)

  ;; EthlanceJobs
  (register-callback! :ethlance-jobs/job-issued (wrap-save-event handle-job-issued) :JobIssued)
  (register-callback! :ethlance-jobs/contribution-added (wrap-save-event handle-job-contribution-added) :ContributionAdded)
  (register-callback! :ethlance-jobs/contribution-refunded (wrap-save-event handle-job-contribution-refunded) :ContributionRefunded)
  (register-callback! :ethlance-jobs/contributions-refunded (wrap-save-event handle-job-contributions-refunded) :ContributionsRefunded)
  (register-callback! :ethlance-jobs/job-drained (wrap-save-event handle-job-drained) :JobDrained)
  (register-callback! :ethlance-jobs/job-invoice (wrap-save-event handle-job-invoice) :JobInvoice)
  (register-callback! :ethlance-jobs/invoice-accepted (wrap-save-event handle-invoice-accepted) :InvoiceAccepted)
  (register-callback! :ethlance-jobs/job-changed (wrap-save-event handle-job-changed) :JobChanged)
  (register-callback! :ethlance-jobs/job-issuers-updated (wrap-save-event handle-job-issuers-updated) :JobIssuersUpdated)
  (register-callback! :ethlance-jobs/job-approvers-updated (wrap-save-event handle-job-approvers-updated) :JobApproversUpdated)
  (register-callback! :ethlance-jobs/job-data-changed (wrap-save-event handle-job-data-changed) :JobDataChanged)
  (register-callback! :ethlance-jobs/candidate-accepted (wrap-save-event handle-candidate-accepted) :CandidateAccepted)
  (register-callback! :ethlance-jobs/candidate-applied (wrap-save-event handle-candidate-applied) :CandidateApplied)

  )


(defn stop
  "Stop the syncer mount component."
  []
  (log/debug "Stopping Syncer...")
  (unregister-callbacks!
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
