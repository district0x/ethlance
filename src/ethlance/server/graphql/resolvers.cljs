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
  [db query limit offset]
  (let [paged-query (cond-> query
                      limit (assoc :limit limit)
                      offset (assoc :offset offset))]
    (db/all paged-query)))

#_(defn default-item-resolver
  "The default resolver that is used with GraphQL schemas that return an
  `:item` key
  "
  [item-list]
  (get item-list :items []))

;; TODO
#_(defn require-auth
  "Given a `resolver` fn returns a wrapped resolver.
  It will call the given `resolver` if the request contains currentUser,
  see `ethlance.server.graphql.mutations.sign-in/session-middleware`.
  It will throw a error otherwise."
  [resolver]
  (fn [& [_ _ req :as args]]
    (if (.-currentUser req)
      (apply resolver args)
      (throw (js/Error. "Authentication required")))))


#_(def graphql-resolver-map

  {:Query
   {}

   :Mutation
   {;; Sign in with signed message
    :sign-in mutations.sign-in/sign-in}})

(defn user-resolver [_ {:keys [:user/address] :as args} _]
  (try-catch-throw
   (log/debug "user-resolver" args)
   (ethlance-db/get-user args)))

(defn user->is-registered-candidate-resolver [root _ _]
  (try-catch-throw
   (let [{user-address :user/address :as user} (graphql-utils/gql->clj root)]
     (log/debug "user->is-registered-candidate-resolver" {:user user})
     (not (= 0 (db/get {:select [[(sql/call :count :*) :count]]
                        :from [:Candidate]
                        :where [:= user-address :Candidate.user/address]}))))))

(defn search-users-resolver [_ {:keys [:user/address :limit :offset :order-by :order-direction] :as args} {:keys [:db]}]
  (try-catch-throw
   (log/debug "search-users-resolver" args)
   (let [query (cond-> {:select [:*]
                        :from [:User]}

                 address (sql-helpers/merge-where [:= :User.user/address address])

                 order-by (sql-helpers/merge-order-by [[(get {:users.order-by/user-name :user/user-name
                                                              ;; random order as a placeholder for ordering
                                                              :users.order-by/random (sql/call :random)}
                                                             (graphql-utils/gql-name->kw order-by))
                                                        (or (keyword order-direction) :asc)]])

                 true (sql-helpers/merge-order-by [[:User.user/address :desc]]))]
     (paged-query db query limit offset))))

;; TODO : mutation
(defn update-user-profile-mutation [_ {:keys [:input]} _ #_{:keys [:user/id]}]
  #_(let [{:keys [:user/name :user/photo :user/bio :user/location] :as args} (utils/gql-input->clj input)]
    (promise-> current-user
               (fn [authenticated-user]
                 (let [user (merge args authenticated-user)]
                   (log/debug "edit-user-mutation" user)
                   (promise-> (db/upsert-user! user db)
                              #(db/get-user user db)))))))

;; TODO : auth + context
(def resolvers-map {:Query {:user user-resolver
                            :searchUsers search-users-resolver}
                    :User {:user_isRegisteredCandidate user->is-registered-candidate-resolver}
                    :Mutation {:updateUserProfile update-user-profile-mutation}})
