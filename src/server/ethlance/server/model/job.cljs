(ns ethlance.server.model.job
  (:require
   [bignumber.core :as bn]
   [cuerdas.core :as str]
   [district.server.config]
   [district.server.db :as district.db]
   [taoensso.timbre :as log]
   [honeysql.core :as sql]
   [ethlance.server.db :as ethlance.db]
   [ethlance.shared.enum.bid-option :as enum.bid-option]
   [ethlance.shared.enum.boolean :as enum.boolean]))


(defn- enum-kw->val
  "Replace arbiter data's keyword enumerations into their respective
  values."
  [m]
  (-> m
      (enum.bid-option/assoc-kw->val :job/bid-option)
      (enum.boolean/assoc-kw->val :job/include-ether-token?)
      (enum.boolean/assoc-kw->val :job/is-invitation-only?)))



(defn- enum-val->kw
  [m]
  (-> m
      (enum.bid-option/assoc-val->kw :job/bid-option)
      (enum.boolean/assoc-val->kw :job/include-ether-token?)
      (enum.boolean/assoc-val->kw :job/is-invitation-only?)))


(defn create-job! [job-data]
  (let [job-data (enum-kw->val job-data)]
    (ethlance.db/insert-row! :Job job-data)))


(defn arbiter-request-listing
  [job-id])


(defn add-arbiter-request!
  [job-id arbiter-request-data])


(defn skill-listing
  [job-id])


(defn update-skill-listing!
  [job-id listing])


(defn work-contract-listing [job-id])


(defn work-contract-count [job-id]
  (let [result (district.db/get {:select [(sql/call :COUNT "*")]
                                 :from :Job
                                 :where [:= :job/id job-id]})]
    result))


(defn create-work-contract! [work-contract-data]
  (ethlance.db/insert-row! :WorkContract work-contract-data))


(defn update-work-contract! [work-contract-data])


(defn get-work-contract [job-id index])


(defn create-invoice! [invoice-data]
   (ethlance.db/insert-row! :WorkContractInvoice invoice-data))


(defn update-invoice! [])


(defn invoice-listing [job-id work-index])


(defn create-dispute! [dispute-data]
   (ethalnce.db/insert-row! :WorkContractDispute dispute-data))


(defn update-dispute! [dispute-data])


(defn dispute-listing [job-id work-index])


(defn get-job-data [job-id]
  (let [job (ethlance.db/get-row :Job {:job/id job-id})]
    (enum-val->kw job)))
  
