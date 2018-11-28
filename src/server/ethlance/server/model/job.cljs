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
   [ethlance.shared.enum.boolean :as enum.boolean]
   [ethlance.shared.enum.contract-status :as enum.status]))


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


(defn update-job! [job-data]
  (let [job-data (enum-kw->val job-data)]
    (ethlance.db/update-row! :Job job-data)))


(defn arbiter-request-listing
  [job-id]
  (let [listing (ethlance.db/get-list :JobArbiterRequest {:job/id job-id})]
    (map #(enum.boolean/assoc-val->kw % :arbiter-request/is-employer-request?) listing)))


(defn add-arbiter-request!
  [arbiter-request-data]
  (let [data (-> arbiter-request-data
                 (enum.boolean/assoc-kw->val :arbiter-request/is-employer-request?))]
    (ethlance.db/insert-row! :JobArbiterRequest data)))


(defn skill-listing
  [job-id]
  (let [listing (ethlance.db/get-list :JobSkills {:job/id job-id})]
    (mapv :skill/name listing)))


(defn update-skill-listing!
  "Clear and replace the skill listing at `job-id` with the provided
  `listing`, which is a sequence of skills."
  [job-id listing]
  
  ;; clear the old data
  (district.db/run! {:delete-from :JobSkills
                     :where [:= :job/id job-id]})

  ;; populate the new data
  (doseq [name listing]
    (ethlance.db/insert-row! :JobSkills {:job/id job-id :skill/name name})))


(defn work-contract-listing [job-id]
  (let [listing (ethlance.db/get-list :WorkContract {:job/id job-id})]
    (mapv #(enum.status/assoc-val->kw % :work-contract/contract-status) listing)))


(defn work-contract-count [job-id]
  (let [result (district.db/get {:select [(sql/call :COUNT "*")]
                                 :from :Job
                                 :where [:= :job/id job-id]})]
    result))


(defn create-work-contract!
  [work-contract-data]
  (let [data (-> work-contract-data
                 (enum.status/assoc-kw->val :work-contract/contract-status))]
    (ethlance.db/insert-row! :WorkContract data)))


(defn update-work-contract!
  [work-contract-data]
  (let [data (-> work-contract-data
                 (enum.status/assoc-kw->val :work-contract/contract-status))]
    (ethlance.db/update-row! :WorkContract data)))


(defn get-work-contract [job-id index]
  (let [data (ethlance.db/get-row :WorkContract {:job/id job-id :work-contract/index index})]
    (enum.status/assoc-val->kw data :work-contract/contract-status)))


(defn create-invoice! [invoice-data]
   (ethlance.db/insert-row! :WorkContractInvoice invoice-data))


(defn update-invoice! [invoice-data]
   (ethlance.db/update-row! :WorkContractInvoice invoice-data))


(defn invoice-listing [job-id work-index]
   (ethlance.db/get-list :WorkContractInvoice {:job/id job-id :work-contract/index work-index}))


(defn create-dispute! [dispute-data]
   (ethlance.db/insert-row! :WorkContractDispute dispute-data))


(defn update-dispute! [dispute-data]
   (ethlance.db/update-row! :WorkContractDispute dispute-data))


(defn dispute-listing [job-id work-index]
   (ethlance.db/get-list :WorkContractDispute {:job/id job-id :work-contract/index work-index}))


(defn get-job [job-id]
  (let [job (ethlance.db/get-row :Job {:job/id job-id})]
    (enum-val->kw job)))
  
