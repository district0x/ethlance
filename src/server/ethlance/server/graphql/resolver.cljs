(ns ethlance.server.graphql.resolver
  "Main GraphQL Resolver Entry-point"
  (:require

   ;; Resolvers
   [ethlance.server.graphql.resolvers.user :as resolvers.user]))


(def graphql-resolver-map
  {:Query
   {:user-id resolvers.user/user-id-query
    :user resolvers.user/user-query}
   :User
   {}})
