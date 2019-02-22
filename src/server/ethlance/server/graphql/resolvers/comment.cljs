(ns ethlance.server.graphql.resolvers.comment
  "GraphQL Resolvers defined for comments and comment listings."
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
   [honeysql.core :as sql]
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
   [ethlance.server.model.candidate :as model.candidate]
   [ethlance.server.model.employer :as model.employer]
   [ethlance.server.model.arbiter :as model.arbiter] 

   [ethlance.server.graphql.pagination :refer [paged-query]]))


(def enum graphql-utils/kw->gql-name)


(defn gql-order-by->db
  "Convert gql orderBy representation into the database representation."
  [gql-name]
  (let [kw (graphql-utils/gql-name->kw gql-name)
        relations {:date-updated :c.comment/date-updated
                   :date-created :c.comment/date-created}]
    (get relations kw)))


(defn work-comments-resolver
  ""
  [work-contract
   {:keys [order-by
           order-direction
           first
           after] :as args}]
  (log/debug (str "work contract comment search: " args))
  (log/debug (str work-contract))
  (let [page-size first
        page-start-idx (when after (js/parseInt after)) 
        query (cond-> {:select [:c.*]
                       :from [[:WorkContractComment :c]]
                       :where [:and
                               [:= :c.job/index (:job/index work-contract)]
                               [:= :c.work-contract/index (:work-contract/index work-contract)]
                               [:= :c.comment/revision 
                                {:select [(sql/call :MAX :c2.comment/revision)]
                                 :from [[:WorkContractComment :c2]]
                                 :where [:= :c.comment/index :c2.comment/index]}]]}
                order-by (sqlh/merge-order-by [(gql-order-by->db order-by)
                                               (or (keyword order-direction) :asc)]))]
    (log/debug query)
    (paged-query query page-size page-start-idx)))


(defn invoice-comments-resolver
  ""
  [invoice
   {:keys [order-by
           order-direction
           first
           after] :as args}]
  (log/debug (str "invoice comment search: " args))
  (log/debug (str invoice))
  (let [page-size first
        page-start-idx (when after (js/parseInt after)) 
        query (cond-> {:select [:c.*]
                       :from [[:WorkContractInvoiceComment :c]]
                       :where [:and
                               [:= :c.job/index (:job/index invoice)]
                               [:= :c.work-contract/index (:work-contract/index invoice)]
                               [:= :c.invoice/index (:invoice/index invoice)]
                               [:= :c.comment/revision 
                                {:select [(sql/call :MAX :c2.comment/revision)]
                                 :from [[:WorkContractInvoiceComment :c2]]
                                 :where [:= :c.comment/index :c2.comment/index]}]]}
                order-by (sqlh/merge-order-by [(gql-order-by->db order-by)
                                               (or (keyword order-direction) :asc)]))]
    (log/debug query)
    (paged-query query page-size page-start-idx)))


(defn dispute-comments-resolver
  ""
  [dispute
   {:keys [order-by
           order-direction
           first
           after] :as args}]
  (log/debug (str "dispute comment search: " args))
  (log/debug (str dispute))
  (let [page-size first
        page-start-idx (when after (js/parseInt after)) 
        query (cond-> {:select [:c.*]
                       :from [[:WorkContractDisputeComment :c]]
                       :where [:and
                               [:= :c.job/index (:job/index dispute)]
                               [:= :c.work-contract/index (:work-contract/index dispute)]
                               [:= :c.dispute/index (:dispute/index dispute)]
                               [:= :c.comment/revision 
                                {:select [(sql/call :MAX :c2.comment/revision)]
                                 :from [[:WorkContractDisputeComment :c2]]
                                 :where [:= :c.comment/index :c2.comment/index]}]]}
                order-by (sqlh/merge-order-by [(gql-order-by->db order-by)
                                               (or (keyword order-direction) :asc)]))]
    (log/debug query)
    (paged-query query page-size page-start-idx)))
