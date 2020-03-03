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
   [district.server.web3-events :refer [register-callback! unregister-callbacks!]]
   [ethlance.server.syncer.processor :as processor]
   [ethlance.server.ipfs :as ipfs]))


(declare start stop)
(defstate ^{:on-reload :noop} syncer
  :start (start)
  :stop (stop))

;; event BountyApproversUpdated(uint _bountyId, address _changer, address[] _approvers);
(defn handle-bounty-approvers-updated [_ {:keys [args] :as event}]
  ;; We aren't doing anything here since arbiters handling is being done thru mutations.
  (log/info (str "Handling event  handle-bounty-approvers-updated" args)))

;; event ContributionAdded(uint _bountyId, uint _contributionId, address payable _contributor, uint _amount);
(defn handle-contribution-added [_ {:keys [args] :as event}]
  (log/ward (str "Handling event handle-contribution-added. Not handling it." args)))

;; event ContributionRefunded(uint _bountyId, uint _contributionId);
(defn handle-contribution-refunded [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-contribution-refunded. Not handling it." args)))

;; event ContributionsRefunded(uint _bountyId, address _issuer, uint[] _contributionIds);
(defn handle-contributions-refunded [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-contributions-refunded. Not handling it." args)))

;; event BountyDrained(uint _bountyId, address _issuer, uint[] _amounts);
(defn handle-bounty-drained [_ {:keys [args] :as event}]

  (log/info (str "Handling event handle-bounty-drained" args)))

;; event ActionPerformed(uint _bountyId, address _fulfiller, string _data);
(defn handle-action-performed [_ {:keys [args] :as event}]
  ;; This lacks documentation on StandardBounties site. It doesn't contain _data file format.
  (log/info (str "Handling event handle-action-performed. Not handling it." args)))

;; event BountyFulfilled(uint _bountyId, uint _fulfillmentId, address payable[] _fulfillers, string _data, address _submitter);
(defn handle-bounty-fulfilled [_ {:keys [args] :as event}]
  ;; {
  ;; :payload {
  ;;   :description ;; A string representing the description of the fulfillment, and any necessary links to works
  ;;   :sourceFileName ;; A string representing the name of the file being submitted
  ;;   :sourceFileHash ;; A string representing the IPFS hash of the file being submitted
  ;;   :sourceDirectoryHash ;; A string representing the IPFS hash of the directory which holds the file being submitted
  ;;   :fulfillers [
  ;;     ;; a list of personas for the individuals whose work is being submitted
  ;;   ]
  ;;   :payoutAmounts [
  ;;     ;; an array of floats which is equal in length to the fulfillers array, representing the % of tokens which should be paid to each of the fulfillers (ie [50, 50] would represent an equal split of a bounty by 2 fulfillers)
  ;;   ]

  ;;   ;; ------- add optional fields here -------
  ;; }
  ;; ethlanceJobId
  ;; ethalnceContractId
  ;;
  ;; :meta {
  ;;   :platform ;; a string representing the original posting platform (ie 'gitcoin')
  ;;   :schemaVersion ;; a string representing the version number (ie '0.1')
  ;;   :schemaName ;; a string representing the name of the schema (ie 'standardSchema' or 'gitcoinSchema')
  ;; }}
  (log/info (str "Handling event handle-bounty-fulfilled" args))
  (let [ipfs-data (<? (server-utils/get-ipfs-meta @ipfs/ipfs (:_data args)))
        job-id (:_bountyId args)
        block-timestamp 1]
    ;; if JobStory/id exists on meta create an Invoice:
    ;;   :job-story/id bountyId
    ;;   :message/id from ipfs fullfilment meta (the ipfs hash is _data)
    ;; else ??

    ;; create a candidate
    ;; create a contract candidate
    ))

;; event FulfillmentUpdated(uint _bountyId, uint _fulfillmentId, address payable[] _fulfillers, string _data);
(defn handle-fulfillment-updated [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-fulfillment-updatedl" args)))

;; event FulfillmentAccepted(uint _bountyId, uint  _fulfillmentId, address _approver, uint[] _tokenAmounts);
(defn handle-fulfillment-accepted [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-fulfillment-acceptedl" args)))

;; event BountyChanged(uint _bountyId, address _changer, address payable[] _issuers, address payable[] _approvers, string _data, uint _deadline);
(defn handle-bounty-changed [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-bounty-changedl" args)))

;; event BountyIssuersUpdated(uint _bountyId, address _changer, address payable[] _issuers);
(defn handle-bounty-issuers-updated [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-bounty-issuers-updatedl" args)))

(defn build-job-data-from-ipfs-object [standard-bunty-data]
  {:job/title (-> standard-bunty-data :payload :title)
   :job/description (-> standard-bunty-data :payload :description)
   :job/category (-> standard-bunty-data :payload :majorCategory)
   :job/estimated-length (-> standard-bunty-data :payload :estimatedLength)
   :job/required-availability (-> standard-bunty-data :payload :requiredAvailability)
   :job/bid-option (-> standard-bunty-data :payload :bidOption)
   :job/expertise-level (-> standard-bunty-data :payload :difficulty)
   :job/number-of-candidates (-> standard-bunty-data :payload :numberOfCandidates)
   :job/invitation-only? (-> standard-bunty-data :invitationOnly)
   :job/platform (-> standard-bunty-data :meta :platform)
   :job/web-reference-url (-> standard-bunty-data :payload :webReferenceUrl)
   :job/reward (-> standard-bunty-data :payload :fulfillmentAmount)
   :job/language-id (-> standard-bunty-data :payload :language-id)})

;; event BountyIssued(uint _bountyId, address payable _creator, address payable[] _issuers, address[] _approvers, string _data, uint _deadline, address _token, uint _tokenVersion);
(defn handle-bounty-issued [_ {:keys [args] :as event}]
  (safe-go
   (log/info (str "Handling event handle-bounty-issued" event))

   ;; StandardBounties metadata (version 1.0)
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
   ;;            :requiredAvailability
   ;;            :bidOption
   ;;            :numberOfCandidates
   ;;            :invitationOnly
   ;;            }
   ;;  :meta {:platform ;; a string representing the original posting platform (ie 'gitcoin')
   ;;         :schemaVersion ;; a string representing the version number (ie '0.1')
   ;;         :schemaName ;; a string representing the name of the schema (ie 'standardSchema' or 'gitcoinSchema')
   ;;         }}

   (let [job-ipfs-data (<? (server-utils/get-ipfs-meta @ipfs/ipfs (:_data args)))
         block-timestamp 1]
     ;; we decided to use bountyId as jobId
     (ethlance-db/add-job (merge {:job/id (:_bountyId args)
                                  :job/bounty-id (:_bountyId args)
                                  ;; draft -> active -> finished hiring -> closed
                                  :job/status  "active"
                                  :job/date-created block-timestamp
                                  :job/date-published block-timestamp
                                  :job/date-updated block-timestamp
                                  :job/token (:_token args)
                                  :job/token-version (:_tokenVersion args)
                                  :job/date-deadline (:_deadline args)}
                                 (build-job-data-from-ipfs-object job-ipfs-data))
                          (:_issuers args)))))

;; event BountyDataChanged(uint _bountyId, address _changer, string _data);
(defn handle-bounty-datachanged [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-bounty-datachangedl" args))
  (let [job-ipfs-data (<? (server-utils/get-ipfs-meta @ipfs/ipfs (:_data args)))]
    (ethlance-db/update-job-data (-> (build-job-data-from-ipfs-object job-ipfs-data)
                                     (assoc :job/id (:_bountyId args))))))

;; event BountyDeadlineChanged(uint _bountyId, address _changer, uint _deadline);
(defn handle-bounty-deadline-changed [_ {:keys [args] :as event}]
  (log/info (str "Handling event handle-bounty-deadline-changedl" args)))

(defn start []
  (log/debug "Starting Syncer...")
  (register-callback! :standard-bounties/bounty-issued handle-bounty-issued :BountyIssued)
  (register-callback! :standard-bounties/bounty-approvers-updated handle-bounty-approvers-updated :BountyApproversUpdated)
  (register-callback! :standard-bounties/bounty-issued handle-bounty-issued :BountyIssued)
  (register-callback! :standard-bounties/contribution-added handle-contribution-added :ContributionAdded)
  (register-callback! :standard-bounties/contribution-refunded handle-contribution-refunded :ContributionRefunded)
  (register-callback! :standard-bounties/contributions-refunded handle-contributions-refunded :ContributionsRefunded)
  (register-callback! :standard-bounties/bounty-drained handle-bounty-drained :BountyDrained)
  (register-callback! :standard-bounties/action-performed handle-action-performed :ActionPerformed)
  (register-callback! :standard-bounties/bounty-fulfilled handle-bounty-fulfilled :BountyFulfilled)
  (register-callback! :standard-bounties/fulfillment-updated handle-fulfillment-updated :FulfillmentUpdated)
  (register-callback! :standard-bounties/fulfillment-accepted handle-fulfillment-accepted :FulfillmentAccepted)
  (register-callback! :standard-bounties/bounty-changed handle-bounty-changed :BountyChanged)
  (register-callback! :standard-bounties/bounty-issuers-updated handle-bounty-issuers-updated :BountyIssuersUpdated)
  (register-callback! :standard-bounties/bounty-data-changed handle-bounty-datachanged :BountyDataChanged)
  (register-callback! :standard-bounties/bounty-deadline-changed handle-bounty-deadline-changed :BountyDeadlineChanged)
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
