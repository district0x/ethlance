(ns ethlance.server.graphql.resolver
  "Main GraphQL Resolver Entry-point"
  (:require
   [bignumber.core :as bn]
   [cljs-time.core :as t]
   [cljs-web3.core :as web3-core]
   [cljs-web3.eth :as web3-eth]
   [cljs-web3.async.eth :as web3-eth-async]
   [cljs.core.match :refer-macros [match]]
   [cljs.nodejs :as nodejs]
   [cuerdas.core :as str]
   [district.graphql-utils :as graphql-utils]
   [district.server.config :refer [config]]
   [district.server.db :as district.db]
   [district.server.smart-contracts :as contracts]
   [district.server.web3 :as web3]

   ;; Enums
   [ethlance.shared.enum.currency-type :as enum.currency]
   [ethlance.shared.enum.payment-type :as enum.payment]
   [ethlance.shared.enum.bid-option :as enum.bid-option]

   ;; Ethlance Models
   [ethlance.server.model.job :as model.job]
   [ethlance.server.model.user :as model.user]
   [ethlance.server.model.arbiter :as model.arbiter]
   [ethlance.server.model.candidate :as model.candidate]
   [ethlance.server.model.employer :as model.employer]

   ;; Misc.
   [ethlance.server.db :as ethlance.db]

   ;; Resolvers
   [ethlance.server.graphql.resolvers.user :as resolvers.user]))


(def graphql-resolver-map
  {:Query {:user resolvers.user/user-query}})
