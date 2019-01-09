(ns ethlance.server.graphql.resolver
  "Main GraphQL Resolver Entry-point"
  (:require

   ;; Resolvers
   [ethlance.server.graphql.resolvers.user :as resolvers.user]
   [ethlance.server.graphql.resolvers.candidate :as resolvers.candidate]))


(def graphql-resolver-map
  {:Query
   {;; User Queries
    :user-id resolvers.user/user-id-query
    :user resolvers.user/user-query
    :user-search resolvers.user/user-search-query

    ;; Candidate Queries
    :candidate resolvers.candidate/candidate-query}
   :User {}
   :Candidate {}
   :Employer {}
   :Arbiter {}
   :Job {}
   :WorkContract {}
   :Invoice {}
   :Dispute {}})
