(ns ethlance.server.graphql.resolvers.employer
  "GraphQL Resolvers defined for a Employer, or Employer Listings."
  (:require
   [bignumber.core :as bn]
   [cljs-time.core :as t]
   [cljs-web3.core :as web3-core]
   [cljs-web3.eth :as web3-eth]
   [cljs-web3.async.eth :as web3-eth-async]
   [cljs.core.match :refer-macros [match]]
   [cljs.nodejs :as nodejs]
   [cuerdas.core :as str]
   [taoensso.timbre :as log]

   [district.shared.error-handling :refer [try-catch]]
   [district.graphql-utils :as graphql-utils]
   [district.server.config :refer [config]]
   [district.server.db :as district.db]
   [district.server.smart-contracts :as contracts]
   [district.server.web3 :as web3]
   [district.server.db :as district.db]

   [ethlance.server.db :as ethlance.db]
   [ethlance.server.model.user :as model.user]
   [ethlance.server.model.employer :as model.employer]
   [ethlance.server.model.arbiter :as model.arbiter]))


(defn employer-query
  "Main Resolver for Employer Data"
  [_ {:keys [:user/id]}]
  (log/debug (str "Querying for Employer: " id))
  (try-catch
   (when (> id 0)
     (model.employer/get-data id))))


(defn employer-search-query
  ""
  [_ {:keys []}])
