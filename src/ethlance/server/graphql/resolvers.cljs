(ns ethlance.server.graphql.resolvers
  (:require
    [district.graphql-utils :as graphql-utils]
    [district.server.async-db :as db :include-macros true]
    [district.shared.async-helpers :refer [<? safe-go]]
    [district.shared.error-handling :refer [try-catch-throw]]
    [ethlance.server.db :as ethlance-db]
    [ethlance.server.event-replay-queue :as replay-queue]
    [ethlance.server.graphql.authorization :as authorization]
    [ethlance.server.syncer :as syncer]
    [honeysql.core :as sql]
    [honeysql.helpers :as sql-helpers]
    [print.foo :include-macros true]
    [taoensso.timbre :as log]
    [clojure.string :as string]
    [ethlance.shared.spec :refer [validate-keys]]))

(def axios (js/require "axios"))
(def querystring (js/require "querystring"))

(defn- paged-query
  [conn query limit offset]
  (safe-go
    (let [paged-query (cond-> query
                        limit (assoc :limit limit)
                        offset (assoc :offset offset))
          total-count (count (<? (db/all conn query)))
          result (<? (db/all conn paged-query))
          end-cursor (cond-> (count result)
                       offset (+ offset))]
      {:items result
       :total-count total-count
       :end-cursor end-cursor
       :has-next-page (< end-cursor total-count)})))

(defn user-search-resolver [_ {:keys [:limit :offset :user/address :user/name :order-by :order-direction]
                               :as args} _]
  (db/with-async-resolver-conn conn
    (log/debug "user-search-resolver" args)
    (let [query (cond-> {:select [:*]
                         :from [:Users]}

                  address (sql-helpers/merge-where [:= :Users.user/address address])

                  name (sql-helpers/merge-where [:= :Users.user/user-name name])

                  order-by (sql-helpers/merge-order-by [[(get {:date-registered :user/date-registered
                                                               :date-updated :user/date-updated}
                                                              (graphql-utils/gql-name->kw order-by))
                                                         (or (keyword order-direction) :asc)]]))]
      (<? (paged-query conn query limit offset)))))

(defn user-resolver [_ {:keys [:user/address] :as args} _]
  (db/with-async-resolver-conn
    conn
    (log/debug "user-resolver" args)
    (<? (db/get conn {:select [:*]
                      :from [:Users]
                      :where [:= address :Users.user/address]}))))

(defn feedback->from-user-resolver [root _ _]
  (let [address (:feedback/from-user-address (graphql-utils/gql->clj root))]
    (user-resolver nil {:user/address address} nil)))

(defn user->is-registered-candidate-resolver [root _ _]
  (db/with-async-resolver-conn conn
    (let [{:keys [:user/address] :as user} (graphql-utils/gql->clj root)]
      (log/debug "user->is-registered-candidate-resolver" user)
      (not (= 0 (:count (<? (db/get conn {:select [[(sql/call :count :*) :count]]
                                          :from [:Candidate]
                                          :where [:= address :Candidate.user/address]}))))))))

(defn user->is-registered-employer-resolver [root _ _]
  (db/with-async-resolver-conn conn
    (let [{:keys [:user/address] :as user} (graphql-utils/gql->clj root)]
      (log/debug "user->is-registered-employer-resolver" user)
      (not (= 0 (:count (<? (db/get conn {:select [[(sql/call :count :*) :count]]
                                          :from [:Employer]
                                          :where [:= address :Employer.user/address]}))))))))

(defn user->is-registered-arbiter-resolver [root _ _]
  (db/with-async-resolver-conn conn
    (let [{:keys [:user/address] :as user} (graphql-utils/gql->clj root)]
      (log/debug "user->is-registered-arbiter-resolver" user)
      (not (= 0 (:count (<? (db/get conn {:select [[(sql/call :count :*) :count]]
                                          :from [:Arbiter]
                                          :where [:= address :Arbiter.user/address]}))))))))

(defn user->languages-resolvers [root _ _]
  (db/with-async-resolver-conn conn
    (let [{:keys [:user/address] :as user} (graphql-utils/gql->clj root)]
      (log/debug "user->languages-resolvers" user)
      (map :language/id
           (<? (db/all conn {:select [:*]
                             :from [:UserLanguage]
                             :where [:= address :UserLanguage.user/address]}))))))

(def ^:private user-type-query
  {:select [:type]
   :from [[{:union [{:select [:Candidate.user/address ["Candidate" :type]]
                     :from [:Candidate]}
                    {:select [:Employer.user/address ["Employer" :type]]
                     :from [:Employer]}
                    {:select [:Arbiter.user/address ["Arbiter" :type]]
                     :from [:Arbiter]}]} :a]]})

(def ^:private employer-query {:select [[:Employer.user/address :user/address]
                                        [:Employer.employer/professional-title :employer/professional-title]
                                        [:Employer.employer/bio :employer/bio]
                                        [:Employer.employer/rating :employer/rating]
                                        [:Users.user/date-registered :employer/date-registered]]
                               :from [:Employer]
                               :join [:Users [:= :Users.user/address :Employer.user/address]]})

(defn employer-resolver [_ {:keys [:user/address] :as args} _]
  (db/with-async-resolver-conn conn
    (log/debug "employer-resolver" args)
    (<? (db/get conn (sql-helpers/merge-where employer-query [:= address :Employer.user/address])))))

(def ^:private user-feedback-query {:select [:Message.message/id
                                             :Job.job/id
                                             :JobStory.job-story/id
                                             :JobStoryFeedbackMessage.feedback/rating
                                             [:Message.message/creator :feedback/from-user-address]
                                             [:JobStoryFeedbackMessage.user/address :feedback/to-user-address]
                                             [:Message.message/date-created :feedback/date-created]
                                             [:Message.message/text :feedback/text]
                                             :Users.user/name]
                                    :from [:JobStoryFeedbackMessage]
                                    :join [:JobStory [:= :JobStoryFeedbackMessage.job-story/id :JobStory.job-story/id]
                                           :Job [:= :JobStory.job/id :Job.job/id]
                                           :Message [:= :Message.message/id :JobStoryFeedbackMessage.message/id]
                                           :Users [:= :Users.user/address :JobStoryFeedbackMessage.user/address]]})

(defn employer->feedback-resolver [root {:keys [:limit :offset] :as args} _]
  (db/with-async-resolver-conn conn
    (let [{:keys [:user/address] :as employer} (graphql-utils/gql->clj root)
          query (sql-helpers/merge-where user-feedback-query [:= address :JobStoryFeedbackMessage.user/address])]
      (log/debug "employer->feedback-resolver" {:employer employer :args args})
      (<? (paged-query conn query limit offset)))))

(defn job-story->employer-feedback-resolver [root _ _]
  (db/with-async-resolver-conn conn
    (let [{:keys [:job-story/id] :as job-story} (graphql-utils/gql->clj root)]
      (log/debug "job-story->employer-feedback-resolver" {:job-story job-story})
      (<? (db/get conn (-> user-feedback-query
                         (sql-helpers/merge-where [:= id :JobStory.job-story/id])
                         ;; TODO: add to-user-type is employer when user-feedback-query is fixed
                         ))))))

(def ^:private arbiter-query {:select [:Arbiter.user/address
                                       :Arbiter.arbiter/bio
                                       :Arbiter.arbiter/professional-title
                                       :Arbiter.arbiter/fee
                                       :Arbiter.arbiter/fee-currency-id
                                       :Arbiter.arbiter/rating
                                       [:Users.user/date-registered :arbiter/date-registered]]
                              :from [:Arbiter]
                              :join [:Users [:= :Users.user/address :Arbiter.user/address]]})

(defn arbiter-resolver [_ {:keys [:user/address] :as args} _]
  (db/with-async-resolver-conn conn
    (log/debug "arbiter-resolver" args)
    (<? (db/get conn (sql-helpers/merge-where arbiter-query [:= address :Arbiter.user/address])))))

(defn arbiter-search-resolver [_ {:keys [:limit :offset
                                         :user/address
                                         :order-by :order-direction]
                                  :as args} _]
  (db/with-async-resolver-conn conn
    (log/debug "arbtier-search-resolver" args)
    (let [query (cond-> arbiter-query
                  address (sql-helpers/merge-where [:= address :Arbiter.user/address])

                  order-by (sql-helpers/merge-order-by [[(get {:date-created :user/date-created
                                                               :date-updated :user/date-updated}
                                                              (graphql-utils/gql-name->kw order-by))
                                                         (or (keyword order-direction) :asc)]]))]
      (<? (paged-query conn query limit offset)))))

(defn arbiter->feedback-resolver [root {:keys [:limit :offset] :as args} {:keys [conn]}]
  (db/with-async-resolver-conn conn
    (let [{:keys [:user/address] :as arbiter} (graphql-utils/gql->clj root)
          query (sql-helpers/merge-where user-feedback-query [:= address :JobStoryFeedbackMessage.user/address])]
      (log/debug "arbiter->feedback-resolver" {:arbiter arbiter :args args})
      (<? (paged-query conn query limit offset)))))

(defn feedback->to-user-type-resolver [root _ _]
  (db/with-async-resolver-conn conn
    (let [{:keys [:feedback/to-user-address] :as feedback} (graphql-utils/gql->clj root)
          q (sql-helpers/merge-where user-type-query [:= to-user-address :user/address])]
      (log/debug "feedback->to-user-type-resolver" feedback)
      (:type (<? (db/get conn q))))))

(defn feedback->from-user-type-resolver [root _ _]
  (db/with-async-resolver-conn conn
    (let [{:keys [:feedback/from-user-address] :as feedback} (graphql-utils/gql->clj root)
          q (sql-helpers/merge-where user-type-query [:= from-user-address :user/address])]
      (log/debug "feedback->from-user-type-resolver" feedback)
      (:type (<? (db/get conn q))))))

(def ^:private candidate-query
  {:select [[:Users.user/address :user/address]
            [:Candidate.candidate/professional-title :candidate/professional-title]
            [:Candidate.candidate/bio :candidate/bio]
            [:Candidate.candidate/rate :candidate/rate]
            [:Candidate.candidate/rating :candidate/rating]
            [:Candidate.candidate/rate-currency-id :candidate/rate-currency-id]
            [:Users.user/date-registered :candidate/date-registered]]
   :from [:Candidate]
   :join [:Users [:= :Users.user/address :Candidate.user/address]]})

(defn candidate-resolver [_ {:keys [:user/address] :as args} _]
  (db/with-async-resolver-conn conn
    (log/debug "candidate-resolver" args)
    (<? (db/get conn (sql-helpers/merge-where candidate-query [:= address :Candidate.user/address])))))

(defn- match-all [query {:keys [:join-table :on-column :column :all-values]}]
  (reduce-kv (fn [result index value]
               (let [table-name (-> query :from first name)
                     alias (str "c" index)
                     on-column-name (name on-column)
                     on-column-namespace (namespace on-column)
                     column-name (name column)
                     column-namespace (namespace column)]
                 (sql-helpers/merge-join result
                                         [join-table (keyword alias)]
                                         [:and
                                          [:= (keyword (str alias "." on-column-namespace) on-column-name)
                                           (keyword (str table-name "." on-column-namespace) on-column-name)]
                                          [:= value (keyword (str alias "." column-namespace) column-name)]])))
             query
             all-values))

(defn candidate-search-resolver [_ {:keys [:limit :offset
                                           :user/address
                                           :categories-and
                                           :categories-or
                                           :skills-and
                                           :skills-or
                                           :professional-title
                                           :order-by :order-direction]
                                    :as args} _]
  (db/with-async-resolver-conn conn
    (log/debug "candidate-search-resolver" {:args args})
    (let [query (cond-> (merge candidate-query
                               {:modifiers [:distinct]})

                  address (sql-helpers/merge-where [:= :Candidate.user/address address])

                  professional-title (sql-helpers/merge-where [:= professional-title :Candidate.candidate/professional-title])

                  categories-or (sql-helpers/merge-left-join :CandidateCategory
                                                             [:= :CandidateCategory.user/address :Candidate.user/address])

                  categories-or (sql-helpers/merge-where [:in :CandidateCategory.category/id categories-or])

                  categories-and (match-all {:join-table :CandidateCategory
                                             :on-column :user/address
                                             :column :category/id
                                             :all-values categories-and})

                  skills-or (sql-helpers/merge-left-join :CandidateSkill
                                                         [:= :CandidateSkill.user/address :Candidate.user/address])

                  skills-or (sql-helpers/merge-where [:in :CandidateSkill.skill/id skills-or])

                  skills-and (match-all {:join-table :CandidateSkill
                                         :on-column :user/address
                                         :column :skill/id
                                         :all-values skills-and})

                  order-by (sql-helpers/merge-order-by [[(get {:date-registered :user/date-registered
                                                               :date-updated :user/date-updated}
                                                              (graphql-utils/gql-name->kw order-by))
                                                         (or (keyword order-direction) :asc)]]))]
      (<? (paged-query conn query limit offset)))))

(defn candidate->candidate-categories-resolver [root _ _]
  (db/with-async-resolver-conn conn
    (let [{:keys [:user/address] :as candidate} (graphql-utils/gql->clj root)]
      (log/debug "candidate->candidate-categories-resolver" candidate)
      (map :category/id (<? (db/all conn {:select [:*]
                                          :from [:CandidateCategory]
                                          :where [:= address :CandidateCategory.user/address]}))))))

(defn candidate->candidate-skills-resolver [root _ _]
  (db/with-async-resolver-conn conn
    (let [{:keys [:user/address] :as candidate} (graphql-utils/gql->clj root)]
      (log/debug "candidate->candidate-skills-resolver" candidate)
      (map :skill/id (<? (db/all conn {:select [:*]
                                       :from [:CandidateSkill]
                                       :where [:= address :CandidateSkill.user/address]}))))))


(def candidate-ethlance-job-stories-query
  {:select [:*]
   :from [:EthlanceJobStory]
   :join [:JobStory [:= :EthlanceJobStory.job-story/id :JobStory.job-story/id]]})

(defn candidate->ethlance-job-stories-resolver [root {:keys [:limit :offset] :as args} _]
  (db/with-async-resolver-conn conn
    (let [address (:user/address (graphql-utils/gql->clj root))
          query (-> candidate-ethlance-job-stories-query
                  (sql-helpers/merge-where [:= address :EthlanceJobStory.ethlance-job-story/candidate]))]
      (log/debug "candidate->ethlance-job-stories-resolver" {:address address :args args})
      (<? (paged-query conn query limit offset)))))

(defn- employer-ethlance-job-stories-query [address]
  {:select
   [:JobStory.*
    :EthlanceJobStory.*]
   :from [:EthlanceJobStory]
   :join [:JobStory [:= :JobStory.job-story/id :EthlanceJobStory.job-story/id]
          :Job [:= :Job.job/id :JobStory.job/id]
          :JobCreator [:= :JobCreator.job/id :Job.job/id]]
   :where [:= :JobCreator.user/address address]})

(defn employer->ethlance-job-stories-resolver [root {:keys [:limit :offset] :as args} _]
  (db/with-async-resolver-conn conn
    (let [address (:user/address (graphql-utils/gql->clj root))
          query (employer-ethlance-job-stories-query address)]
      (log/debug "employer->ethlance-job-stories-resolver" {:address address :args args})
      (<? (paged-query conn query limit offset)))))

(defn- arbiter-ethlance-job-stories-query [address]
  {:select
   [:JobStory.*
    :EthlanceJobStory.*]
   :from [:EthlanceJobStory]
   :join [:JobStory [:= :JobStory.job-story/id :EthlanceJobStory.job-story/id]
          :Job [:= :Job.job/id :JobStory.job/id]
          :JobArbiter [:= :JobArbiter.job/id :Job.job/id]]
   :where [:= :JobArbiter.user/address address]})

(defn arbiter->ethlance-job-stories-resolver [root {:keys [:limit :offset] :as args} _]
  (db/with-async-resolver-conn conn
    (let [address (:user/address (graphql-utils/gql->clj root))
          query (arbiter-ethlance-job-stories-query address)]
      (log/debug "arbiter->ethlance-job-stories-resolver" {:address address :args args})
      (<? (paged-query conn query limit offset)))))

(defn candidate->feedback-resolver [root {:keys [:limit :offset] :as args} _]
  (db/with-async-resolver-conn conn
    (let [{:keys [:user/address] :as candidate} (graphql-utils/gql->clj root)
          query (-> user-feedback-query
                  (sql-helpers/merge-where [:= address :JobStoryFeedbackMessage.user/address]))]
      (log/debug "candidate->feedback-resolver" {:candidate candidate :args args})
      (<? (paged-query conn query limit offset)))))

(defn job-story->candidate-feedback-resolver [root _ _]
  (db/with-async-resolver-conn conn
    (let [{:keys [:job-story/id :contract/candidate-address] :as contract} (graphql-utils/gql->clj root)]
      (log/debug "job-story->candidate-feedback-resolver" {:contract contract})
      (<? (db/get conn (-> user-feedback-query
                         (sql-helpers/merge-where [:= id :JobStory.job-story/id])
                         (sql-helpers/merge-where [:= candidate-address :JobStoryFeedbackMessage.user/address])))))))

(defn employer-search-resolver [_ {:keys [:limit :offset
                                          :user/address
                                          :professional-title
                                          :order-by :order-direction]
                                   :as args} _]
  (db/with-async-resolver-conn conn
    (log/debug "employer-search-resolver" args)
    (let [query (cond-> employer-query

                  address (sql-helpers/merge-where [:= address :Employer.user/address])

                  professional-title (sql-helpers/merge-where [:= professional-title :Employer.employer/professional-title])

                  order-by (sql-helpers/merge-order-by [[(get {:date-registered :user/date-registered
                                                               :date-updated :user/date-updated}
                                                              (graphql-utils/gql-name->kw order-by))
                                                         (or (keyword order-direction) :asc)]]))]
      (<? (paged-query conn query limit offset)))))

(def ^:private job-query {:select [:Job.job/id
                                   :Job.job/type
                                   :Job.job/title
                                   :Job.job/description
                                   :Job.job/category
                                   :Job.job/status
                                   :Job.job/date-created
                                   :Job.job/date-published
                                   :Job.job/date-updated
                                   :Job.job/token
                                   :Job.job/token-version
                                   :Job.job/reward

                                   [:JobArbiter.user/address :job/accepted-arbiter-address]
                                   [:JobCreator.user/address :job/employer-address]]
                          :from [:Job]
                          :join [:JobArbiter [:= :JobArbiter.job/id :Job.job/id]
                                 :JobCreator [:= :JobCreator.job/id :Job.job/id]]})

(def ^:private standard-bounty-query {:select [:StandardBounty.standard-bounty/id
                                               :StandardBounty.standard-bounty/platform
                                               :StandardBounty.standard-bounty/deadline]
                                      :from [:StandardBounty]})

(def ^:private ethlance-job-query {:select [:EthlanceJob.ethlance-job/id
                                            :EthlanceJob.ethlance-job/estimated-length
                                            :EthlanceJob.ethlance-job/max-number-of-candidates
                                            :EthlanceJob.ethlance-job/invitation-only?
                                            :EthlanceJob.ethlance-job/hire-address
                                            :EthlanceJob.ethlance-job/bid-option]
                                   :from [:EthlanceJob]})

(defn job-resolver [parent {:keys [:job/id] :as args} _]
  (db/with-async-resolver-conn conn
    (log/debug "job-resolver" args)
    (let [parent-job-id (:job/id (graphql-utils/gql->clj parent))
          job-id (or parent-job-id id)
          job (<? (db/get conn (sql-helpers/merge-where job-query [:= job-id :Job.job/id])))
          job-type-query (-> (case (keyword (:job/type job))
                               :standard-bounty (sql-helpers/merge-where standard-bounty-query [:= job-id :StandardBounty.job/id])
                               :ethlance-job (sql-helpers/merge-where ethlance-job-query [:= job-id :EthlanceJob.job/id])))]
      (merge job (<? (db/get conn job-type-query))))))

(def ^:private job-story-query {:select [:JobStory.job-story/id
                                         :Job.job/id
                                         :JobStory.job-story/status
                                         :JobStory.job-story/date-created
                                         :JobStory.job-story/date-updated
                                         [:ContractCandidate.user/address :contract/candidate-address]]
                                :from [:JobStory]
                                :join [:Job [:= :Job.job/id :JobStory.job/id]]})

(defn job->job-stories-resolver [root {:keys [:limit :offset] :as args} _]
  (db/with-async-resolver-conn conn
    (let [{:keys [:job/id] :as job} (graphql-utils/gql->clj root)]
      (log/debug "job->job-stories-resolver" {:job job :args args})
      (<? (paged-query conn (sql-helpers/merge-where job-story-query [:= id :Contract.job/id]) limit offset)))))

(defn job-story-resolver [_ {job-id :job/id job-story-id :contract/id :as args} _]
  (db/with-async-resolver-conn conn
    (log/debug "job-story-resolver" args)
    (<? (db/get conn (-> job-story-query
                       (sql-helpers/merge-where [:= job-id :Job.job/id])
                       (sql-helpers/merge-where [:= job-story-id :JobStory.job-story/id]))))))


(def ^:private invoice-query {:select [:JobStoryInvoiceMessage.invoice/id :JobStoryInvoiceMessage.invoice/date-paid :JobStoryInvoiceMessage.invoice/amount-requested :JobStoryInvoiceMessage.invoice/amount-paid
                                       :JobStory.job-story/id :Job.job/id]
                              :from [:JobStoryInvoiceMessage]
                              :join [:JobStory [:= :JobStory.job-story/id :JobStoryInvoiceMessage.contract/id]
                                     :Job [:= :Job.job/id :JobStory.job/id]]})

(defn invoice-resolver [_ {message-id :message/id :as args} _]
  (db/with-async-resolver-conn conn
    (log/debug "invoice-resolver" {:args args})
    (<? (db/get conn (-> invoice-query
                       (sql-helpers/merge-where [:= message-id :JobStoryInvoiceMessage.message/id]))))))

(defn job-story->invoices-resolver [root {:keys [:limit :offset] :as args} _]
  (db/with-async-resolver-conn conn
    (let [{job-story-id :job-story/id :as job-story} (graphql-utils/gql->clj root)
          query (-> invoice-query
                  (sql-helpers/merge-where [:= job-story-id :JobStory.job-story/id]))]
      (log/debug "job-story->invoices-resolver" {:job-story job-story :args args})
      (<? (paged-query conn query limit offset)))))

(defn sign-in-mutation [_ {:keys [:data :data-signature] :as input} {:keys [config]}]
  (try-catch-throw
    (let [sign-in-secret (-> config :graphql :sign-in-secret)
          user-address (authorization/recover-personal-signature data data-signature)
          jwt (authorization/create-jwt user-address sign-in-secret)]
      (log/debug "sign-in-mutation" {:input input})
      {:jwt jwt :user/address user-address})))


(defn send-message-mutation [_ {:keys [:to :text]} {:keys [:current-user :timestamp]}]
  (db/with-async-resolver-tx conn
    (<? (ethlance-db/add-message conn {:message/type :direct-message
                                       :message/date-created timestamp
                                       :message/creator (:user/address current-user)
                                       :message/text text
                                       :direct-message/receiver to}))))

(defn raise-dispute-mutation [_ {:keys [:job-story/id :text]} {:keys [current-user timestamp]}]
  (db/with-async-resolver-tx conn
    (<? (ethlance-db/add-message conn {:message/type :job-story-message
                                       :job-story-message/type :raise-dispute
                                       :job-story/id id
                                       :message/date-created timestamp
                                       :message/creator (:user/address current-user)
                                       :message/text text}))))

(defn resolve-dispute-mutation [_ {:keys [:job-story/id]} {:keys [current-user timestamp]}]
  (db/with-async-resolver-tx conn
    (<? (ethlance-db/add-message conn {:message/type :job-story-message
                                       :job-story-message/type :resolve-dispute
                                       :job-story/id id
                                       :message/date-created timestamp
                                       :message/creator (:user/address current-user)
                                       :message/text "Dispute resolved"}))))

(defn leave-feedback-mutation [_ {:keys [:job-story/id :rating :to]} {:keys [current-user timestamp]}]
  (db/with-async-resolver-tx conn
    (<? (ethlance-db/add-message conn {:message/type :job-story-message
                                       :job-story-message/type :feedback
                                       :job-story/id id
                                       :message/date-created timestamp
                                       :message/creator (:user/address current-user)
                                       :message/text "Feedback"
                                       :feedback/rating rating
                                       :user/address to}))))

(defn update-employer-mutation [_ {:keys [input]} {:keys [timestamp]}]
  (db/with-async-resolver-tx conn
    (let [{:user/keys [address]} input
          response {:user/address address
                    :user/date-updated timestamp
                    :employer/date-updated timestamp}]
      (log/debug "update-employer-mutation" {:input input :response response})
      (<? (ethlance-db/upsert-user! conn (-> input
                                             (assoc :user/type :employer)
                                             (merge response))))
      response)))

(defn update-candidate-mutation [_ {:keys [input]} {:keys [timestamp]}]
  (db/with-async-resolver-tx conn
    (let [{:user/keys [address]} input
          response {:user/address address
                    :user/date-updated timestamp
                    :candidate/date-updated timestamp}]
      (log/debug "update-candidate-mutation" {:input input :response response})
      (<? (ethlance-db/upsert-user! conn (-> input
                                             (assoc :user/type :candidate)
                                             (merge response))))
      response)))

(defn update-arbiter-mutation [_ {:keys [input]} {:keys [timestamp]}]
  (db/with-async-resolver-tx conn
    (let [{:user/keys [address]} input
          response {:user/address address
                    :user/date-updated timestamp
                    :arbiter/date-updated timestamp}]
      (log/debug "arbiter-candidate-mutation" {:input input :response response})
      (<? (ethlance-db/upsert-user! conn (-> input
                                             (assoc :user/type :arbiter)
                                             (merge response))))
      response)))

(defn create-job-proposal-mutation [_ {:keys [text rate rate-currency-id]} {:keys [current-user timestamp]}]
  (db/with-async-resolver-tx conn
    (<? (ethlance-db/add-message conn {:message/type :job-story-message
                                       :job-story-message/type :proposal
                                       :message/date-created timestamp
                                       :message/creator (:user/address current-user)
                                       :message/text text
                                       :ethlance-job-story/proposal-rate rate
                                       :ethlance-job-story/proposal-rate-currency-id rate-currency-id}))))

(defn github-signup-mutation [_ {:keys [input]} {:keys [current-user config]}]
  (db/with-async-resolver-conn conn
    (let [{:keys [code]} input
          {:keys [client-id client-secret]} (:github config)
          response
          (<? (axios (clj->js {:url "https://github.com/login/oauth/access_token"
                               :method :post
                               :headers {"Content-Type" "application/json"
                                         "Accept" "application/json"}
                               :data (js/JSON.stringify (clj->js {:client_id client-id
                                                                  :client_secret client-secret
                                                                  :scope "user"
                                                                  :code code}))})))
          {:keys [data]} (js->clj response :keywordize-keys true)
          access-token (-> data (string/split "&") first (string/split "=") second)
          response
          (<? (axios (clj->js {:url "https://api.github.com/user"
                               :method :get
                               :headers {"Authorization" (str "token " access-token)
                                         "Content-Type" "application/json"
                                         "Accept" "application/json"}})))
          {:keys [name login email location] :as gh-resp} (:data (js->clj response :keywordize-keys true))
          _ (log/debug "github response" gh-resp)
          user {:user/address (:user/address current-user)
                :user/name name
                :user/github-username login
                :user/email email
                :user/country location}]

      (<? (ethlance-db/upsert-user-social-accounts! conn (select-keys user [:user/address :user/github-username])))

      user)))

(defn linkedin-signup-mutation [_ {:keys [input]} {:keys [current-user config]}]
  (db/with-async-resolver-conn conn
    (let [{:keys [code redirect-uri]} input
          {:keys [client-id client-secret]} (:linkedin config)
          response
          (<? (axios (clj->js {:url "https://www.linkedin.com/oauth/v2/accessToken"
                               :method :post
                               :headers {"Content-Type" "application/x-www-form-urlencoded"}
                               :data
                               (.stringify querystring (clj->js {:grant_type "authorization_code"
                                                                 :code code
                                                                 :redirect_uri redirect-uri
                                                                 :client_id client-id
                                                                 :client_secret client-secret}))})))
          {:keys [data]} (js->clj response :keywordize-keys true)
          access-token (:access_token data)
          {:keys [id localizedLastName localizedFirstName] :as resp1} (-> (<? (axios (clj->js {:url "https://api.linkedin.com/v2/me"
                                                                                               :method :get
                                                                                               :headers {"Authorization" (str "Bearer " access-token)
                                                                                                         "Accept" "application/json"}})))
                                                                        (js->clj :keywordize-keys true)
                                                                        :data)
          {email "emailAddress" :as resp2} (-> (<? (axios (clj->js {:url "https://api.linkedin.com/v2/emailAddress?q=members&projection=(elements*(handle~))"
                                                                    :method :get
                                                                    :headers {"Authorization" (str "Bearer " access-token)
                                                                              "Accept" "application/json"}})))
                                             js->clj
                                             (get-in ["data" "elements"])
                                             first
                                             (get "handle~"))
          _ (log/debug "linkedin response" (merge resp1 resp2))
          user {:user/address (:user/address current-user)
                :user/name (str localizedFirstName " " localizedLastName)
                :user/linkedin-username id
                :user/email email}]

      (<? (ethlance-db/upsert-user-social-accounts! conn (select-keys user [:user/address :user/linkedin-username])))

      user)))


(defn replay-events [_ _ _]
  (db/with-async-resolver-tx conn
    (let [dispatch-event (:dispatcher @syncer/syncer)]
      (loop [ev (<? (replay-queue/pop-event conn))]
        (when ev
          ;; NOTE: if something goes wrong inside dispatch-event it will
          ;; push the event into the queue again and throw, so this loop will stop
          (<? (dispatch-event nil ev)))))))

(defn require-auth [next]
  (fn [root args {:keys [:current-user] :as context} info]
    (if-not current-user
      (throw (js/Error. "Authentication required"))
      (next root args context info))))

(defn validate-input [next]
  (fn [root args context info]
    (let [some-invalid (some #(not %) (vals (validate-keys (:input args))))]
      (if some-invalid (throw (js/Error "Invalid form data sent to server"))
        (next root args context info)))))

(def resolvers-map {:Query {:user user-resolver
                            :userSearch user-search-resolver
                            :candidate candidate-resolver
                            :candidateSearch candidate-search-resolver
                            :employer employer-resolver
                            :employerSearch employer-search-resolver
                            :arbiter arbiter-resolver
                            :arbiterSearch arbiter-search-resolver
                            :job job-resolver
                            :jobStory job-story-resolver
                            :invoice invoice-resolver}
                    :Job {:job_stories job->job-stories-resolver}
                    :JobStory {:jobStory_employerFeedback job-story->employer-feedback-resolver
                               :jobStory_candidateFeedback job-story->candidate-feedback-resolver
                               :jobStory_invoices job-story->invoices-resolver
                               :job job-resolver}
                    :User {:user_languages user->languages-resolvers
                           :user_isRegisteredCandidate user->is-registered-candidate-resolver
                           :user_isRegisteredEmployer user->is-registered-employer-resolver
                           :user_isRegisteredArbiter user->is-registered-arbiter-resolver}
                    :Candidate {:candidate_feedback candidate->feedback-resolver
                                :candidate_categories candidate->candidate-categories-resolver
                                :candidate_skills candidate->candidate-skills-resolver
                                :candidate_ethlanceJobStories candidate->ethlance-job-stories-resolver}
                    :EthlanceJobStory {:job job-resolver}
                    :Employer {:employer_feedback employer->feedback-resolver
                               :employer_ethlanceJobStories employer->ethlance-job-stories-resolver}
                    :Arbiter {:arbiter_feedback arbiter->feedback-resolver
                              :arbiter_ethlanceJobStories arbiter->ethlance-job-stories-resolver}
                    :Feedback {:feedback_toUserType feedback->to-user-type-resolver
                               :feedback_fromUser feedback->from-user-resolver
                               :feedback_fromUserType feedback->from-user-type-resolver}
                    :Mutation {:signIn sign-in-mutation
                               :sendMessage (require-auth send-message-mutation)
                               :raiseDispute (require-auth raise-dispute-mutation)
                               :resolveDispute (require-auth resolve-dispute-mutation)
                               :leaveFeedback (require-auth leave-feedback-mutation)
                               ;; TODO : do require auth
                               :updateEmployer (require-auth update-employer-mutation)
                               :updateCandidate (require-auth (validate-input update-candidate-mutation))
                               :updateArbiter (require-auth update-arbiter-mutation)
                               :createJobProposal (require-auth create-job-proposal-mutation)
                               :replayEvents replay-events
                               :githubSignUp (require-auth github-signup-mutation)
                               :linkedinSignUp (require-auth linkedin-signup-mutation)}})
