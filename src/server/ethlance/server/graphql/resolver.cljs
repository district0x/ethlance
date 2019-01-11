(ns ethlance.server.graphql.resolver
  "Main GraphQL Resolver Entry-point"
  (:require

   ;; Resolvers
   [ethlance.server.graphql.resolvers.user :as resolvers.user]
   [ethlance.server.graphql.resolvers.candidate :as resolvers.candidate]
   [ethlance.server.graphql.resolvers.employer :as resolvers.employer]
   [ethlance.server.graphql.resolvers.arbiter :as resolvers.arbiter]))


(def graphql-resolver-map
  {:Query
   {;; User Queries
    :user-id resolvers.user/user-id-query
    :user resolvers.user/user-query
    :user-search resolvers.user/user-search-query

    ;; Candidate Queries
    :candidate resolvers.candidate/candidate-query
    :candidate-search resolvers.candidate/candidate-search-query

    ;; Employer Queries
    :employer resolvers.employer/employer-query
    :employer-search resolvers.employer/employer-search-query

    ;; Arbiter Queries
    :arbiter resolvers.arbiter/arbiter-query
    :arbiter-search resolvers.arbiter/arbiter-search-query}
   :User
   {:user/languages resolvers.user/user-languages-query}
   :Candidate {}
   :Employer {}
   :Arbiter {}
   :Job {}
   :WorkContract {}
   :Invoice {}
   :Dispute {}})
