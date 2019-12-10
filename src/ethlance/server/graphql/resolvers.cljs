(ns ethlance.server.graphql.resolvers
  "Main GraphQL Resolver Entry-point"
  (:require
   [district.shared.error-handling :refer [try-catch-throw]]
   [taoensso.timbre :as log :refer [spy]]
   [ethlance.server.db :as ethlance-db]
   [ethlance.shared.graphql.utils :as graphql-utils]
   [honeysql.helpers :as sql-helpers]
   [honeysql.core :as sql]
   [district.server.db :as db]
   #_[ethlance.server.graphql.mutations.sign-in :as mutations.sign-in]))

(defn- paged-query
  [query limit offset]
  (let [paged-query (cond-> query
                      limit (assoc :limit limit)
                      offset (assoc :offset offset))]
    (db/all paged-query)))

(defn user-resolver [_ {:keys [:user/address] :as args} _]
  (try-catch-throw
   (log/debug "user-resolver" args)
   (ethlance-db/get-user args)))

(defn current-user-resolver [_ _ {:keys [:current-user]}]
  (try-catch-throw
   (log/debug "current-user-resolver" current-user)
   (ethlance-db/get-user current-user)))

(defn user->is-registered-candidate-resolver [root _ _]
  (try-catch-throw
   (let [{user-address :user/address :as user} (graphql-utils/gql->clj root)]
     (log/debug "user->is-registered-candidate-resolver" {:user user})
     (not (= 0 (db/get {:select [[(sql/call :count :*) :count]]
                        :from [:Candidate]
                        :where [:= user-address :Candidate.user/address]}))))))

(defn search-users-resolver [_ {:keys [:user/address :not-current-user :limit :offset :order-by :order-direction] :as args} {:keys [:current-user]}]
  (try-catch-throw
   (log/debug "search-users-resolver" {:args args
                                       :current-user current-user})
   (let [query (cond-> {:select [:*]
                        :from [:User]}

                 address (sql-helpers/merge-where [:= :User.user/address address])

                 (and current-user not-current-user) (sql-helpers/merge-where [:not-in :User.user/address [(:user/address current-user)]])

                 order-by (sql-helpers/merge-order-by [[(get {:users.order-by/user-name :user/user-name
                                                              ;; random order as a placeholder for ordering
                                                              :users.order-by/random (sql/call :random)}
                                                             (graphql-utils/gql-name->kw order-by))
                                                        (or (keyword order-direction) :asc)]])

                 ;; true (sql-helpers/merge-order-by [[:User.user/address :desc]])

                 )]
     (paged-query query limit offset))))

(defn update-user-profile-mutation [_ {:keys [:input]} _]
  (try-catch-throw
   (let [{:keys [:user/address :user/user-name :user/profile-image :user/country-code] :as user} (graphql-utils/gql-input->clj input)]
     (log/debug "update-user-profile-mutation" user)
     (ethlance-db/upsert-user! user)
     (ethlance-db/get-user user))))

(defn require-auth [next]
  "Given a `resolver` fn returns a wrapped resolver.
  It will call the given `resolver` if the request contains currentUser,
  see `ethlance.server.graphql.mutations.sign-in/session-middleware`.
  It will throw a error otherwise."
  (fn [root args {:keys [:current-user] :as context} info]
    (if-not current-user
      (throw (js/Error. "Authentication required"))
      (next root args context info))))

;; TODO : auth + context
(def resolvers-map {:Query {:user user-resolver
                            :currentUser (require-auth current-user-resolver)
                            :searchUsers search-users-resolver}
                    :User {:user_isRegisteredCandidate user->is-registered-candidate-resolver}
                    :Mutation {:updateUserProfile update-user-profile-mutation}})
