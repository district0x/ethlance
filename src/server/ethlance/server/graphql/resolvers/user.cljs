(ns ethlance.server.graphql.resolvers.user
  "GraphQL Resolvers defined for a User, or User Listings"
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
   [ethlance.server.model.comment :as model.comment]))


(def enum graphql-utils/kw->gql-name)


(defn user-id-query
  "Returns the User ID of the given ethereum address, or nil"
  [_ {:keys [:user/address]}]
  (log/debug (str "Returning User ID of: " address))
  (try-catch
   (-> (district.db/get {:select [:user/id] :from [:User] :where [:= :user/address address]})
       (get :user/id))))


(defn user-query
  "Main Resolver of User Data"
  [_ {:keys [:user/id]}]
  (log/debug (str "Querying User Id: " id))
  (try-catch
   (when (> id 0)
     (model.user/get-data id))))


(defn user-search-query
  ""
  [_ {:keys [:user/user-address
             :user/fullname
             :user/username
             order-by
             order-direction
             first
             after]}]
  (let []
    {:items nil
     :total-count 0
     :end-cursor nil
     :has-next-page false}))


(defn user-languages-query
  "List of languages the current user speaks."
  [{:keys [:user/id]}]
  (log/debug (str "User Language Listing: " id))
  (model.user/language-listing id))


(defn is-registered-candidate-query
  [{:keys [:user/id]}]
  (log/debug (str "User Is Registered Candidate: " id))
  (-> (district.db/get
       {:select [1]
        :from [[:UserCandidate :uc]]
        :where [:= :user/id id]})
      seq boolean))


(defn is-registered-arbiter-query
  [{:keys [:user/id]}]
  (log/debug (str "User Is Registered Arbiter: " id))
  (-> (district.db/get
       {:select [1]
        :from [[:UserArbiter :uc]]
        :where [:= :user/id id]})
      seq boolean))


(defn is-registered-employer-query
  [{:keys [:user/id]}]
  (log/debug (str "User Is Registered Employer: " id))
  (-> (district.db/get
       {:select [1]
        :from [[:UserEmployer :uc]]
        :where [:= :user/id id]})
      seq boolean))
