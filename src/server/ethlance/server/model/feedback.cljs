(ns ethlance.server.model.feedback
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


(defn enum-kw->val
  [m]
  (-> m
      (enum.user-type/assoc-kw->val :feedback/to-user-type)
      (enum.user-type/assoc-kw->val :feedback/from-user-type)))


(defn enum-val->kw
  [m]
  (-> m
      (enum.user-type/assoc-val->kw :feedback/to-user-type)
      (enum.user-type/assoc-val->kw :feedback/from-user-type)))


(s/def ::feedback
  (s/keys
   :req [:job/index
         :work-contract/index
         :feedback/index
         :feedback/to-user-type
         :feedback/to-user-id
         :feedback/from-user-type
         :feedback/from-user-id
         :feedback/date-created
         :feedback/rating
         :feedback/text]))


(s/fdef create-feedback!
  :args (s/cat :feedback-data ::feedback))

(defn create-feedback!
  [feedback-data]
  (let [feedback-data (enum-kw->val feedback-data)]
    (ethlance.db/insert-row! :WorkContractFeedback feedback-data)))


(s/fdef update-feedback!
  :args (s/cat :feedback-data ::feedback))

(defn update-feedback!
  [feedback-data]
  (let [feedback-data (enum-kw->val feedback-data)]
    (ethlance.db/update-row! :WorkContractFeedback feedback-data)))


(s/fdef feedback-listing
  :args (s/cat :job-index :job/index
               :work-index :work-contract/index)
  :ret (s/coll-of ::feedback :distinct true :into []))

(defn feedback-listing
  [job-index work-index]
  (if-let [listing (ethlance.db/get-list :WorkContractFeedback
                                         {:job/index job-index
                                          :work-contract/index work-index})]
    (mapv enum-val->kw listing)
    []))
