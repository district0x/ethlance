(ns ethlance.server.graphql.resolvers.job
  "GraphQL Resolvers defined for a Job, or Job Listings."
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
   [ethlance.server.model.arbiter :as model.arbiter]
   [ethlance.server.model.job :as model.job]))


(def enum graphql-utils/kw->gql-name)


(defn job-query
  "Main Resolver of Job Data"
  [_ {:keys [:job/index]}]
  (log/debug (str "Querying Job Index: " index))
  (try-catch
   (when (> index 0)
     (model.job/get-job index))))


(defn job-search-query
  ""
  [_ {:keys [:job/index
             :job/title
             :job/accepted-arbiter
             :job/availability
             :job/bid-option
             :job/category
             :job/employer-uid
             order-by
             order-direction
             first
             after]}]
  (let []
    {:items nil
     :total-count 0
     :end-cursor nil
     :has-next-page false}))


