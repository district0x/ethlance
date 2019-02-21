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
   [honeysql.helpers :as sqlh]

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
   [ethlance.server.model.arbiter :as model.arbiter]

   [ethlance.server.graphql.pagination :refer [paged-query]]))


(def enum graphql-utils/kw->gql-name)


(defn gql-order-by->db
  "Convert gql orderBy representation into the database representation."
  [gql-name]
  (let [kw (graphql-utils/gql-name->kw gql-name)
        relations {:date-updated :u.user/date-updated
                   :date-created :u.user/date-created
                   :date-registered :ue.employer/date-registered}]
    (get relations kw)))


(defn employer-query
  "Main Resolver for Employer Data"
  [_ {:keys [:user/id]}]
  (log/debug (str "Querying for Employer: " id))
  (try-catch
   (when (> id 0)
     (model.employer/get-data id))))


(defn employer-search-query
  ""
  [_ {:keys [:user/address
             :user/full-name
             :user/user-name
             :employer/professional-title
             order-by
             order-direction
             first
             after] :as args}]
  (log/debug (str "employer search: " args))
  (let [page-size first
        page-start-idx (when after (js/parseInt after)) 
        query (cond-> {:select [:ue.*]
                       :from [[:User :u]]
                       :join [[:UserEmployer :ue]
                              [:= :u.user/id :ue.user/id]]}
                address (sqlh/merge-where [:like :u.user/address (str "%" address "%")])
                full-name (sqlh/merge-where [:like :u.user/full-name (str "%" full-name "%")])
                user-name (sqlh/merge-where [:like :u.user/user-name (str "%" user-name "%")])
                professional-title (sqlh/merge-where [:like :ue.employer/professional-title (str "%" professional-title "%")])
                order-by (sqlh/merge-order-by [(gql-order-by->db order-by)
                                               (or (keyword order-direction) :asc)]))]
    (log/debug query)
    (paged-query query page-size page-start-idx)))
