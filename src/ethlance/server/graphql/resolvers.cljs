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

;; (defn- cons-and [k v]
;;   (cons :and (map (fn [elem]
;;                     [:= k elem])
;;                   v)))

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
   (log/debug "user-search-resolver" args)
   (let [query (cond-> {:select [:*]
                        :from [:User]}

                 address (sql-helpers/merge-where [:= :User.user/address address])

                 full-name (sql-helpers/merge-where [:= :User.user/full-name full-name])

                 user-name (sql-helpers/merge-where [:= :User.user/user-name user-name])

                 order-by (sql-helpers/merge-order-by [[(get {:date-registered :user/date-registered
                                                              :date-updated :user/date-updated}
                                                             (graphql-utils/gql-name->kw order-by))
                                                        (or (keyword order-direction) :asc)]]))]
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
     (log/debug "user->is-registered-candidate-resolver" user)
     (not (= 0 (:count (db/get {:select [[(sql/call :count :*) :count]]
                                :from [:Candidate]
                                :where [:= address :Candidate.user/address]})))))))

(defn user->is-registered-employer-resolver [root _ _]
  (try-catch-throw
   (let [{:keys [:user/address] :as user} (graphql-utils/gql->clj root)]
     (log/debug "user->is-registered-employer-resolver" user)
     (not (= 0 (:count (db/get {:select [[(sql/call :count :*) :count]]
                                :from [:Employer]
                                :where [:= address :Employer.user/address]})))))))

(defn user->is-registered-arbiter-resolver [root _ _]
  (try-catch-throw
   (let [{:keys [:user/address] :as user} (graphql-utils/gql->clj root)]
     (log/debug "user->is-registered-arbiter-resolver" user)
     (not (= 0 (:count (db/get {:select [[(sql/call :count :*) :count]]
                                :from [:Arbiter]
                                :where [:= address :Arbiter.user/address]})))))))

(defn user->languages-resolvers [root _ _]
  (try-catch-throw
   (let [{:keys [:user/address] :as user} (graphql-utils/gql->clj root)]
     (log/debug "user->languages-resolvers" user)
     (map :language/id
          (db/all {:select [:*]
                   :from [:UserLanguage]
                   :where [:= address :UserLanguage.user/address]})))))

(def ^:private employer-query {:select [[:Employer.user/address :user/address]
                                        [:Employer.employer/professional-title :employer/professional-title]
                                        [:Employer.employer/bio :employer/bio]
                                        [:User.user/date-registered :employer/date-registered]]
                               :from [:Employer]
                               :join [:User [:= :User.user/address :Employer.user/address]]})

(defn employer-resolver [_ {:keys [:user/address] :as args} _]
  (try-catch-throw
   (log/debug "employer-resolver" args)
   (db/get (sql-helpers/merge-where employer-query [:= address :Employer.user/address]))))

(def ^:private employer-feedback-query {:select [:Job.job/id :Contract.contract/id :Message.message/id :Feedback.feedback/rating
                                                 [:JobCreator.user/address :feedback/to-user-address]
                                                 [:ContractCandidate.user/address :feedback/from-user-address]
                                                 [:Message.message/date-created :feedback/date-created]
                                                 [:Message.message/text :feedback/text]]
                                        :from [:Feedback]
                                        :join [:Contract [:= :Feedback.contract/id :Contract.contract/id]
                                               :Job [:= :Contract.job/id :Job.job/id]
                                               :Message [:= :Message.message/id :Feedback.message/id]
                                               :JobCreator [:= :JobCreator.job/id :Job.job/id]
                                               :ContractCandidate [:= :ContractCandidate.contract/id :Contract.contract/id]]
                                        :where [:and
                                                [:= :Message.message/creator :ContractCandidate.user/address]
                                                [:= "EMPLOYER FEEDBACK" :Message.message/type]]})

(defn employer->feedback-resolver [root {:keys [:limit :offset] :as args} _]
  (try-catch-throw
   (let [{:keys [:user/address] :as employer} (graphql-utils/gql->clj root)
         query (sql-helpers/merge-where employer-feedback-query [:= address :JobCreator.user/address])]
     (log/debug "employer->feedback-resolver" {:employer employer :args args})
     (paged-query query limit offset))))

(defn contract->employer-feedback-resolver [root _ _]
  (try-catch-throw
   (let [{:keys [:contract/id] :as contract} (graphql-utils/gql->clj root)]
     (log/debug "contract->employer-feedback-resolver" {:contract contract})
     (db/get (-> employer-feedback-query
                 (sql-helpers/merge-where [:= id :Contract.contract/id])
                 (sql-helpers/merge-where [:= (:job/id contract) :Job.job/id]))))))

(def ^:private arbiter-query {:select [:Arbiter.user/address
                                       :Arbiter.arbiter/bio
                                       :Arbiter.arbiter/fee
                                       :Arbiter.arbiter/fee-currency-id
                                       [:User.user/date-registered :arbiter/date-registered]]
                              :from [:Arbiter]
                              :join [:User [:= :User.user/address :Arbiter.user/address]]})

(defn arbiter-resolver [_ {:keys [:user/address] :as args} _]
  (try-catch-throw
   (log/debug "arbiter-resolver" args)
   (db/get (sql-helpers/merge-where arbiter-query [:= address :Arbiter.user/address]))))

(defn arbiter-search-resolver [_ {:keys [:limit :offset
                                         :user/address
                                         :order-by :order-direction]
                                  :as args} _]
  (try-catch-throw
   (log/debug "arbtier-search-resolver" args)
   (let [query (cond-> arbiter-query
                 address (sql-helpers/merge-where [:= address :Employer.user/address])

                 order-by (sql-helpers/merge-order-by [[(get {:date-created :user/date-created
                                                              :date-updated :user/date-updated}
                                                             (graphql-utils/gql-name->kw order-by))
                                                        (or (keyword order-direction) :asc)]]))]
     (paged-query query limit offset))))

(defn arbiter->feedback-resolver [root {:keys [:limit :offset] :as args} _]
  (try-catch-throw
   (let [{:keys [:user/address] :as arbiter} (graphql-utils/gql->clj root)
         query {:select [:Job.job/id :Contract.contract/id :Message.message/id :feedback/rating
                         [:JobCreator.user/address :feedback/to-user-address]
                         [:ContractCandidate.user/address :feedback/from-user-address]
                         [:Message.message/date-created :feedback/date-created]
                         [:Message.message/text :feedback/text]]
                :from [:Feedback]
                :join [:Contract [:= :Feedback.contract/id :Contract.contract/id]
                       :Job [:= :Contract.job/id :Job.job/id]
                       :Message [:= :Message.message/id :Feedback.message/id]
                       :JobCreator [:= :JobCreator.job/id :Job.job/id]
                       :ContractCandidate [:= :ContractCandidate.contract/id :Contract.contract/id]
                       :JobArbiter [:= :JobArbiter.job/id :Job.job/id]
                       ]
                :where [:and [:= address :JobArbiter.user/address]
                        [:= "ARBITER FEEDBACK" :Message.message/type]]}]
     (log/debug "arbiter->feedback-resolver" {:arbiter arbiter :args args})
     (paged-query query limit offset))))

(def ^:private user-type-query
  {:select [:type]
   :from [{:union [{:select [:Candidate.user/address ["Candidate" :type]]
                    :from [:Candidate]}
                   {:select [:Employer.user/address ["Employer" :type]]
                    :from [:Employer]}
                   {:select [:Arbiter.user/address ["Arbiter" :type]]
                    :from [:Arbiter]}]}]})

(defn feedback->to-user-type-resolver [root _ _]
  (try-catch-throw
   (let [{:keys [:feedback/to-user-address] :as feedback} (graphql-utils/gql->clj root)]
     (log/debug "feedback->to-user-type-resolver" feedback)
     (-> (sql-helpers/merge-where user-type-query [:= to-user-address :user/address])
         db/get
         :type))))

(defn feedback->from-user-type-resolver [root _ _]
  (try-catch-throw
   (let [{:keys [:feedback/from-user-address] :as feedback} (graphql-utils/gql->clj root)]
     (log/debug "feedback->to-user-type-resolver" feedback)
     (-> (sql-helpers/merge-where user-type-query [:= from-user-address :user/address])
         db/get
         :type ))))

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
  (try-catch-throw
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
     (paged-query query limit offset))))

(defn candidate->candidate-categories-resolver [root _ _]
  (try-catch-throw
   (let [{:keys [:user/address] :as candidate} (graphql-utils/gql->clj root)]
     (log/debug "candidate->candidate-categories-resolver" candidate)
     (map :category/id (db/all {:select [:*]
                                :from [:CandidateCategory]
                                :where [:= address :CandidateCategory.user/address]})))))

(defn candidate->candidate-skills-resolver [root _ _]
  (try-catch-throw
   (let [{:keys [:user/address] :as candidate} (graphql-utils/gql->clj root)]
     (log/debug "candidate->candidate-skills-resolver" candidate)
     (map :skill/id (db/all {:select [:*]
                             :from [:CandidateSkill]
                             :where [:= address :CandidateSkill.user/address]})))))

(def ^:private candidate-feedback-query {:select [:Job.job/id :Contract.contract/id :Message.message/id :feedback/rating
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
                                         :where [:and #_[:= address :ContractCandidate.user/address]
                                                 [:= :Message.message/creator :JobCreator.user/address]
                                                 [:= "CANDIDATE FEEDBACK" :Message.message/type]
                                                 ]})

(defn candidate->feedback-resolver [root {:keys [:limit :offset] :as args} _]
  (try-catch-throw
   (let [{:keys [:user/address] :as candidate} (graphql-utils/gql->clj root)
         query (sql-helpers/merge-where candidate-feedback-query [:= address :ContractCandidate.user/address])]
     (log/debug "candidate->feedback-resolver" {:candidate candidate :args args})
     (paged-query query limit offset))))

(defn contract->candidate-feedback-resolver [root _ _]
  (try-catch-throw
   (let [{:keys [:contract/id :contract/candidate-address] :as contract} (graphql-utils/gql->clj root)]
     (log/debug "contract->candidate-feedback-resolver" {:contract contract})
     (db/get (-> candidate-feedback-query
                 (sql-helpers/merge-where [:= id :Contract.contract/id])
                 (sql-helpers/merge-where [:= candidate-address :ContractCandidate.user/address]))))))

(defn employer-search-resolver [_ {:keys [:limit :offset
                                          :user/address
                                          :professional-title
                                          :order-by :order-direction]
                                   :as args} _]
  (try-catch-throw
   (log/debug "employer-search-resolver" args)
   (let [query (cond-> employer-query

                 address (sql-helpers/merge-where [:= address :Employer.user/address])

                 professional-title (sql-helpers/merge-where [:= professional-title :Employer.employer/professional-title])

                 order-by (sql-helpers/merge-order-by [[(get {:date-registered :user/date-registered
                                                              :date-updated :user/date-updated}
                                                             (graphql-utils/gql-name->kw order-by))
                                                        (or (keyword order-direction) :asc)]]))]
     (paged-query query limit offset))))

(def ^:private job-query {:select [:Job.job/id
                                   :Job.job/title
                                   :Job.job/status
                                   :Job.job/bid-option
                                   :Job.job/category
                                   :Job.job/description
                                   :Job.job/date-created
                                   :Job.job/date-published
                                   :Job.job/date-updated
                                   :Job.job/estimated-length
                                   :Job.job/reward
                                   [:Job.job/invitation-only? :job/is-invitation-only]
                                   [:JobArbiter.user/address :job/accepted-arbiter-address]
                                   [:JobCreator.user/address :job/employer-address]]
                          :from [:Job]
                          :join [:JobArbiter [:= :JobArbiter.job/id :Job.job/id]
                                 :JobCreator [:= :JobCreator.job/id :Job.job/id]]})

(defn job-resolver [_ {:keys [:job/id] :as args} _]
  (try-catch-throw
   (log/debug "job-resolver" args)
   (db/get (sql-helpers/merge-where job-query [:= id :Job.job/id]))))

(def ^:private contract-query {:select [:Contract.contract/id
                                        :Job.job/id
                                        :Contract.contract/status
                                        :Contract.contract/date-created
                                        :Contract.contract/date-updated

                                        :Contract.contract/raised-dispute-message-id
                                        :Contract.contract/resolved-dispute-message-id

                                        [:ContractCandidate.user/address :contract/candidate-address]]
                               :from [:Contract]
                               :join [:Job [:= :Job.job/id :Contract.job/id]
                                      :ContractCandidate [:= :ContractCandidate.contract/id :Contract.contract/id]]})

(defn job->contracts-resolver [root {:keys [:limit :offset] :as args} _]
  (try-catch-throw
   (let [{:keys [:job/id] :as job} (graphql-utils/gql->clj root)]
     (log/debug "job->contracts-resolver" {:job job :args args})
     (paged-query (sql-helpers/merge-where contract-query [:= id :Contract.job/id]) limit offset))))

(defn contract-resolver [_ {job-id :job/id contract-id :contract/id :as args} _]
  (try-catch-throw
   (log/debug "contract-resolver" args)
   (db/get (-> contract-query
               (sql-helpers/merge-where [:= job-id :Job.job/id])
               (sql-helpers/merge-where [:= contract-id :Contract.contract/id])))))

(defn contract->disputes-resolver [root {:keys [:limit :offset] :as args} _]
  (try-catch-throw
   (let [{contract-id :contract/id job-id :job/id
          raised-dispute-message-id :contract/raised-dispute-message-id
          resolved-dispute-message-id :contract/resolved-dispute-message-id
          :as contract} (graphql-utils/gql->clj root)
         query {:select [[contract-id :contract/id]
                         [job-id :job/id]
                         [{:select [:message/text]
                           :from [:Message]
                           :where [:= raised-dispute-message-id :Message.message/id]} :dispute/reason]
                         [{:select [:message/date-created]
                           :from [:Message]
                           :where [:= raised-dispute-message-id :Message.message/id]} :dispute/date-created]
                         [{:select [:message/date-created]
                           :from [:Message]
                           :where [:= resolved-dispute-message-id :Message.message/id]} :dispute/date-resolved]]}]
     (log/debug "contract->disputes-resolver" {:contract contract :args args})
     (paged-query query limit offset))))

;; TODO
(defn contract->invoices-resolver [root {:keys [:limit :offset] :as args} _]
  (try-catch-throw
   (let [{contract-id :contract/id job-id :job/id
          ;; raised-dispute-message-id :contract/raised-dispute-message-id
          ;; resolved-dispute-message-id :contract/resolved-dispute-message-id
          :as contract} (graphql-utils/gql->clj root)
         query {} #_{:select [[contract-id :contract/id]
                            [job-id :job/id]
                            [{:select [:message/text]
                              :from [:Message]
                              :where [:= raised-dispute-message-id :Message.message/id]} :dispute/reason]
                            [{:select [:message/date-created]
                              :from [:Message]
                              :where [:= raised-dispute-message-id :Message.message/id]} :dispute/date-created]
                            [{:select [:message/date-created]
                              :from [:Message]
                              :where [:= resolved-dispute-message-id :Message.message/id]} :dispute/date-resolved]]}]
     (log/debug "contract->invoices-resolver" {:contract contract :args args})
     #_(paged-query query limit offset))))

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
                            :employer employer-resolver
                            :employerSearch employer-search-resolver
                            :arbiter arbiter-resolver
                            :arbiterSearch arbiter-search-resolver
                            :job job-resolver
                            :contract contract-resolver

                            }
                    :Job {:job_contracts job->contracts-resolver}
                    :Contract {:contract_employerFeedback contract->employer-feedback-resolver
                               :contract_candidateFeedback contract->candidate-feedback-resolver
                               :contract_disputes contract->disputes-resolver
                               :contract_invoices contract->invoices-resolver
                               }
                    :User {:user_languages user->languages-resolvers
                           :user_isRegisteredCandidate user->is-registered-candidate-resolver
                           :user_isRegisteredEmployer user->is-registered-employer-resolver
                           :user_isRegisteredArbiter user->is-registered-arbiter-resolver}
                    :Candidate {:candidate_feedback candidate->feedback-resolver
                                :candidate_categories candidate->candidate-categories-resolver
                                :candidate_skills candidate->candidate-skills-resolver}
                    :Employer {:employer_feedback employer->feedback-resolver}
                    :Arbiter {:arbiter_feedback arbiter->feedback-resolver}
                    :Feedback {:feedback_toUserType feedback->to-user-type-resolver
                               :feedback_fromUserType feedback->from-user-type-resolver}
                    :Mutation {:signIn sign-in-mutation}})
