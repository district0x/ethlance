(ns ethlance.server.graphql.resolvers.invoice
  "GraphQL Resolvers defined for a Invoice, or Invoice Listings."
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
   [ethlance.server.model.candidate :as model.candidate]
   [ethlance.server.model.employer :as model.employer]
   [ethlance.server.model.arbiter :as model.arbiter]
   [ethlance.server.model.job :as model.job]

   [ethlance.server.graphql.pagination :refer [paged-query]]))


(def enum graphql-utils/kw->gql-name)


(defn gql-order-by->db
  "Convert gql orderBy representation into the database representation."
  [gql-name]
  (let [kw (graphql-utils/gql-name->kw gql-name)
        relations {:date-updated :i.invoice/date-updated
                   :date-created :i.invoice/date-created
                   :date-paid :i.invoice/date-paid}]
    (get relations kw)))


(defn invoice-query
  "Main Resolver of Invoice Data"
  [_ {job-index :job/index
      work-contract-index :work-contract/index
      invoice-index :invoice/index}]
  
  (log/debug (str "Querying Invoice Index: " invoice-index))
  (try-catch
   (when (and (>= job-index 0) (>= work-contract-index 0) (>= invoice-index 0))
     (nth (model.job/invoice-listing job-index work-contract-index) invoice-index))))


(defn invoices-resolver
  ""
  [work-contract
   {:keys [order-by
           order-direction
           first
           after] :as args}]
  (log/debug (str "invoice search: " args))
  (log/debug (str work-contract))
  (let [page-size first
        page-start-idx (when after (js/parseInt after)) 
        query (cond-> {:select [:i.*]
                       :from [[:WorkContractInvoice :i]]
                       :where [:and
                               [:= :i.job/index (:job/index work-contract)]
                               [:= :i.work-contract/index (:work-contract/index work-contract)]]}
                order-by (sqlh/merge-order-by [(gql-order-by->db order-by)
                                               (or (keyword order-direction) :asc)]))]
    (log/debug query)
    (paged-query query page-size page-start-idx)))


