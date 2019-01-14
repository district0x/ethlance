(ns ethlance.server.model.job
  (:require
   [clojure.spec.alpha :as s]
   [bignumber.core :as bn]
   [cuerdas.core :as str]
   [district.server.config]
   [district.server.db :as district.db]
   [taoensso.timbre :as log]
   [honeysql.core :as sql]
   [ethlance.server.db :as ethlance.db]
   [ethlance.shared.enum.availability :as enum.availability]
   [ethlance.shared.enum.bid-option :as enum.bid-option]
   [ethlance.shared.enum.boolean :as enum.boolean]
   [ethlance.shared.enum.contract-status :as enum.status]

   ;; Includes additional spec namespaces
   [ethlance.shared.spec :as espec]))


(defn- enum-kw->val
  "Replace arbiter data's keyword enumerations into their respective
  values."
  [m]
  (-> m
      (enum.availability/assoc-kw->val :job/availability)
      (enum.bid-option/assoc-kw->val :job/bid-option)
      (enum.boolean/assoc-kw->val :job/include-ether-token?)
      (enum.boolean/assoc-kw->val :job/is-invitation-only?)))


(defn- enum-val->kw
  [m]
  (-> m
      (enum.availability/assoc-val->kw :job/availability)
      (enum.bid-option/assoc-val->kw :job/bid-option)
      (enum.boolean/assoc-val->kw :job/include-ether-token?)
      (enum.boolean/assoc-val->kw :job/is-invitation-only?)))


(s/def ::job-data
  (s/keys
   :req [:job/index
         :job/title
         :job/availability
         :job/bid-option
         :job/category
         :job/description
         :job/date-created
         :job/employer-uid
         :job/estimated-length-seconds
         :job/include-ether-token?
         :job/is-invitation-only?]
   :opt [:job/accepted-arbiter
         :job/date-finished
         :job/date-started
         :job/reward-value]))


(s/fdef create-job!
  :args (s/cat :job-data ::job-data))

(defn create-job! [job-data]
  (let [job-data (enum-kw->val job-data)]
    (ethlance.db/insert-row! :Job job-data)))


(s/fdef update-job!
  :args (s/cat :job-data (s/keys :req [:job/index])))

(defn update-job! [job-data]
  (let [job-data (enum-kw->val job-data)]
    (ethlance.db/update-row! :Job job-data)))


(s/def ::arbiter-request-data ::espec/arbiter-request)


(s/fdef arbiter-request-listing
  :args (s/cat :job-index :job/index)
  :ret (s/coll-of ::arbiter-request-data :distinct true :into []))

(defn arbiter-request-listing
  [job-index]
  (let [listing (ethlance.db/get-list :JobArbiterRequest {:job/index job-index})]
    (mapv #(enum.boolean/assoc-val->kw % :arbiter-request/is-employer-request?) listing)))


(s/fdef add-arbiter-request!
  :args (s/cat :arbiter-request-data ::arbiter-request-data))

(defn add-arbiter-request!
  [arbiter-request-data]
  (let [data (-> arbiter-request-data
                 (enum.boolean/assoc-kw->val :arbiter-request/is-employer-request?))]
    (ethlance.db/insert-row! :JobArbiterRequest data)))


(s/fdef skill-listing
  :args (s/cat :job-index :job/index)
  :ret :job/skills)

(defn skill-listing
  [job-index]
  (let [listing (ethlance.db/get-list :JobSkills {:job/index job-index})]
    (mapv :skill/name listing)))


(s/fdef update-skill-listing!
  :args (s/cat :job-index :job/index :listing :job/skills))

(defn update-skill-listing!
  "Clear and replace the skill listing at `job-index` with the provided
  `listing`, which is a sequence of skills."
  [job-index listing]
  
  ;; clear the old data
  (district.db/run! {:delete-from :JobSkills
                     :where [:= :job/index job-index]})

  ;; populate the new data
  (doseq [name listing]
    (ethlance.db/insert-row! :JobSkills {:job/index job-index :skill/name name})))


(s/def ::work-contract
  (s/keys
   :req [:job/index
         :work-contract/index
         :work-contract/contract-status
         :work-contract/candidate-address
         :work-contract/date-updated
         :work-contract/date-created]
   :opts [:work-contract/date-finished]))


(s/fdef work-contract-listing
  :args (s/cat :job-index :job/index)
  :ret (s/coll-of ::work-contract :distinct true :into []))

(defn work-contract-listing [job-index]
  (let [listing (ethlance.db/get-list :WorkContract {:job/index job-index})]
    (mapv #(enum.status/assoc-val->kw % :work-contract/contract-status) listing)))


(s/fdef work-contract-count
  :args (s/cat :job-index :job/index)
  :ret nat-int?)

(defn work-contract-count [job-index]
  (let [result (district.db/get {:select [(sql/call :COUNT)]
                                 :from [:Job]
                                 :where [:= :job/index job-index]})]
    (-> result vals first)))


(s/fdef create-work-contract!
  :args (s/cat :work-contract-data ::work-contract))

(defn create-work-contract!
  [work-contract-data]
  (let [data (-> work-contract-data
                 (enum.status/assoc-kw->val :work-contract/contract-status))]
    (ethlance.db/insert-row! :WorkContract data)))


(s/fdef update-work-contract!
  :args (s/cat :work-contract-data (s/keys :req [:job/index :work-contract/index])))

(defn update-work-contract!
  [work-contract-data]
  (let [data (-> work-contract-data
                 (enum.status/assoc-kw->val :work-contract/contract-status))]
    (ethlance.db/update-row! :WorkContract data)))


(s/fdef get-work-contract
  :args (s/cat :job-index :job/index :index :work-contract/index)
  :ret ::work-contract)

(defn get-work-contract [job-index index]
  (let [data (ethlance.db/get-row :WorkContract {:job/index job-index :work-contract/index index})]
    (enum.status/assoc-val->kw data :work-contract/contract-status)))


(s/def ::invoice
  (s/keys
   :req [:job/index
         :work-contract/index
         :invoice/index
         :invoice/date-created
         :invoice/date-updated
         :invoice/amount-requested]
   :opt [:invoice/amount-paid
         :invoice/date-paid]))


(s/fdef create-invoice!
  :args (s/cat :invoice-data ::invoice))

(defn create-invoice! [invoice-data]
   (ethlance.db/insert-row! :WorkContractInvoice invoice-data))


(s/fdef update-invoice!
  :args (s/cat :invoice-data (s/keys :req [:job/index :work-contract/index :invoice/index])))

(defn update-invoice! [invoice-data]
   (ethlance.db/update-row! :WorkContractInvoice invoice-data))


(s/fdef invoice-listing
  :args (s/cat :job-index :job/index :work-index :work-contract/index)
  :ret (s/nilable (s/coll-of ::invoice :distinct true :into [])))

(defn invoice-listing [job-index work-index]
   (ethlance.db/get-list :WorkContractInvoice {:job/index job-index :work-contract/index work-index}))


(s/def ::dispute
  (s/keys
   :req [:job/index
         :work-contract/index
         :dispute/index
         :dispute/reason
         :dispute/date-created
         :dispute/date-updated]
   :opt [:dispute/date-resolved
         :dispute/employer-resolution-amount
         :dispute/candidate-resolution-amount
         :dispute/arbiter-resolution-amount]))


(s/fdef create-dispute!
  :args (s/cat :dispute-data ::dispute))

(defn create-dispute! [dispute-data]
   (ethlance.db/insert-row! :WorkContractDispute dispute-data))


(s/fdef update-dispute!
  :args (s/cat :dispute-data (s/keys :req [:job/index :work-contract/index :dispute/index])))

(defn update-dispute! [dispute-data]
   (ethlance.db/update-row! :WorkContractDispute dispute-data))


(s/fdef dispute-listing
  :args (s/cat :job-index :job/index :work-index :work-contract/index)
  :ret (s/nilable (s/coll-of ::dispute :distinct true :into [])))

(defn dispute-listing [job-index work-index]
   (ethlance.db/get-list :WorkContractDispute {:job/index job-index :work-contract/index work-index}))


(s/fdef get-job
  :args (s/cat :job-index :job/index)
  :ret (s/nilable ::job-data))

(defn get-job [job-index]
  (let [job (ethlance.db/get-row :Job {:job/index job-index})]
    (enum-val->kw job)))
  
