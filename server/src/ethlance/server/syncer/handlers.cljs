(ns ethlance.server.syncer.handlers
  (:require
    [clojure.core.async :as async :refer [<! go] :include-macros true]
    [district.server.async-db :as db]
    [district.shared.async-helpers :refer [<? safe-go]]
    [ethlance.server.db :as ethlance-db]
    [ethlance.server.ipfs :as ipfs]
    [ethlance.server.utils :as server-utils]
    [ethlance.shared.contract-constants :refer [offered-vec->flat-map
                                                enum-val->token-type]]
    [ethlance.shared.token-utils :as token-utils]
    [ethlance.shared.utils :as shared-utils]
    [honeysql.core :as sql]
    [taoensso.timbre :as log]))


(defn get-timestamp
  ([] (get-timestamp {}))
  ([_event] (.now js/Date)))


(defn build-ethlance-job-data-from-ipfs-object
  [ethlance-job-data]
  {:job/title (:job/title ethlance-job-data)
   :job/description (:job/description ethlance-job-data)
   :job/category (:job/category ethlance-job-data)
   :job/required-skills (:job/required-skills ethlance-job-data)
   :job/required-experience-level (:job/required-experience-level ethlance-job-data)
   :job/language-id nil; TODO: where does it come from
   :job/bid-option (:job/bid-option ethlance-job-data)
   :job/estimated-project-length (:job/estimated-project-length ethlance-job-data)
   :job/invitation-only? nil ; TODO: where does it come from
   :job/required-availability (:job/required-availability ethlance-job-data)})


(defn ensure-db-token-details
  [token-type token-address conn]
  (go
    (let [eth-token-details {:address "0x0000000000000000000000000000000000000000"
                             :name "Ether"
                             :symbol "ETH"
                             :type :eth
                             :decimals 18}]
      (when (not (<? (ethlance-db/get-token conn token-address)))
        (if (= :eth token-type)
          (<! (ethlance-db/store-token-details conn eth-token-details))
          (<! (ethlance-db/store-token-details conn (<! (token-utils/get-token-details token-type token-address)))))))))

(defn handle-job-created
  [conn _ {:keys [args] :as event}]
  (go
    (log/info ">>> Handling event job-created: " args)
    (let [ipfs-hash (shared-utils/hex->base58 (:ipfs-data args))
          ipfs-job-content (<? (server-utils/get-ipfs-meta @ipfs/ipfs ipfs-hash))
          offered-value (offered-vec->flat-map (first (:offered-values args)))
          token-address (:token-address offered-value)
          token-type (enum-val->token-type (:token-type offered-value))
          token-amount (:token-amount offered-value)
          for-the-db (merge {:job/id (:job args)
                             :job/status  "active" ; draft -> active -> finished hiring -> closed
                             :job/creator (:creator args)
                             :job/date-created (get-timestamp event)
                             :job/date-updated (get-timestamp event)

                             :job/token-type token-type
                             :job/token-amount token-amount
                             :job/token-address token-address
                             :job/token-id (:token-id offered-value)
                             :invited-arbiters (get args :invited-arbiters [])}
                            (build-ethlance-job-data-from-ipfs-object ipfs-job-content))]
      (log/debug ">>> handle-job-created" for-the-db)
      (<? (ensure-db-token-details token-type token-address conn))
      (<? (ethlance-db/add-job conn for-the-db)))))


(defn handle-invoice-created
  [conn _ {:keys [args]}]
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
                           :invoice/date-requested (get-timestamp)
                           :invoice/status "created"
                           :invoice/amount-requested (:token-amount offered-value)
                           :invoice/hours-worked (:invoice/hours-worked ipfs-data)
                           :invoice/hourly-rate (:invoice/hourly-rate ipfs-data)
                           :invoice/ref-id (:invoice-id args)}]
      (<? (ethlance-db/add-message conn invoice-message)))))


(defn handle-invoice-paid
  [conn _ {:keys [args]}]
  (safe-go
    (log/info "Handling event handle-invoice-paid")
    (let [ipfs-data (<? (server-utils/get-ipfs-meta @ipfs/ipfs (shared-utils/hex->base58 (:ipfs-data args))))
          invoice-id (:invoice-id args)
          job-story-id (:job-story/id ipfs-data)
          invoice-message (<? (db/get conn {:select [:*]
                                            :from [:JobStoryInvoiceMessage]
                                            :where [:and
                                                    [:= :JobStoryInvoiceMessage.job-story/id job-story-id]
                                                    [:= :JobStoryInvoiceMessage.invoice/ref-id invoice-id]]}))
          requested-value (:invoice/amount-requested invoice-message)
          invoice-message {:job-story/id job-story-id
                           :invoice/id (or (:invoice/id ipfs-data) (:invoice-id ipfs-data))
                           :message/type :job-story-message
                           :job-story-message/type :payment
                           :message/creator (:payer ipfs-data)
                           :message/date-created (get-timestamp)
                           :message/text "Invoice paid"
                           :invoice/hours-worked (:invoice/hours-worked ipfs-data)
                           :invoice/hourly-rate (:invoice/hourly-rate ipfs-data)
                           :invoice/date-paid (get-timestamp)
                           :invoice/amount-paid requested-value
                           :invoice/status "paid"}]

      (<? (ethlance-db/add-message conn invoice-message)))))


(defn handle-dispute-raised
  [conn _ {:keys [args]}]
  (safe-go
    (log/info "Handling event dispute-raised")
    (let [ipfs-data (<? (server-utils/get-ipfs-meta @ipfs/ipfs (shared-utils/hex->base58 (:ipfs-data args))))
          job-id (:job args)
          invoice-id (:invoice-id args)
          job-story (<? (db/get conn {:select [:*]
                                      :from [:JobStoryInvoiceMessage]
                                      :join [:JobStory [:= :JobStory.job/id job-id]]
                                      :where [:= :JobStoryInvoiceMessage.invoice/ref-id invoice-id]}))
          dispute-message {:job-story/id (:job-story/id job-story)
                           :message/type :job-story-message
                           :job-story-message/type :raise-dispute
                           :invoice/id invoice-id
                           :message/text (:message/text ipfs-data)
                           :message/creator (:message/creator ipfs-data)
                           :message/date-created (.now js/Date)}]
      (<? (ethlance-db/add-message conn dispute-message)))))


(defn handle-dispute-resolved
  [conn _ {:keys [args]}]
  (safe-go
    (log/info "Handling event dispute-resolved")
    (let [ipfs-data (<? (server-utils/get-ipfs-meta @ipfs/ipfs (shared-utils/hex->base58 (:ipfs-data args))))
          ;; job-id (:job args) ; FIXME: after re-deploying the contracts can use this added event field to get the job contract address (instead of relying on IPFS)
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


(defn handle-candidate-added
  [conn _ {:keys [args]}]
  (safe-go
    (log/info "Handling event candidate-added" args)
    (let [ipfs-data (<? (server-utils/get-ipfs-meta @ipfs/ipfs (shared-utils/hex->base58 (:ipfs-data args))))
          job-id (:job args)
          job-story-message-type (:job-story-message/type ipfs-data)
          message {:job-story/id (:job-story/id ipfs-data)
                   :job/id job-id
                   :candidate (:candidate ipfs-data)
                   :message/type :job-story-message
                   :job-story-message/type job-story-message-type
                   :message/text (:text ipfs-data)
                   :message/creator (:message/creator ipfs-data)
                   :message/date-created (get-timestamp)}]
      (<? (ethlance-db/add-message conn message)))))


(defn handle-quote-for-arbitration-set
  [conn _ {:keys [args]}]
  (safe-go
    (log/info (str "Handling event quote-for-arbitration-set" args))
    (let [quoted-value (offered-vec->flat-map (first (:quote args)))
          token-type (enum-val->token-type (:token-type quoted-value))
          for-the-db {:job/id (:job args)
                      :user/id (:arbiter args)
                      :job-arbiter/fee (:token-amount quoted-value)
                      :job-arbiter/fee-currency-id (if (keyword? token-type)
                                                     (name token-type)
                                                     (str token-type))
                      :job-arbiter/status "quote-set"}]
      (<? (ethlance-db/update-arbitration conn for-the-db)))))


(def system-message-address "0x0000000000000000000000000000000000000000")


(defn handle-quote-for-arbitration-accepted
  [conn _ {:keys [args]}]
  (safe-go
    (log/info (str ">>> Handling event quote-for-arbitration-accepted" args))
    (let [arbiter-id (:arbiter args)
          job-id (:job args)
          new-accepted-arbiter {:job/id job-id
                                :user/id arbiter-id
                                :job-arbiter/date-accepted (get-timestamp)
                                :job-arbiter/status "accepted"}
          previous-accepted-query {:select [:JobArbiter.job/id :JobArbiter.user/id]
                                   :from [:JobArbiter]
                                   :where [:and [:ilike :JobArbiter.job/id job-id]
                                           [:= :JobArbiter.job-arbiter/status "accepted"]]}
          previous-accepted-arbiter (<? (db/get conn previous-accepted-query))]
      (when previous-accepted-arbiter
        (<? (ethlance-db/update-arbitration conn (assoc previous-accepted-arbiter :job-arbiter/status "replaced")))
        (<? (ethlance-db/add-message conn
                                     {:job-story/id (<? (ethlance-db/get-job-story-id-by-job-id conn job-id))
                                      :message/type :job-story-message
                                      :job-story-message/type :feedback
                                      :message/creator system-message-address
                                      :message/text "Was replaced as arbiter due to inactivity"
                                      :message/date-created (get-timestamp)
                                      :feedback/rating 1
                                      :user/id (:user/id previous-accepted-arbiter)})))
      (<? (ethlance-db/update-arbitration conn new-accepted-arbiter)))))


(defn handle-arbiters-invited
  [conn _ {:keys [args]}]
  (go
    (log/info (str ">>> Handling event ArbitersInvited" args))
    (let [job-id (:job args)
          arbiters (:arbiters args)]
      (doseq [arbiter arbiters]
        ;; Guard against error of adding same arbitrer more than once (can be invited multiple times)
        (if (not (:exists (<? (db/get conn {:select [(sql/call :exists {:select [1]
                                                                        :from [:JobArbiter]
                                                                        :where [:and
                                                                                [:ilike :JobArbiter.job/id job-id]
                                                                                [:ilike :JobArbiter.user/id arbiter]]})]}))))
          (<? (ethlance-db/add-job-arbiter conn job-id arbiter))
          (log/debug ">>> handle-arbiters-invited Avoided adding duplicate" {:job job-id :arbiter arbiter}))))))


(defn handle-job-ended
  [conn _ {:keys [args]}]
  (safe-go
    (log/info (str "Handling event job-ended" args))
    (let [job-id (:job args)
          job-status "ended"
          stories (<? (db/all conn {:select [:JobStory.job-story/id]
                                    :from [:JobStory]
                                    :where [:and
                                            [:= :JobStory.job/id job-id]
                                            [:!= :JobStory.job-story/status "finished"]]}))]
      (doseq [story-id (map :job-story/id stories)]
        (<? (ethlance-db/update-job-story conn story-id {:job-story/status "job-ended"})))
      (<? (ethlance-db/update-job conn job-id {:job/status job-status})))))


(defn handle-job-funds-change
  [movement-sign-fn conn _ {:keys [args] :as event}]
  (go
    (log/info (str "handle-job-funds-change" event))
    (let [funds (:funds args)
          funds-map (map offered-vec->flat-map funds)
          job-id (:job args)
          funding-base {:tx (:transaction-hash event)
                        :job/id job-id
                        :job-funding/created-at (get-timestamp)}
          extract-token-info (fn [funds]
                               [(-> funds :token-type enum-val->token-type)
                                (:token-address funds)])
          tokens-info (map extract-token-info funds-map)
          funding-updates (map (fn [tv]
                                 (merge
                                   funding-base
                                   {:job-funding/amount (movement-sign-fn (:token-amount tv))
                                    :token-detail/id (:token-address tv)}))
                               funds-map)]
      (doseq [[token-type token-address] tokens-info] (<! (ensure-db-token-details token-type token-address conn)))
      (doseq [funding funding-updates]
        (<? (ethlance-db/insert-row! conn :JobFunding funding :ignore-conflict-on [:tx]))))))


(defn handle-test-event
  [& args]
  (log/info "Handling TestEvent args: " args))


(defn logging-noop-handler
  [event-name & args]
  (log/debug (str "Handler for " event-name " called with" (with-out-str (cljs.pprint/pprint args)))))
