(ns ethlance.server.graphql.resolvers.candidate
  "GraphQL Resolvers defined for a Candidate, or Candidate Listings."
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
   [ethlance.server.model.candidate :as model.candidate]
   [ethlance.server.model.employer :as model.employer]
   [ethlance.server.model.arbiter :as model.arbiter]))


(defn candidate-query
  "Main Resolver for Candidate Data"
  [_ {:keys [:user/id]}]
  (log/debug (str "Querying for Candidate: " id))
  (try-catch
   (when (> id 0)
     (model.candidate/get-data id))))


(defn candidate-search-query
  ""
  [_ {:keys []}])


(defn candidate-categories-query
  [{:keys [:user/id]}]
  (log/debug (str "Candidate Category Listing: " id))
  (model.candidate/category-listing id))


(defn candidate-skills-query
  [{:keys [:user/id]}]
  (log/debug (str "Candidate Skills Listing: " id))
  (model.candidate/skill-listing id))
