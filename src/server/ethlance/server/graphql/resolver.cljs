(ns ethlance.server.graphql.resolver
  "Main GraphQL Resolver Entry-point"
  (:require

   ;; Resolvers
   [ethlance.server.graphql.resolvers.user :as resolvers.user]))


(def graphql-resolver-map
  {:Query
   {:hello (constantly "Hello World!!")
    :user resolvers.user/user-query}})
