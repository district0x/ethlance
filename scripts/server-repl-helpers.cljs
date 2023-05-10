(shadow/repl :dev-server)
(in-ns 'ethlance.server.core)

; Subscribe to contract event
(district.server.web3-events/register-callback! :ethlance/test-event (fn [err event] (println ">>> server.web3-events/register-callback!  GOT" {:err err :event event})))

; Call smart-contract method
(def res (district.server.smart-contracts/contract-send :ethlance :emitTestEvent [42]))
(def res-val (atom nil))
(cljs.core.async/take! res (fn [val] (reset! res-val val)))

; Subscribe (low level) via cljs-web3-next
(def ethlance-contract (:instance (district.server.smart-contracts.contract :ethlance)))
(cljs-web3-next.eth/subscribe-events ethlance-contract :TestEvent {:from-block 1} (fn [err event] (println ">>> cljs-web3-next/subscribe-events TestEvent" err event)))

; Subscribe via server-smart-contracts
(district.server.smart-contracts/subscribe-events :ethlance :TestEvent {:from-block 1} [(fn [err event] (println ">>> server.smart-contracts/subscribe-events TestEvent" err event))])

; IPFS data -> ipfs hash
(def ipfs-data "0x12204129c213954a4864af722e5160c92b158f1215c13416a1165a6ee7142371b368")

(require '[alphabase.base58 :as base58])
(require '[alphabase.hex :as hex])

(defn base58->hex [base58-str]
  (->> base58-str
    base58/decode
    hex/encode
    (str "0x" ,,,)))

(defn hex->base58 [hex-str]
  (-> hex-str
    (clojure.string/replace #"^0x" "")
    hex/decode
    base58/encode))

; DB operations (getting connection)
(in-ns 'ethlance.server.db)
(def conn (atom nil))
(.then (db/get-connection) #(reset! conn %))
(drop-db! @conn)
(create-db! @conn)

; Trying DB queries
(in-ns 'ethlance.server.graphql.resolvers)
(def contract "0xD6C0df65B9c3b453b016f46EC1f5F5751a74b2f2")
(def query-promise (atom nil))
(reset! query-promise (job-resolver nil {:contract contract} nil))
(def query-result (atom nil))
(.then @query-promise #(reset! query-result %))
(.catch @query-promise #(reset! query-result %))

(sql/format (sql-helpers/merge-where job-query [:= contract :Job.job/contract]))
(db/get conn (sql-helpers/merge-where job-query [:= contract :Job.job/contract]))


; Add some example data for proposals
(shadow/repl :dev-server)
(in-ns 'ethlance.server.db)
(require '[clojure.core.async :as async])
(def conn (atom nil))
(.then (db/get-connection) #(reset! conn %))

(defn add-proposal [job-contract connection]
  (async/go
    (let [invitation-message-id nil
          proposal-message-id nil
          candidate-address "0xCAfDfDAfE913F15F12F902d6480178484063A6Fb"
          story-fields {:job/id 14
                        :job-story/status "active"
                        :job-story/date-created (.getTime (new js/Date "2023-02-13 12:07"))
                        :job-story/date-updated (.getTime (new js/Date "2023-02-13 12:07"))
                        :job-story/invitation-message-id invitation-message-id
                        :job-story/proposal-message-id proposal-message-id
                        :job-story/raised-dispute-message-id nil
                        :job-story/resolved-dispute-message-id nil
                        :job-story/proposal-rate 2
                        :job-story/proposal-rate-currency-id "ETH"
                        }
          job-id ]
      (ethlance.server.db/add-ethlance-job-story connection story-fields))))

(def res (add-proposal "0x9C63FC175F700698A6FA836705fA1706113deBBD" @conn))

; transform EthlanceStructs.OfferedValues (nested vector coming from web3.js) to HashMap
(def event {:args {:job "0x5Af29eB96F033B1C11D74952d127ed0522cd6203",
                  :job-version 1,
                  :creator "0x0935D2ec65144343df67Bd3c7399c37Beb1bBce0",
                  :offered-values  [[[[0 "0x1111111111111111111111111111111111111111"] 1]
                                      300000000000000000]],
                  :invited-arbiters [],
                  :ipfs-data "0x1220e2ca4600a88eeecc6cff7c7ad6a8474285ad8011b5d6015149a58a4688fe2968",
                  :timestamp 21,
                  :version nil}})

(def off-va [[[0 "0x1111111111111111111111111111111111111111"] 1] 300000000000000000])

(defn offered-vec->map
  [offered]
  {:value (get-in offered [1])
   :token
   {:tokenId (get-in offered [0 0 0])
    :tokenContract
    {:tokenType (get-in offered [0 1])
     :tokenAddress (get-in offered [0 0 1])}}})

(offered-vec->map off-va)

; Make DB queries
(in-ns 'ethlance.server.graphql.resolvers)
(require '[ethlance.server.db :as db])
(require '[district.server.async-db :as async-db])
(def conn (atom nil))
(.then (async-db/get-connection) #(reset! conn %))

(def query-res (atom nil))
(def query-chan (atom nil))

(def query {:select [:Job.job/id :Job.job/contract] :from [:Job] :where [:= 1 1]})

(def query {:select [:*
                     ; [:Employer.user/id :user/id]
                     ; [:Employer.employer/professional-title :employer/professional-title]
                     ; [:Employer.employer/bio :employer/bio]
                     ; [:Employer.employer/rating :employer/rating]
                     ; [:Users.user/date-registered :employer/date-registered]
                     ]
                               :from [:Employer]
                               :join [:Users [:= :Users.user/id :Employer.user/id]
                                      :Job [:= :Job.job/creator :Employer.user/id]]})

(def query {:select [(sql/call :exists {:select [1] :from [:Job] :where [:= :Job.job/id "0xf327083ff367825521d4992B907bC8BaB5B265B7"]})]})
(reset! query-chan (db/get @conn query))
(cljs.core.async/take! @query-chan (fn [val] (println ">> take query-atom" val) (reset! query-res val)))

(reset! query-chan (db/get @conn (sql-helpers/merge-where candidate-query [:ilike "0x0935D2ec65144343df67Bd3c7399c37Beb1bBce0" :Candidate.user/id])))
(reset! query-chan (db/all @conn job->employer-query))
(cljs.core.async/take! @query-chan (fn [val] (println ">> take query-atom" val) (reset! query-res val)))

(def candi {:jobStory_invitationMessageId nil,
  :jobStory_dateCreated 1677100474598,
  :jobStory_status "proposed",
  :jobStory_raisedDisputeMessageId nil,
  :jobStory_candidate "0x0935d2ec65144343df67bd3c7399c37beb1bbce0",
  :jobStory_proposalRateCurrencyId nil,
  :jobStory_proposalRate 33,
  :jobStory_id 72,
  :job_id nil,
  :jobStory_resolvedDisputeMessageId nil,
  :jobStory_proposalMessageId 74,
  :jobStory_dateUpdated nil,
  :job_contract "0x15DeA784613B57a5427069d76f2b3E182ba44687",
  :jobStory_dateCandidateAccepted nil})

; Detect if row exists
(in-ns 'ethlance.server.graphql.resolvers)
(sql/format {:select [:exists [:select 1 :from [:Job] :where [:= :Job.job/id 42]]]})
(sql/format {:select [(sql/call :exists {:select [1] :from [:Job] :where [:= :Job.job/id "ABC"]})]})
(sql/format {:select [:*] :from [:Job] :where [:>= 2 {:select [(sql/call :count :*)] :from [:JobStoryFeedbackMessage] :where [:= :JobStoryFeedbackMessage.user/id "0x0"]}]})
(sql/format {:select [:* [(sql/raw "'wut'") :ze-but]] :from [:jobstory]})
; Generate data
; Users
(generate-users @conn [["EMPLOYER" "0xb794f5ea0ba39494ce839613fffba74279579268"]])

; Generate job-story messages
(shadow/repl :dev-server)
(in-ns 'tests.graphql.generator)
(require '[district.server.async-db :as async-db])
(require '[ethlance.server.db :as ethlance-db])
(def conn (atom nil))
(.then (async-db/get-connection) #(reset! conn %))
(def result (atom nil))

(defn add-message! [conn {:keys [creator story-id receiver] :as message-params} result]
  (safe-go
    (reset! result (<? (ethlance-db/add-message conn (generate-message message-params))))))

(defn proposal-message [employer-addr story-id]
  {:message/creator employer-addr
   :message/text "This offer looks good. I would like to work on it"
   :message/type :job-story-message
   :job-story-message/type :proposal
   :job-story/id story-id})

(add-message! @conn (proposal-message "0x0935D2ec65144343df67Bd3c7399c37Beb1bBce0" 1) result)

; Reading files
(defn read-edn [path f]
  (.readFile fs path "utf8" (fn [err data] (f (cljs.reader/read-string data)))))

(defn read-edn-sync [path]
  (cljs.reader/read-string (.readFileSync fs path "utf8")))

; Add Ethereum token details
(store-token-details @conn {:address "0x0000000000000000000000000000000000000000" :name "Ether" :symbol "ETH" :abi []})
