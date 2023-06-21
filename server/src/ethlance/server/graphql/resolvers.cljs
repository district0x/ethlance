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
    [oops.core]
    [honeysql.core :as sql]
    [honeysql.helpers :as sql-helpers]
    [print.foo :include-macros true]
    [taoensso.timbre :as log]
    [clojure.string :as string]
    [ethlance.shared.spec :refer [validate-keys]]))

(def axios (js/require "axios"))
(def querystring (js/require "querystring"))

(defn js-obj->clj-map [obj]
  (let [obj-keys (district.graphql-utils/gql->clj (js-keys obj))
        keywordize (fn [k] (keyword (camel-snake-kebab.core/->kebab-case k)))
        assoc-keywordized (fn [acc js-key] (assoc acc (keywordize js-key) (aget obj js-key)))]
    (reduce assoc-keywordized {} obj-keys)))

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

(defn user-search-resolver [_ {:keys [:limit :offset :user/id :user/name :order-by :order-direction]
                               :as args} _]
  (db/with-async-resolver-conn conn
    (log/debug "user-search-resolver" args)
    (let [query (cond-> {:select [:*]
                         :from [:Users]}

                  id (sql-helpers/merge-where [:= :Users.user/id id])

                  name (sql-helpers/merge-where [:= :Users.user/user-name name])

                  order-by (sql-helpers/merge-order-by [[(get {:date-registered :user/date-registered
                                                               :date-updated :user/date-updated}
                                                              (graphql-utils/gql-name->kw order-by))
                                                         (or (keyword order-direction) :asc)]]))]
      (<? (paged-query conn query limit offset)))))

(defn user-resolver [parent {:keys [:user/id] :as args} _]
  (db/with-async-resolver-conn
    conn
    (let [clj-parent (graphql-utils/gql->clj parent)
          user-id-from-parent (or (:user/id clj-parent) (:message/creator clj-parent))
          user-id (or id user-id-from-parent)]
      (log/debug "user-resolver" args)
      (<? (db/get conn {:select [:*]
                        :from [:Users]
                        :where [:ilike user-id :Users.user/id]})))))

(defn feedback->from-user-resolver [root _ _]
  (let [id (:feedback/from-user-address (graphql-utils/gql->clj root))]
    (user-resolver nil {:user/id id} nil)))

(defn user->is-registered-candidate-resolver [root _ _]
  (db/with-async-resolver-conn conn
    (let [{:keys [:user/id] :as user} (graphql-utils/gql->clj root)]
      (log/debug "user->is-registered-candidate-resolver" user)
      (not (= 0 (:count (<? (db/get conn {:select [[(sql/call :count :*) :count]]
                                          :from [:Candidate]
                                          :where [:= id :Candidate.user/id]}))))))))

(defn user->is-registered-employer-resolver [root _ _]
  (db/with-async-resolver-conn conn
    (let [{:keys [:user/id] :as user} (graphql-utils/gql->clj root)]
      (log/debug "user->is-registered-employer-resolver" user)
      (not (= 0 (:count (<? (db/get conn {:select [[(sql/call :count :*) :count]]
                                          :from [:Employer]
                                          :where [:= id :Employer.user/id]}))))))))

(defn user->is-registered-arbiter-resolver [root _ _]
  (db/with-async-resolver-conn conn
    (let [{:keys [:user/id] :as user} (graphql-utils/gql->clj root)]
      (log/debug "user->is-registered-arbiter-resolver" user)
      (not (= 0 (:count (<? (db/get conn {:select [[(sql/call :count :*) :count]]
                                          :from [:Arbiter]
                                          :where [:= id :Arbiter.user/id]}))))))))

(defn user->languages-resolvers [root _ _]
  (db/with-async-resolver-conn conn
    (let [{:keys [:user/id] :as user} (graphql-utils/gql->clj root)]
      (log/debug "user->languages-resolvers" user)
      (map :language/id
           (<? (db/all conn {:select [:*]
                             :from [:UserLanguage]
                             :where [:= id :UserLanguage.user/id]}))))))

(def ^:private user-type-query
  {:select [:type]
   :from [[{:union [{:select [:Candidate.user/id ["Candidate" :type]]
                     :from [:Candidate]}
                    {:select [:Employer.user/id ["Employer" :type]]
                     :from [:Employer]}
                    {:select [:Arbiter.user/id ["Arbiter" :type]]
                     :from [:Arbiter]}]} :a]]})

(def ^:private employer-query {:select [:Employer.*
                                        [:Users.user/date-registered :employer/date-registered]]
                               :from [:Employer]
                               :join [:Users [:= :Users.user/id :Employer.user/id]]})

(def ^:private job->employer-query {:select[:*]
                                    :from [:Employer]
                                    :join [:Users [:= :Users.user/id :Employer.user/id]
                                           :Job [:= :Job.job/creator :Employer.user/id]]})

(defn job->token-details-resolver [parent args context info]
  (log/debug "job->token-details-resolver" parent)
  (db/with-async-resolver-conn conn
    (let [clj-parent (js->clj parent)
          token-address (get (js->clj parent :keywordize-keys) "job_tokenAddress")
          query {:select [:*]
                 :from [:TokenDetail]
                 :where [:= :TokenDetail.token-detail/id token-address]}]
      (<? (db/get conn query)))))

(defn participant->user-resolver [parent args context info]
  (log/debug "participant->user-resolver")
  (db/with-async-resolver-conn conn
    (let [clj-parent (graphql-utils/gql->clj parent)
          user-id (:user/id clj-parent)
          query {:select [:*]
                 :from [:Users]
                 :where [:ilike :Users.user/id user-id]}
          user-results (<? (db/get conn query))]
      user-results)))

(defn job->employer-resolver [parent args context info]
  (db/with-async-resolver-conn conn
    (log/debug "job->employer-resolver contract:" (:contract args))
    (let [contract (:job/id (graphql-utils/gql->clj parent))
          query (sql-helpers/merge-where job->employer-query [:= contract :Job.job/id])]
      (<? (db/get conn query)))))

(def ^:private job->arbiter-query {:select[:*]
                                   :from [:Arbiter]
                                   :join [:JobArbiter [:= :JobArbiter.user/id :Arbiter.user/id]
                                          :Job [:= :Job.job/id :JobArbiter.job/id]]})

(defn job->arbiter-resolver [parent args context info]
  (db/with-async-resolver-conn conn
    (log/debug "job->arbiter-resolver contract:" (:contract args))
    (let [contract (:job/id (graphql-utils/gql->clj parent))
          query (sql-helpers/merge-where job->arbiter-query [:= contract :Job.job/id])]
      (<? (db/get conn query)))))

(defn employer-resolver [raw-parent {:keys [:user/id ] :as args} _]
  (db/with-async-resolver-conn conn
    (log/debug "employer-resolver" args)
    (let [address-from-args (:user/id args)
          parent (graphql-utils/gql->clj raw-parent)
          address-from-parent (or
                                (:employer/id parent)
                                (:job/creator parent))
          address (or address-from-args address-from-parent)]
      (<? (db/get conn (sql-helpers/merge-where employer-query [:ilike address :Employer.user/id]))))))

(def ^:private user-feedback-query {:select [:Message.message/id
                                             :Job.job/id
                                             :JobStory.job-story/id
                                             :JobStoryFeedbackMessage.feedback/rating
                                             [:Message.message/creator :feedback/from-user-address]
                                             [:JobStoryFeedbackMessage.user/id :feedback/to-user-address]
                                             [:Message.message/date-created :feedback/date-created]
                                             [:Message.message/text :feedback/text]
                                             :Users.user/name]
                                    :from [:JobStoryFeedbackMessage]
                                    :join [:JobStory [:= :JobStoryFeedbackMessage.job-story/id :JobStory.job-story/id]
                                           :Job [:= :JobStory.job/id :Job.job/id]
                                           :Message [:= :Message.message/id :JobStoryFeedbackMessage.message/id]
                                           :Users [:= :Users.user/id :JobStoryFeedbackMessage.user/id]]})

(defn employer->feedback-resolver [root {:keys [:limit :offset] :as args} _]
  (db/with-async-resolver-conn conn
    (let [{:keys [:user/id] :as employer} (graphql-utils/gql->clj root)
          query (sql-helpers/merge-where user-feedback-query [:= id :JobStoryFeedbackMessage.user/id])]
      (log/debug "employer->feedback-resolver" {:employer employer :args args})
      (<? (paged-query conn query limit offset)))))

(def ^:private arbiter-query {:select [:Arbiter.*
                                       [:Users.user/date-registered :arbiter/date-registered]]
                              :from [:Arbiter]
                              :join [:Users [:= :Users.user/id :Arbiter.user/id]]})

(defn arbiter-resolver [raw-parent args _]
  (db/with-async-resolver-conn conn
    (log/debug "arbiter-resolver" args)
    (let [address-from-args (:user/id args)
          parent (graphql-utils/gql->clj raw-parent)
          address-from-parent (:arbiter/id parent)
          address (or address-from-args address-from-parent)]
      (<? (db/get conn (sql-helpers/merge-where arbiter-query [:ilike address :Arbiter.user/id]))))))

(defn arbiter-search-resolver [_ {:keys [:limit :offset
                                         :user/id
                                         :search-params
                                         :order-by :order-direction]
                                  :as args} _]
  (db/with-async-resolver-conn conn
    (log/debug "arbiter-search-resolver" args)
    (let [search-params (js-obj->clj-map search-params)
          categories-and (when (:category search-params) [(:category search-params)])
          categories-or nil ; Not used, switch form ...-and if want this behaviour
          skills-and (or (js->clj (:skills search-params)) [])
          skills-or nil ; Not used, switch form ...-and if want this behaviour
          min-rating (:feedback-min-rating search-params)
          max-rating (:feedback-max-rating search-params)
          min-fee (:min-fee search-params)
          max-fee (:max-fee search-params)
          min-num-feedbacks (:min-num-feedbacks search-params)
          country (:country search-params)

          query (cond-> (merge arbiter-query {:modifiers [:distinct]})
                  min-rating (sql-helpers/merge-where [:<= min-rating :Arbiter.arbiter/rating])
                  max-rating (sql-helpers/merge-where [:>= max-rating :Arbiter.arbiter/rating])
                  (nil? min-rating) (sql-helpers/merge-where :or [:= nil :Arbiter.arbiter/rating])
                  id (sql-helpers/merge-where [:ilike :Arbiter.user/id id])

                  country (sql-helpers/merge-where [:= country :Users.user/country])

                  categories-or (sql-helpers/merge-left-join :ArbiterCategory
                                                             [:= :ArbiterCategory.user/id :Arbiter.user/id])

                  categories-or (sql-helpers/merge-where [:in :ArbiterCategory.category/id categories-or])

                  categories-and (match-all {:join-table :ArbiterCategory
                                             :on-column :user/id
                                             :column :category/id
                                             :all-values categories-and})

                  skills-or (sql-helpers/merge-left-join :ArbiterSkill
                                                         [:= :ArbiterSkill.user/id :Arbiter.user/id])

                  skills-or (sql-helpers/merge-where [:in :ArbiterSkill.skill/id skills-or])
                  min-fee (sql-helpers/merge-where [:>= :Arbiter.arbiter/fee min-fee])
                  max-fee (sql-helpers/merge-where [:<= :Arbiter.arbiter/fee max-fee])
                  min-num-feedbacks (sql-helpers/merge-where
                                      [:<= min-num-feedbacks
                                       {:select [(sql/call :count :*)]
                                        :from [:JobStoryFeedbackMessage]
                                        :where [:= :JobStoryFeedbackMessage.user/id :Arbiter.user/id]}])

                  (not (empty? skills-and)) (match-all {:join-table :ArbiterSkill
                                         :on-column :user/id
                                         :column :skill/id
                                         :all-values skills-and})

                  order-by (sql-helpers/merge-order-by [[(get {:date-registered :user/date-registered
                                                               :date-updated :user/date-updated}
                                                              (graphql-utils/gql-name->kw order-by))
                                                         (or (keyword order-direction) :asc)]]))]
      (<? (paged-query conn query limit offset)))))

(defn arbiter->feedback-resolver [root {:keys [:limit :offset] :as args} {:keys [conn]}]
  (db/with-async-resolver-conn conn
    (let [{:keys [:user/id] :as arbiter} (graphql-utils/gql->clj root)
          job-story-id 2 ; FIXME: read from params
          query (sql-helpers/merge-where user-feedback-query [:and
                                                              [:= id :JobStoryFeedbackMessage.user/id]
                                                              [:= job-story-id :JobStoryFeedbackMessage.job-story/id]])]
      (log/debug "arbiter->feedback-resolver" {:arbiter arbiter :args args})
      (<? (paged-query conn query limit offset)))))

(defn feedback->to-user-type-resolver [root _ _]
  (db/with-async-resolver-conn conn
    (let [{:keys [:feedback/to-user-address] :as feedback} (graphql-utils/gql->clj root)
          q (sql-helpers/merge-where user-type-query [:= to-user-address :user/id])]
      (log/debug "feedback->to-user-type-resolver" feedback)
      (:type (<? (db/get conn q))))))

(defn feedback->from-user-type-resolver [root _ _]
  (db/with-async-resolver-conn conn
    (let [{:keys [:feedback/from-user-address] :as feedback} (graphql-utils/gql->clj root)
          q (sql-helpers/merge-where user-type-query [:= from-user-address :user/id])]
      (log/debug "feedback->from-user-type-resolver" feedback)
      (:type (<? (db/get conn q))))))

(def ^:private candidate-query
  {:select [:Candidate.*]
   :from [:Candidate]
   :join [:Users [:ilike :Users.user/id :Candidate.user/id]]})

(defn candidate-resolver [raw-parent args _]
  (db/with-async-resolver-conn conn
    (log/debug "candidate-resolver" {:args args :raw-parent raw-parent})
    (let [address-from-args (:user/id args)
          parent (graphql-utils/gql->clj raw-parent)
          address-from-parent (or
                                (:candidate/id parent)
                                (:job-story/candidate parent))
          address (or address-from-args address-from-parent)]
      (<? (db/get conn (sql-helpers/merge-where candidate-query [:ilike address :Candidate.user/id]))))))


(defn job-story->proposal-message-resolver [raw-parent args _]
  (db/with-async-resolver-conn conn
    (log/debug "job-story->proposal-message-resolver")
    (let [address-from-args (:user/id args)
          parent (graphql-utils/gql->clj raw-parent)
          proposal-message-id (:job-story/proposal-message-id parent)
          proposal-message-query {:select [:*]
                                  :from [:Message]
                                  :where [:= :Message.message/id proposal-message-id]}
          query-result (<? (db/get conn proposal-message-query))]
      (assoc query-result :__typename "JobStoryMessage"))))

(defn job-story->proposal-accepted-message-resolver [raw-parent args _]
  (db/with-async-resolver-conn conn
    (log/debug "job-story->proposal-accepted-message-resolver")
    (let [address-from-args (:user/id args)
          parent (graphql-utils/gql->clj raw-parent)
          job-story-id (:job-story/id parent)
          query {:select [:*]
                          :from [:JobStoryMessage]
                          :join [:Message [:= :Message.message/id :JobStoryMessage.message/id]]
                          :where [:and
                                  [:= :JobStoryMessage.job-story/id job-story-id]
                                  [:= :JobStoryMessage.job-story-message/type "accept-proposal"]]}
          query-result (<? (db/get conn query))]
      (assoc query-result :__typename "JobStoryMessage"))))

(defn job-story->invitation-message-resolver [raw-parent args _]
  (db/with-async-resolver-conn conn
    (log/debug "job-story->proposal-message-resolver")
    (let [address-from-args (:user/id args)
          parent (graphql-utils/gql->clj raw-parent)
          invitation-message-id (:job-story/invitation-message-id parent)
          invitation-message-query {:select [:*]
                                    :from [:Message]
                                    :where [:= :Message.message/id invitation-message-id]}
          query-result (<? (db/get conn invitation-message-query))]
      (assoc query-result :__typename "JobStoryMessage"))))

(defn job-story->invitation-accepted-message-resolver [raw-parent args _]
  (db/with-async-resolver-conn conn
    (log/debug "job-story-invitation-accepted-message-resolver")
    (let [address-from-args (:user/id args)
          parent (graphql-utils/gql->clj raw-parent)
          job-story-id (:job-story/id parent)
          query {:select [:*]
                          :from [:JobStoryMessage]
                          :join [:Message [:= :Message.message/id :JobStoryMessage.message/id]]
                          :where [:and
                                  [:= :JobStoryMessage.job-story/id job-story-id]
                                  [:= :JobStoryMessage.job-story-message/type "accept-invitation"]]}
          query-result (<? (db/get conn query))]
      (assoc query-result :__typename "JobStoryMessage"))))

(defn job-story->direct-messages-resolver [raw-parent args {:keys [:current-user :timestamp] :as ctx}]
  (db/with-async-resolver-conn conn
      (log/debug "job-story->direct-message-resolver")
      (let [parent (graphql-utils/gql->clj raw-parent)
            job-story-id (:job-story/id parent)
            user-id (:user/id current-user)
            query {:select [:*]
                   :from [:DirectMessage]
                   :join [:Message [:= :Message.message/id :DirectMessage.message/id]]
                   :where [:and
                           [:= :DirectMessage.job-story/id job-story-id]
                           [:or
                            [:= :DirectMessage.direct-message/recipient user-id]
                            [:= :Message.message/creator user-id]]]}]
        (<? (db/all conn query)))))

(defn candidate-search-resolver [_ {:keys [:limit :offset
                                           :user/id
                                           :search-params
                                           :order-by :order-direction]
                                    :as args} _]
  (db/with-async-resolver-conn conn
    (log/debug "candidate-search-resolver")
    (let [search-params (js-obj->clj-map search-params)
          categories-and (when (:category search-params) [(:category search-params)])
          categories-or nil
          skills-and (or (js->clj (:skills search-params)) [])
          skills-or nil
          min-rating (:feedback-min-rating search-params)
          max-rating (:feedback-max-rating search-params)
          min-hourly (:min-hourly-rate search-params)
          max-hourly (:max-hourly-rate search-params)
          min-num-feedbacks (:min-num-feedbacks search-params)
          country (:country search-params)

          query (cond-> (merge candidate-query {:modifiers [:distinct]})
                  min-rating (sql-helpers/merge-where [:<= min-rating :Candidate.candidate/rating])
                  max-rating (sql-helpers/merge-where [:>= max-rating :Candidate.candidate/rating])
                  (nil? min-rating) (sql-helpers/merge-where :or [:= nil :Candidate.candidate/rating])
                  id (sql-helpers/merge-where [:= :Candidate.user/id id])

                  country (sql-helpers/merge-where [:= country :Users.user/country])

                  categories-or (sql-helpers/merge-left-join :CandidateCategory
                                                             [:= :CandidateCategory.user/id :Candidate.user/id])

                  categories-or (sql-helpers/merge-where [:in :CandidateCategory.category/id categories-or])

                  categories-and (match-all {:join-table :CandidateCategory
                                             :on-column :user/id
                                             :column :category/id
                                             :all-values categories-and})

                  skills-or (sql-helpers/merge-left-join :CandidateSkill
                                                         [:= :CandidateSkill.user/id :Candidate.user/id])

                  skills-or (sql-helpers/merge-where [:in :CandidateSkill.skill/id skills-or])
                  min-hourly (sql-helpers/merge-where [:>= :Candidate.candidate/rate min-hourly])
                  max-hourly (sql-helpers/merge-where [:<= :Candidate.candidate/rate max-hourly])
                  min-num-feedbacks (sql-helpers/merge-where
                                      [:<= min-num-feedbacks
                                       {:select [(sql/call :count :*)]
                                        :from [:JobStoryFeedbackMessage]
                                        :where [:= :JobStoryFeedbackMessage.user/id :Candidate.user/id]}])

                  (not (empty? skills-and)) (match-all {:join-table :CandidateSkill
                                         :on-column :user/id
                                         :column :skill/id
                                         :all-values skills-and})

                  order-by (sql-helpers/merge-order-by [[(get {:date-registered :user/date-registered
                                                               :date-updated :user/date-updated}
                                                              (graphql-utils/gql-name->kw order-by))
                                                         (or (keyword order-direction) :asc)]]))]
      (<? (paged-query conn query limit offset)))))

(defn candidate->candidate-categories-resolver [root _ _]
  (db/with-async-resolver-conn conn
    (let [{:keys [:user/id] :as candidate} (graphql-utils/gql->clj root)]
      (log/debug "candidate->candidate-categories-resolver" candidate)
      (map :category/id (<? (db/all conn {:select [:*]
                                          :from [:CandidateCategory]
                                          :where [:= id :CandidateCategory.user/id]}))))))

(defn candidate->candidate-skills-resolver [root _ _]
  (db/with-async-resolver-conn conn
    (let [{:keys [:user/id] :as candidate} (graphql-utils/gql->clj root)]
      (log/debug "candidate->candidate-skills-resolver" candidate)
      (map :skill/id (<? (db/all conn {:select [:*]
                                       :from [:CandidateSkill]
                                       :where [:= id :CandidateSkill.user/id]}))))))

(defn participant->categories-resolver [participant-table root _ _]
  (db/with-async-resolver-conn conn
    (let [{:keys [:user/id] :as participant} (graphql-utils/gql->clj root)]
      (log/debug "participant->categories-resolver" participant)
      (map :category/id (<? (db/all conn {:select [:*]
                                          :from [participant-table]
                                          :where [:= id (keyword (str (name participant-table) ".user") :id)]}))))))

(defn participant->skills-resolver [participant-table root _ _]
  (db/with-async-resolver-conn conn
    (let [{:keys [:user/id] :as participant} (graphql-utils/gql->clj root)]
      (log/debug "participant->skills-resolver" participant)
      (map :skill/id (<? (db/all conn {:select [:*]
                                       :from [participant-table]
                                       :where [:= id (keyword (str (name participant-table) ".user") :id)]}))))))


(def candidate-job-stories-query
  {:select [:*]
   :from [:JobStory]})

(defn candidate->job-stories-resolver [root {:keys [:limit :offset] :as args} _]
  (db/with-async-resolver-conn conn
    (let [address (:user/id (graphql-utils/gql->clj root))
          query (-> candidate-job-stories-query
                  (sql-helpers/merge-where [:ilike address :JobStory.job-story/candidate]))]
      (log/debug "candidate->job-stories-resolver" {:address address :args args})
      (<? (paged-query conn query limit offset)))))

(defn- employer-job-stories-query [address]
  {:select
   [:JobStory.*]
   :from [:JobStory]
   :join [:Job [:= :Job.job/id :JobStory.job/id]]
   :where [:and [:= :Job.job/creator address] [:!= :JobStory.job-story/status "deleted"]]})

(defn employer->job-stories-resolver [root {:keys [:limit :offset] :as args} _]
  (db/with-async-resolver-conn conn
    (let [address (:user/id (graphql-utils/gql->clj root))
          query (employer-job-stories-query address)]
      (log/debug "employer->job-stories-resolver" {:address address :args args})
      (<? (paged-query conn query limit offset)))))

(defn- arbiter-arbitrations-query [id]
  {:select
   [[:JobStory.job-story/id :id]
    :JobArbiter.user/id
    :JobArbiter.job/id
    [:JobArbiter.job-arbiter/date-accepted :arbitration/date-arbiter-accepted]
    [:JobArbiter.job-arbiter/fee :arbitration/fee]
    [:JobArbiter.job-arbiter/status :arbitration/status]
    [:JobArbiter.job-arbiter/fee-currency-id :arbitration/fee-currency-id]]
   :from [:JobStory]
   :join [:Job [:= :Job.job/id :JobStory.job/id]
          :JobArbiter [:= :JobArbiter.job/id :Job.job/id]]
   :where [:and [:= :JobArbiter.user/id id] [:!= :JobStory.job-story/status "deleted"]]})

(defn arbiter->arbitrations-resolver [root {:keys [:limit :offset] :as args} _]
  (db/with-async-resolver-conn conn
    (let [address (:user/id (graphql-utils/gql->clj root))
          query (arbiter-arbitrations-query address)]
      (log/debug "arbiter->arbitrations-resolver" {:address address :args args})
      (<? (paged-query conn query limit offset)))))

(defn candidate->feedback-resolver [root {:keys [:limit :offset] :as args} _]
  (db/with-async-resolver-conn conn
    (let [{:keys [:user/id] :as candidate} (graphql-utils/gql->clj root)
          query (-> user-feedback-query
                  (sql-helpers/merge-where [:= id :JobStoryFeedbackMessage.user/id]))]
      (log/debug "candidate->feedback-resolver" {:candidate candidate :args args})
      (<? (paged-query conn query limit offset)))))

(defn job-story->employer-feedback-resolver [root _ _]
  (db/with-async-resolver-conn conn
    (let [{job-story-id :job-story/id} (graphql-utils/gql->clj root)
          query (-> user-feedback-query
                    (sql-helpers/merge-where ,,, [:= job-story-id :JobStory.job-story/id])
                    (sql-helpers/merge-where ,,, [:and
                                                  [:= :JobStoryFeedbackMessage.job-story/id job-story-id]
                                                  [:= :Job.job/creator :JobStoryFeedbackMessage.user/id]]))]
      (log/debug "job-story->employer-feedback-resolver")
      (<? (db/all conn query)))
    ))

(defn job-story->candidate-feedback-resolver [root _ _]
  (db/with-async-resolver-conn conn
    (let [{job-story-id :job-story/id} (graphql-utils/gql->clj root)]
      (log/debug "job-story->candidate-feedback-resolver")
      (<? (db/all conn (-> user-feedback-query
                           (sql-helpers/merge-where [:= job-story-id :JobStory.job-story/id])
                           (sql-helpers/merge-where [:ilike :JobStory.job-story/candidate :JobStoryFeedbackMessage.user/id])))))))

(defn job-story->arbiter-feedback-resolver [root _ _]
  (db/with-async-resolver-conn conn
    (let [{job-story-id :job-story/id} (graphql-utils/gql->clj root)
          query (-> user-feedback-query
                    (sql-helpers/merge-join ,,, :JobArbiter [:= :JobArbiter.job/id :Job.job/id])
                    (sql-helpers/merge-where ,,, [:= job-story-id :JobStory.job-story/id])
                    (sql-helpers/merge-where ,,, [:and
                                                  [:= :JobStoryFeedbackMessage.job-story/id job-story-id]
                                                  [:= :JobArbiter.user/id :JobStoryFeedbackMessage.user/id]]))]
      (log/debug "job-story->arbiter-feedback-resolver")
      (<? (db/all conn query)))))

(defn message-resolver [root args _]
  (db/with-async-resolver-conn conn
    (let [{message-id :message/id :as root-clj} (graphql-utils/gql->clj root)]
          (log/debug "message-resolver")
          (<? (db/get conn {:select [:*] :from [:Message] :where [:= :message/id message-id]})))))

(defn dispute-message-query [job-story-id invoice-id invoice-dispute-message-column]
  {:select [:Message.*]
            :from [:JobStoryInvoiceMessage]
            :join [:Message [:= invoice-dispute-message-column :Message.message/id]]
            :where [:and
                    [:= :JobStoryInvoiceMessage.job-story/id job-story-id]
                    [:= :JobStoryInvoiceMessage.invoice/ref-id invoice-id]
                    ]})

(defn invoice->sub-message-resolver [invoice-message-column root args _]
 (db/with-async-resolver-conn conn
    (let [root-obj (graphql-utils/gql->clj root)
          job-story-id (:job-story/id root-obj)
          invoice-id (:invoice/id root-obj)
          invoice-message-column (keyword :JobStoryInvoiceMessage.invoice invoice-message-column)
          message-id (:message/id root-obj)]
          (log/debug (str "invoice->sub-message-resolver for " invoice-message-column " job-story-id:") job-story-id)
          (<? (db/get conn (dispute-message-query job-story-id invoice-id invoice-message-column))))))

(defn employer-search-resolver [_ {:keys [:limit :offset
                                          :user/id
                                          :professional-title
                                          :order-by :order-direction]
                                   :as args} _]
  (db/with-async-resolver-conn conn
    (log/debug "employer-search-resolver" args)
    (let [query (cond-> employer-query

                  id (sql-helpers/merge-where [:= id :Employer.user/id])

                  professional-title (sql-helpers/merge-where [:= professional-title :Employer.employer/professional-title])

                  order-by (sql-helpers/merge-order-by [[(get {:date-registered :user/date-registered
                                                               :date-updated :user/date-updated}
                                                              (graphql-utils/gql-name->kw order-by))
                                                         (or (keyword order-direction) :asc)]]))]
      (<? (paged-query conn query limit offset)))))

(def ^:private job-query {:select [:Job.job/id
                                   :Job.job/bid-option
                                   :Job.job/category
                                   :Job.job/creator
                                   :Job.job/date-created
                                   :Job.job/date-updated
                                   :Job.job/description
                                   :Job.job/estimated-project-length
                                   :Job.job/invitation-only?
                                   :Job.job/required-experience-level
                                   :Job.job/required-availability
                                   :Job.job/status
                                   :Job.job/title

                                   :Job.job/token-type
                                   :Job.job/token-amount
                                   :Job.job/token-address
                                   :Job.job/token-id

                                   [:Job.job/creator :job/employer-address]
                                   [:JobArbiter.user/id :job/accepted-arbiter-address]]
                          :from [:Job]
                          :left-join [:JobArbiter [:= :JobArbiter.job/id :Job.job/id]]})

(defn job-resolver [parent {:keys [:contract :job/id] :as args} _]
  (db/with-async-resolver-conn conn
    (log/debug "job-resolver")
    (let [contract-from-parent (:job/id (graphql-utils/gql->clj parent))
          contract-from-args (:job/id args)
          contract-address (or contract-from-parent contract-from-args)
          job (<? (db/get conn (sql-helpers/merge-where job-query [:= contract-address :Job.job/id])))
          skills (<? (db/all conn {:select [:JobSkill.skill/id] :from [:JobSkill] :where [:= :JobSkill.job/id (:job/id job)]}))
          job-full (assoc-in job [:job/required-skills] (map :skill/id skills))]
      job-full)))

(defn job->required-skills-resolver [parent args _]
  (db/with-async-resolver-conn conn
    (let [job-id (:job/id (graphql-utils/gql->clj parent))]
    (map :skill/id (<? (db/all conn {:select [:JobSkill.skill/id] :from [:JobSkill] :where [:= :JobSkill.job/id job-id]}))))))

(def ^:private job-search-query {:select [:Job.*]
                                 :from [:Job]
                                 :join [:Employer [:= :Employer.user/id :Job.job/creator]]
                                 :left-join [:JobStory [:= :JobStory.job/id :Job.job/id]
                                             :JobArbiter [:ilike :JobArbiter.job/id :Job.job/id]]})

(defn job-search-resolver [_ {:keys [:job/id
                                     :search-params
                                     :limit
                                     :offset
                                     :order-by
                                     :order-direction]
                                  :as args} _]
  (log/debug "job-search-resolver" args)
  (db/with-async-resolver-conn conn
    (let [search-params (js-obj->clj-map search-params)
          max-rating (:feedback-max-rating search-params)
          min-rating (:feedback-min-rating search-params)
          skills (or (js->clj (:skills search-params)) [])
          category (:category search-params)
          min-hourly-rate (:min-hourly-rate search-params)
          max-hourly-rate (:max-hourly-rate search-params)
          min-num-feedbacks (:min-num-feedbacks search-params)
          creator (:creator search-params)
          arbiter (:arbiter search-params)
          payment-type (:payment-type search-params)

          experience-level (:experience-level search-params)
          ordered-experience-levels ["beginner" "intermediate" "expert"]
          suitable-levels (drop-while #(not (= experience-level %)) ordered-experience-levels)

          query (cond-> (merge job-search-query {:modifiers [:distinct]})
                  min-rating (sql-helpers/merge-where [:<= min-rating :Employer.employer/rating])
                  max-rating (sql-helpers/merge-where [:>= max-rating :Employer.employer/rating])
                  (nil? min-rating) (sql-helpers/merge-where :or [:= nil :Employer.employer/rating])

                  creator (sql-helpers/merge-where [:ilike creator :Job.job/creator])
                  arbiter (sql-helpers/merge-where [:ilike arbiter :JobArbiter.user/id])

                  ; The case for OR-ing the skills
                  ; (not (empty? skills)) (sql-helpers/merge-where [:in :JobSkill.skill/id skills])

                  ; The case for AND-ing the skills
                  (not (empty? skills)) (match-all {:join-table :JobSkill
                                                    :on-column :job/id
                                                    :column :skill/id
                                                    :all-values skills})
                  category (sql-helpers/merge-where [:= :Job.job/category category])

                  min-hourly-rate (sql-helpers/merge-where [:<= min-hourly-rate :JobStory.job-story/proposal-rate])
                  max-hourly-rate (sql-helpers/merge-where [:>= max-hourly-rate :JobStory.job-story/proposal-rate])
                  min-num-feedbacks (sql-helpers/merge-where
                                      [:<= min-num-feedbacks
                                       {:select [(sql/call :count :*)]
                                        :from [:JobStoryFeedbackMessage]
                                        :where [:= :JobStoryFeedbackMessage.user/id :Job.job/creator]}])
                  payment-type (sql-helpers/merge-where [:= :Job.job/bid-option payment-type])
                  experience-level (sql-helpers/merge-where [:in :Job.job/required-experience-level suitable-levels]))]
      (<? (paged-query conn query limit offset)))))

(def ^:private job-story-query {:select [:*]
                                :from [:JobStory]
                                :join [:Job [:= :Job.job/id :JobStory.job/id]]})

(defn job->job-stories-resolver [root {:keys [:limit :offset] :as args} _]
  (db/with-async-resolver-conn conn
    (let [{:keys [:job/id] :as job} (graphql-utils/gql->clj root)]
      (log/debug "job->job-stories-resolver" {:job job :args args})
      (<? (paged-query conn (sql-helpers/merge-where job-story-query [:= id :JobStory.job/id]) limit offset)))))

(defn job-story-resolver [root args _]
  (db/with-async-resolver-conn conn
    (log/debug "job-story-resolver" args)
    (let [job-story-from-root (:job-story/id (graphql-utils/gql->clj root))
          job-story-from-args (:job-story/id args)
          job-story-id (or job-story-from-args job-story-from-root)]
      (<? (db/get conn (-> job-story-query
                           (sql-helpers/merge-where [:= job-story-id :JobStory.job-story/id])))))))

(defn job-story-list-resolver [parent args _]
  (db/with-async-resolver-conn conn
    (log/debug "job-story-list-resolver" args)
    (let [contract (:job-contract args)
          query {:select [:*]
                 :from [:JobStory]
                 :where [:= :JobStory.job/id contract]}]
      (<? (db/all conn query)))))

(defn job-story-search-resolver [_ args _]
  (db/with-async-resolver-conn conn
    (log/debug "job-story-search-resolver" args)
    (let [contract (:job/id args)
          query {:select [:*]
                 :from [:JobStory]
                 :where [:= :JobStory.job/id contract]}
          limit (:limit args)
          offset (:offset args)]
      (<? (paged-query conn query limit offset)))))


(def ^:private invoice-query {:modifiers [:distinct-on :JobStory.job-story/id :JobStoryInvoiceMessage.invoice/ref-id]
                              :select [[(sql/call :concat :JobStory.job/id (sql/raw "'-'") :invoice/ref-id) :id]
                                       [:JobStoryInvoiceMessage.invoice/ref-id :invoice/id]
                                       :JobStoryInvoiceMessage.message/id
                                       :JobStoryInvoiceMessage.invoice/status
                                       :JobStoryInvoiceMessage.invoice/date-paid
                                       :JobStoryInvoiceMessage.invoice/date-requested
                                       :JobStoryInvoiceMessage.invoice/amount-requested
                                       :JobStoryInvoiceMessage.invoice/amount-paid
                                       :JobStoryInvoiceMessage.invoice/hours-worked
                                       :JobStoryInvoiceMessage.invoice/hourly-rate
                                       :JobStoryInvoiceMessage.invoice/dispute-raised-message-id
                                       :JobStoryInvoiceMessage.invoice/dispute-resolved-message-id
                                       :JobStory.job-story/id
                                       :Job.job/id]
                              :from [:JobStoryInvoiceMessage]
                              :join [:JobStory [:= :JobStory.job-story/id :JobStoryInvoiceMessage.job-story/id]
                                     :Job [:= :Job.job/id :JobStory.job/id]]})

(defn invoice-search-resolver [_ {:keys [:employer :candidate :limit :offset] :as args} _]
  (db/with-async-resolver-conn conn
    (log/debug "invoice-search-resolver" {:args args})
    (let [query (cond-> invoice-query
                  employer (sql-helpers/merge-where [:ilike :Job.job/creator employer])
                  candidate (sql-helpers/merge-where [:ilike :JobStory.job-story/candidate candidate]))]
      (<? (paged-query conn query limit offset)))))

(defn invoice-resolver [_ {invoice-id :invoice/id  job-id :job/id :as args} _]
  (db/with-async-resolver-conn conn
    (log/debug "invoice-resolver" {:args args})
    (<? (db/get conn (-> invoice-query
                       (sql-helpers/merge-where [:and
                                                 [:= job-id :Job.job/id]
                                                 [:= invoice-id :JobStoryInvoiceMessage.invoice/ref-id]]))))))

(def ^:private dispute-query {:modifiers [:distinct-on :JobStory.job-story/id :JobStoryInvoiceMessage.invoice/ref-id]
                              :select [[(sql/call :concat :JobStory.job/id (sql/raw "'-'") :invoice/ref-id) :id]
                                       [:JobStoryInvoiceMessage.invoice/ref-id :invoice/id]
                                       :JobStoryInvoiceMessage.message/id
                                       :Job.job/id
                                       :JobStory.job-story/id

                                       :JobStoryInvoiceMessage.invoice/amount-requested
                                       :JobStoryInvoiceMessage.invoice/amount-paid

                                       [:JobStory.job-story/candidate :candidate/id]
                                       [:Job.job/creator :employer/id]
                                       [:JobArbiter.user/id :arbiter/id]

                                       [:raised-message.message/text :dispute/reason]
                                       [:resolved-message.message/text :dispute/resolution]
                                       [:raised-message.message/date-created :dispute/date-created]
                                       [:resolved-message.message/date-created :dispute/date-resolved]
                                       ]
                              :from [:JobStoryInvoiceMessage]
                              :join [:JobStory [:= :JobStory.job-story/id :JobStoryInvoiceMessage.job-story/id]
                                     :Job [:= :Job.job/id :JobStory.job/id]
                                     :JobArbiter [:ilike :Job.job/id :JobArbiter.job/id]
                                     [:Message :raised-message] [:= :raised-message.message/id :JobStoryInvoiceMessage.invoice/dispute-raised-message-id]]
                              :left-join [[:Message :resolved-message] [:= :resolved-message.message/id :JobStoryInvoiceMessage.invoice/dispute-resolved-message-id]]})

(defn dispute-search-resolver [_ {:keys [:arbiter :employer :candidate :limit :offset] :as args} _]
  (db/with-async-resolver-conn conn
    (log/debug "invoice-search-resolver" {:args args})
    (let [query (cond-> dispute-query
                  employer (sql-helpers/merge-where [:ilike :Job.job/creator employer])
                  candidate (sql-helpers/merge-where [:ilike :JobStory.job-story/candidate candidate])
                  arbiter (sql-helpers/merge-where [:ilike :JobArbiter.user/id arbiter]))]
      (<? (paged-query conn query limit offset)))))

(defn job-story->invoices-resolver [root {:keys [:limit :offset] :as args} _]
  (db/with-async-resolver-conn conn
    (let [parsed-root (graphql-utils/gql->clj root)
          job-story-id (:job-story/id (graphql-utils/gql->clj root))
          query (-> invoice-query
                  (sql-helpers/merge-where [:= job-story-id :JobStory.job-story/id]))
          result-pages (<? (paged-query conn query limit offset))]
      (log/debug "job-story->invoices-resolver RESULT-PAGES" job-story-id " | " result-pages)
      result-pages)))

(defn job->invoices-resolver [root {:keys [:limit :offset] :as args} _]
  (db/with-async-resolver-conn conn
    (let [parsed-root (graphql-utils/gql->clj root)
          job-id (:job/id (graphql-utils/gql->clj root))
          query (-> invoice-query
                  (sql-helpers/merge-where [:= job-id :Job.job/id]))
          result-pages (<? (paged-query conn query limit offset))]
      (log/debug "job->invoices-resolver RESULT-PAGES" job-id " | " result-pages)
      result-pages)))

(defn sign-in-mutation [_ {:keys [:data :data-signature] :as input} {:keys [config]}]
  (try-catch-throw
    (let [sign-in-secret (-> config :graphql :sign-in-secret)
          user-address (authorization/recover-personal-signature data data-signature)
          jwt (authorization/create-jwt user-address sign-in-secret)]
      (log/debug "sign-in-mutation" {:input input})
      {:jwt jwt :user/id user-address})))


(defn send-message-mutation [_ message-params {:keys [:current-user :timestamp]}]
  (db/with-async-resolver-tx conn
    (log/debug "send-message-mutation" )
    (let [job-story-id (:job-story/id message-params)
          recipient (:to message-params)
          text (:text message-params)
          job-story-message-type (graphql-utils/gql-name->kw (:job-story-message/type message-params))
          message-type (or (graphql-utils/gql-name->kw (:message/type message-params)) :direct-message)]
      (<? (ethlance-db/add-message conn {:job-story/id job-story-id
                                         :message/type message-type
                                         :job-story-message/type job-story-message-type
                                         :message/date-created timestamp
                                         :message/creator (:user/id current-user)
                                         :direct-message/recipient recipient
                                         :message/text text}))
      true)))

; This is done by employer (invitation)
(defn send-proposal-message-mutation [_ {:keys [:to :text]} {:keys [:current-user :timestamp]}]
  (db/with-async-resolver-tx conn
    (<? (ethlance-db/add-message conn {:message/type :direct-message
                                       :message/date-created timestamp
                                       :message/creator (:user/id current-user)
                                       :message/text text
                                       :direct-message/recipient to})))) ; FIXME: this should be job-story-message,

(defn leave-feedback-mutation [_ {:keys [:job-story/id :text :rating :to] :as params} {:keys [current-user timestamp]}]
  ; Change JobStory status to "ended-by-feedback" when employer or candidate sends feedback
  (db/with-async-resolver-tx conn
    (let [job-story-id id
          current-user-id (:user/id current-user)
          employer (<? (ethlance-db/get-employer-id-by-job-story-id conn job-story-id))
          candidate (<? (ethlance-db/get-candidate-id-by-job-story-id conn job-story-id))
          participants (set [employer candidate])
          feedback-from-participants? (contains? participants current-user-id)
          previous-status (:status (<? (db/get conn
                                               {:select [[:JobStory.job-story/status :status]]
                                                :from [:JobStory]
                                                :where [:= :job-story/id job-story-id]})))
          arbiter-feedback-before-ending? (and (not= previous-status "ended-by-feedback")
                                               (not feedback-from-participants?))]

      (when feedback-from-participants?
       (<? (ethlance-db/update-job-story-status conn job-story-id "finished")))
      (when arbiter-feedback-before-ending? (throw (js/Error. "Arbiter can't leave feedback before job contract has been ended")))
      (<? (ethlance-db/add-message conn {:message/type :job-story-message
                                         :job-story-message/type :feedback
                                         :job-story/id job-story-id
                                         :message/date-created timestamp
                                         :message/creator current-user-id
                                         :message/text text
                                         :feedback/rating rating
                                         :user/id to}))
      true
      )))

(defn update-employer-mutation [_ {:keys [input]} {:keys [timestamp]}]
  (db/with-async-resolver-tx conn
    (let [{:user/keys [id]} input
          response {:user/id id
                    :user/date-updated timestamp
                    :employer/date-updated timestamp}]
      (log/debug "update-employer-mutation" {:input input :response response})
      (<? (ethlance-db/upsert-user! conn (-> input
                                             (assoc :user/type :employer)
                                             (merge response))))
      response)))

(defn update-candidate-mutation [_ {:keys [input]} {:keys [timestamp]}]
  (db/with-async-resolver-tx conn
    (let [{:user/keys [id]} input
          response {:user/id id
                    :user/date-updated timestamp
                    :candidate/date-updated timestamp}]
      (log/debug "update-candidate-mutation" {:input input :response response})
      (<? (ethlance-db/upsert-user! conn (-> input
                                             (assoc :user/type :candidate)
                                             (merge response))))
      response)))

(defn update-arbiter-mutation [_ {:keys [input]} {:keys [timestamp]}]
  (db/with-async-resolver-tx conn
    (let [{:user/keys [id]} input
          response {:user/id id
                    :user/date-updated timestamp
                    :arbiter/date-updated timestamp}]
      (log/debug "arbiter-candidate-mutation" {:input input :response response})
      (<? (ethlance-db/upsert-user! conn (-> input
                                             (assoc :user/type :arbiter)
                                             (merge response))))
      response)))

(defn create-job-proposal-mutation [_ gql-params {:keys [current-user timestamp]}]
  (db/with-async-resolver-conn conn
    (let [input (js-obj->clj-map (:input gql-params))
          message-params {:message/type :job-story-message
                          :job-story-message/type :proposal
                          :job/id (:contract input)
                          :message/date-created timestamp
                          :message/creator (:user/id current-user)
                          :message/text (:text input)
                          :job-story/proposal-rate (:rate input)
                          :job-story/proposal-rate-currency-id (:rate-currency-id input)}
          job-story-id (:job-story/id (<? (ethlance-db/add-message conn message-params)))]
      (<? (db/get conn {:select [:*] :from [:JobStory] :where [:= :job-story/id  job-story-id]})))))


(defn remove-job-proposal-mutation [_ gql-params {:keys [current-user timestamp]}]
  (db/with-async-resolver-conn conn
    (let [message-params {:job-story/status "deleted" :job-story/id (:job-story/id gql-params)}]
      (first (<? (ethlance-db/update-row! conn :JobStory message-params))))))

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
          user {:user/id (:user/id current-user)
                :user/name name
                :user/github-username login
                :user/email email
                :user/country location}]

      (<? (ethlance-db/upsert-user-social-accounts! conn (select-keys user [:user/id :user/github-username])))

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
          user {:user/id (:user/id current-user)
                :user/name (str localizedFirstName " " localizedLastName)
                :user/linkedin-username id
                :user/email email}]

      (<? (ethlance-db/upsert-user-social-accounts! conn (select-keys user [:user/id :user/linkedin-username])))

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
                            :jobSearch job-search-resolver
                            :jobStory job-story-resolver
                            :jobStoryList job-story-list-resolver
                            :jobStorySearch job-story-search-resolver
                            :invoiceSearch invoice-search-resolver
                            :disputeSearch dispute-search-resolver}
                    :Job {:jobStories job->job-stories-resolver
                          :job_employer job->employer-resolver
                          :job_arbiter job->arbiter-resolver
                          :tokenDetails job->token-details-resolver
                          :invoices job->invoices-resolver
                          :invoice invoice-resolver
                          :job_requiredSkills job->required-skills-resolver}
                    :JobStory {:jobStory_employerFeedback job-story->employer-feedback-resolver
                               :jobStory_candidateFeedback job-story->candidate-feedback-resolver
                               :jobStory_arbiterFeedback job-story->arbiter-feedback-resolver
                               :jobStory_invoices job-story->invoices-resolver
                               :job job-resolver
                               :candidate candidate-resolver
                               :proposalMessage job-story->proposal-message-resolver
                               :proposalAcceptedMessage job-story->proposal-accepted-message-resolver
                               :directMessages (require-auth job-story->direct-messages-resolver)
                               :invitationMessage job-story->invitation-message-resolver
                               :invitationAcceptedMessage job-story->invitation-accepted-message-resolver}
                    :User {:user_languages user->languages-resolvers
                           :user_isRegisteredCandidate user->is-registered-candidate-resolver
                           :user_isRegisteredEmployer user->is-registered-employer-resolver
                           :user_isRegisteredArbiter user->is-registered-arbiter-resolver}
                    :Candidate {:candidate_feedback candidate->feedback-resolver
                                :candidate_categories (partial participant->categories-resolver :CandidateCategory)
                                :candidate_skills (partial participant->skills-resolver :CandidateSkill)
                                :jobStories candidate->job-stories-resolver
                                :user participant->user-resolver}
                    :Employer {:employer_feedback employer->feedback-resolver
                               :jobStories employer->job-stories-resolver
                               :user participant->user-resolver}
                    :Arbiter {:arbiter_feedback arbiter->feedback-resolver
                              :arbiter_categories (partial participant->categories-resolver :ArbiterCategory)
                              :arbiter_skills (partial participant->skills-resolver :ArbiterSkill)
                              :arbitrations arbiter->arbitrations-resolver
                              :user participant->user-resolver}
                    :Arbitration {:job job-resolver}
                    :Dispute {:job job-resolver
                              :jobStory job-story-resolver
                              :candidate candidate-resolver
                              :employer employer-resolver
                              :arbiter arbiter-resolver}

                    :Feedback {:feedback_toUserType feedback->to-user-type-resolver
                               :feedback_fromUser feedback->from-user-resolver
                               :message message-resolver
                               :feedback_fromUserType feedback->from-user-type-resolver}
                    :JobStoryMessage {:creator user-resolver}
                    :DirectMessage {:creator user-resolver}
                    :Invoice {:jobStory job-story-resolver
                              :creationMessage message-resolver
                              :paymentMessage (partial invoice->sub-message-resolver :payment-message-id)
                              :disputeRaisedMessage (partial invoice->sub-message-resolver :dispute-raised-message-id)
                              :disputeResolvedMessage (partial invoice->sub-message-resolver :dispute-resolved-message-id)}
                    :Mutation {:signIn sign-in-mutation
                               :sendMessage (require-auth send-message-mutation)
                               :leaveFeedback (require-auth leave-feedback-mutation)
                               ;; TODO : do require auth
                               :updateEmployer (require-auth update-employer-mutation)
                               :updateCandidate (require-auth (validate-input update-candidate-mutation))
                               :updateArbiter (require-auth update-arbiter-mutation)
                               :createJobProposal (require-auth create-job-proposal-mutation)
                               :removeJobProposal (require-auth remove-job-proposal-mutation)
                               :replayEvents replay-events
                               :githubSignUp (require-auth github-signup-mutation)
                               :linkedinSignUp (require-auth linkedin-signup-mutation)}
                    ; :Date ; TODO: https://www.apollographql.com/docs/apollo-server/schema/custom-scalars/#example-the-date-scalar
                    })
