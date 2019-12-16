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
                      offset (assoc :offset offset))]
    (db/all paged-query)))



#_(defn current-user-resolver [_ _ {:keys [:current-user]}]
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

#_(defn search-users-resolver [_ {:keys [:limit :offset :user/address :not-current-user  :order-by :order-direction] :as args} {:keys [:current-user]}]
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
                                                        (or (keyword order-direction) :asc)]]))]
     (paged-query query limit offset))))

#_(defn update-user-profile-mutation [_ {:keys [:input]} _]
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

(defn user-resolver [_ {:keys [:user/address] :as args} _]
  (try-catch-throw
   (log/debug "user-resolver" args)
   (ethlance-db/get-user args)))

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

(def resolvers-map {:Query {:user user-resolver
                            ;; :currentUser (require-auth current-user-resolver)
                            ;; :searchUsers search-users-resolver
                            }
                    ;; :User {:user_isRegisteredCandidate user->is-registered-candidate-resolver}
                    :Mutation {:signIn sign-in-mutation}


                    })
