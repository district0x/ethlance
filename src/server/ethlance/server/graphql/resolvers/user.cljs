(ns ethlance.server.graphql.resolvers.user
  "GraphQL Resolvers defined for a User, or User Listings"
  (:require
   [district.server.db :as district.db]
   [ethlance.server.db :as ethlance.db]
   [ethlance.server.model.user :as model.user]
   [ethlance.server.model.candidate :as model.candidate]
   [ethlance.server.model.employer :as model.employer]
   [ethlance.server.model.arbiter :as model.arbiter]))


(defn user-query
  "Main Resolver"
  [obj {:keys [:user/id]}]
  (model.user/get-data id))
