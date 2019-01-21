(ns ethlance.server.model.comment
  (:require
   [clojure.spec.alpha :as s]
   [bignumber.core :as bn]
   [cuerdas.core :as str]
   [district.server.config]
   [district.server.db :as district.db]
   [taoensso.timbre :as log]
   [honeysql.core :as sql]
   [ethlance.server.db :as ethlance.db]
   [ethlance.shared.enum.user-type :as enum.user-type]

   ;; Includes additional spec namespaces
   [ethlance.shared.spec :as espec]))


(defn- enum-kw->val
  [m]
  (-> m
      (enum.user-type/assoc-kw->val :comment/user-type)))


(defn- enum-val->kw
  [m]
  (-> m
      (enum.user-type/assoc-val->kw :comment/user-type)))


(s/def :work-contract/comment
  (s/keys
   :req [:job/index
         :work-contract/index
         :comment/index
         :comment/revision
         :user/id
         :comment/user-type
         :comment/date-created
         :comment/text]))


(s/fdef create-work-contract-comment!
  :args (s/cat :work-contract-comment-data :work-contract/comment))

(defn create-work-contract-comment!
  [comment-data]
  (let [comment-data (enum-kw->val comment-data)]
    (ethlance.db/insert-row! :WorkContractComment comment-data)))


(defn- work-contract-query
  [job-index work-contract-index]
  (district.db/all
   {:select [:*]
    :from [[:WorkContractComment :wc1]]
    :where
    [:and
     [:= :wc1.job/index job-index]
     [:= :wc1.work-contract/index work-contract-index]
     [:= :wc1.comment/revision 
         {:select [(sql/call :MAX :wc2.comment/revision)]
          :from [[:WorkContractComment :wc2]]
          :where [:= :wc1.comment/index :wc2.comment/index]}]]}))


(defn work-contract-comment-listing
  "Retrieves the latest revision of each work contract comment."
  [job-index work-contract-index]
  (when-let [results (work-contract-query job-index work-contract-index)]
    (mapv enum-val->kw results)))


(s/def :invoice/comment
  (s/keys
   :req [:job/index
         :work-contract/index
         :invoice/index
         :comment/index
         :comment/revision
         :user/id
         :comment/user-type
         :comment/date-created
         :comment/text]))


(s/fdef create-invoice-comment!
  :args (s/cat :invoice-comment-data :invoice/comment))

(defn create-invoice-comment!
  [comment-data]
  (let [comment-data (enum-kw->val comment-data)]
    (ethlance.db/insert-row! :WorkContractInvoiceComment comment-data)))


(defn- invoice-query
  [job-index work-contract-index invoice-index]
  (district.db/all
   {:select [:*]
    :from [[:WorkContractInvoiceComment :wc1]]
    :where
    [:and
     [:= :wc1.job/index job-index]
     [:= :wc1.work-contract/index work-contract-index]
     [:= :wc1.invoice/index invoice-index]
     [:= :wc1.comment/revision 
         {:select [(sql/call :MAX :wc2.comment/revision)]
          :from [[:WorkContractInvoiceComment :wc2]]
          :where [:= :wc1.comment/index :wc2.comment/index]}]]}))


(defn invoice-comment-listing
  "Retrieves the latest revision of each invoice comment."
  [job-index work-contract-index invoice-index]
  (when-let [results (invoice-query job-index work-contract-index invoice-index)]
    (mapv enum-val->kw results)))


(s/def :dispute/comment
  (s/keys
   :req [:job/index
         :work-contract/index
         :dispute/index
         :comment/index
         :comment/revision
         :user/id
         :comment/user-type
         :comment/date-created
         :comment/text]))


(s/fdef create-dispute-comment!
  :args (s/cat :dispute-comment-data :dispute/comment))

(defn create-dispute-comment!
  [comment-data]
  (let [comment-data (enum-kw->val comment-data)]
    (ethlance.db/insert-row! :WorkContractDisputeComment comment-data)))


(defn- dispute-query
  [job-index work-contract-index dispute-index]
  (district.db/all
   {:select [:*]
    :from [[:WorkContractDisputeComment :wc1]]
    :where
    [:and
     [:= :wc1.job/index job-index]
     [:= :wc1.work-contract/index work-contract-index]
     [:= :wc1.dispute/index dispute-index]
     [:= :wc1.comment/revision 
         {:select [(sql/call :MAX :wc2.comment/revision)]
          :from [[:WorkContractDisputeComment :wc2]]
          :where [:= :wc1.comment/index :wc2.comment/index]}]]}))


(defn dispute-comment-listing
  "Retrieves the latest revision of each dispute comment."
  [job-index work-contract-index dispute-index]
  (when-let [results (dispute-query job-index work-contract-index dispute-index)]
    (mapv enum-val->kw results)))
