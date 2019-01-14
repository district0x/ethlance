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


(defn invoice-query
  "Main Resolver of Invoice Data"
  [_ {job-index :job/index
      work-contract-index :work-contract/index
      invoice-index :invoice/index}]
  
  (log/debug (str "Querying Invoice Index: " invoice-index))
  (try-catch
   (when (> job-index 0) ;; FIXME
     (nth (model.job/invoice-listing job-index work-contract-index) invoice-index))))


(defn invoices-query
  ""
  [_ {:keys [:work-contract/index
             :work-contract/contract-status
             :work-contract/candidate-address
             order-by
             order-direction
             first
             after]}]
  (let []
    {:items nil
     :total-count 0
     :end-cursor nil
     :has-next-page false}))


