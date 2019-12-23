(ns ethlance.server.graphql.resolvers
  (:require [cljs.nodejs :as nodejs]
            [district.server.db :as db]
            [ethlance.server.graphql.authorization :as authorization]
            [district.shared.error-handling :refer [try-catch-throw]]
            [ethlance.server.db :as ethlance-db]
            [ethlance.shared.graphql.utils :as graphql-utils]
            [honeysql.core :as sql]
            [honeysql.helpers :as sql-helpers]
            [taoensso.timbre :as log :refer [spy]]))

(defn- paged-query
  [query limit offset]
  (let [paged-query (cond-> query
                      limit (assoc :limit limit)
                      offset (assoc :offset offset))
        total-count (count (db/all query))
        result (db/all paged-query)
        end-cursor (cond-> (count result)
                     offset (+ offset))]
    {:items result
     :total-count total-count
     :end-cursor end-cursor
     :has-next-page (< end-cursor total-count)}))

(defn user-search-resolver [_ {:keys [:limit :offset :user/address :user/full-name :user/user-name :order-by :order-direction]
                               :as args} _]
  (try-catch-throw
   (log/debug "user-search-resolver" {:args args})
   (let [query (cond-> {:select [:*]
                        :from [:User]}

                 address (sql-helpers/merge-where [:= :User.user/address address])

                 full-name (sql-helpers/merge-where [:= :User.user/full-name full-name])

                 user-name (sql-helpers/merge-where [:= :User.user/user-name user-name])

                 order-by (sql-helpers/merge-order-by [[(get {:date-created :user/date-created
                                                              :date-updated :user/date-updated
                                                              ;; random order as a placeholder for ordering
                                                              :order-by/random (sql/call :random)}
                                                             (graphql-utils/gql-name->kw order-by))
                                                        (or (keyword order-direction) :asc)]])

                 ;; order-by (sql-helpers/merge-order-by [[:User.user/address :asc]])

                 )]
     (paged-query query limit offset))))

(defn user-resolver [_ {:keys [:user/address] :as args} _]
  (try-catch-throw
   (log/debug "user-resolver" args)
   (db/get {:select [:*]
            :from [:User]
            :where [:= address :User.user/address]})))

(defn user->is-registered-candidate-resolver [root _ _]
  (try-catch-throw
   (let [{:keys [:user/address] :as user} (graphql-utils/gql->clj root)]
     (log/debug "user->is-registered-candidate-resolver" {:user user})
     (not (= 0 (:count (db/get {:select [[(sql/call :count :*) :count]]
                                :from [:Candidate]
                                :where [:= address :Candidate.user/address]})))))))

(defn user->is-registered-employer-resolver [root _ _]
  (try-catch-throw
   (let [{:keys [:user/address] :as user} (graphql-utils/gql->clj root)]
     (log/debug "user->is-registered-employer-resolver" {:user user})
     (not (= 0 (:count (db/get {:select [[(sql/call :count :*) :count]]
                                :from [:Employer]
                                :where [:= address :Employer.user/address]})))))))

(defn user->is-registered-arbiter-resolver [root _ _]
  (try-catch-throw
   (let [{:keys [:user/address] :as user} (graphql-utils/gql->clj root)]
     (log/debug "user->is-registered-arbiter-resolver" {:user user})
     (not (= 0 (:count (db/get {:select [[(sql/call :count :*) :count]]
                                :from [:Arbiter]
                                :where [:= address :Arbiter.user/address]})))))))

(defn user->languages-resolvers [root _ _]
  (try-catch-throw
   (let [{:keys [:user/address] :as user} (graphql-utils/gql->clj root)]
     (log/debug "user->languages-resolvers" {:user user})
     (map :language/id
          (db/all {:select [:*]
                   :from [:UserLanguage]
                   :where [:= address :UserLanguage.user/address]})))))

(def ^:private candidate-query
  {:select [[:User.user/address :user/address]
            [:Candidate.candidate/professional-title :candidate/professional-title]
            [:Candidate.candidate/bio :candidate/bio]
            [:User.user/date-registered :candidate/date-registered]]
   :from [:Candidate]
   :join [:User [:= :User.user/address :Candidate.user/address]]})

(defn candidate-resolver [_ {:keys [:user/address] :as args} _]
  (try-catch-throw
   (log/debug "candidate-resolver" args)
   (db/get (sql-helpers/merge-where candidate-query [:= address :Candidate.user/address]))))

;; TODO
(defn candidate-search-resolver [_ {:keys [:limit :offset
                                           :user/address
                                           :categories-and
                                           :categories-or
                                           :skills-and
                                           :skills-or
                                           :professional-title
                                           :order-by :order-direction]
                                    :as args} _]
  (try-catch-throw
   (log/debug "candidate-search-resolver" {:args args})
   (let [query (cond-> #_{:select [:*]
                        :from [:Candidate]
                        :modifiers [:distinct]}
                 (merge candidate-query {:modifiers [:distinct]})

                 address (sql-helpers/merge-where [:= :Candidate.user/address address])

                 ;; TODO : join where in
                 categories-or (sql-helpers/merge-left-join :CandidateCategory
                                                       [:= :CandidateCategory.user/address :Candidate.user/address])

                 categories-or (sql-helpers/merge-where [:in :CandidateCategory.category/id categories-or])



                 )]
     (log/debug "@@@ candidate-search" query)
     (paged-query query limit offset))))

(defn candidate->candidate-categories-resolver [root _ _]
  (try-catch-throw
   (let [{:keys [:user/address] :as candidate} (graphql-utils/gql->clj root)]
     (log/debug "candidate->candidate-categories-resolver" {:candidate candidate})
     (map :category/id (db/all {:select [:*]
                                :from [:CandidateCategory]
                                :where [:= address :CandidateCategory.user/address]})))))

(defn candidate->candidate-skills-resolver [root _ _]
  (try-catch-throw
   (let [{:keys [:user/address] :as candidate} (graphql-utils/gql->clj root)]
     (log/debug "candidate->candidate-skills-resolver" {:candidate candidate})
     (map :skill/id (db/all {:select [:*]
                             :from [:CandidateSkill]
                             :where [:= address :CandidateSkill.user/address]})))))

(defn candidate->feedback-resolver [root {:keys [:limit :offset] :as args} _]
  (try-catch-throw
   (let [{:keys [:user/address] :as candidate} (graphql-utils/gql->clj root)
         query {:select [:Job.job/id :Contract.contract/id :feedback/rating
                         [:JobCreator.user/address :feedback/from-user-address]
                         [:ContractCandidate.user/address :feedback/to-user-address]
                         [:Message.message/date-created :feedback/date-created]
                         [:Message.message/text :feedback/text]]
                :from [:Feedback]
                :join [:Contract [:= :Feedback.contract/id :Contract.contract/id]
                       :Job [:= :Contract.job/id :Job.job/id]
                       :Message [:= :Message.message/id :Feedback.message/id]
                       :JobCreator [:= :JobCreator.job/id :Job.job/id]
                       :ContractCandidate [:= :ContractCandidate.contract/id :Contract.contract/id]]
                :where [:= address :ContractCandidate.user/address]}]
     (log/debug "candidate->feedback-resolver" {:candidate candidate :args args})
     (paged-query query limit offset))))

(defn sign-in-mutation [_ {:keys [:input]} {:keys [:config]}]
  "Graphql sign-in mutation. Given `data` and `data-signature`
  recovers user address. If successful returns a JWT containing the user address."
  (try-catch-throw
   (let [sign-in-secret (-> config :graphql :sign-in-secret)
         {:keys [:data-signature :data] :as input} (graphql-utils/gql-input->clj input)
         user-address (authorization/recover-personal-signature data data-signature)
         jwt (authorization/create-jwt user-address sign-in-secret)]
     (log/debug "sign-in-mutation" {:input input})
     jwt)))

(defn require-auth [next]
  "Given a `resolver` fn returns a wrapped resolver.
  It will call the given `resolver` if the request contains currentUser,
  see `ethlance.server.graphql.mutations.sign-in/session-middleware`.
  It will throw a error otherwise."
  (fn [root args {:keys [:current-user] :as context} info]
    (if-not current-user
      (throw (js/Error. "Authentication required"))
      (next root args context info))))

(def resolvers-map {:Query {:user user-resolver
                            :userSearch user-search-resolver
                            :candidate candidate-resolver
                            :candidateSearch candidate-search-resolver
                            }
                    :User {:user_languages user->languages-resolvers
                           :user_isRegisteredCandidate user->is-registered-candidate-resolver
                           :user_isRegisteredEmployer user->is-registered-employer-resolver
                           :user_isRegisteredArbiter user->is-registered-arbiter-resolver}
                    :Candidate {:candidate_feedback candidate->feedback-resolver
                                :candidate_categories candidate->candidate-categories-resolver
                                :candidate_skills candidate->candidate-skills-resolver

                                }
                    :Mutation {:signIn sign-in-mutation}})
