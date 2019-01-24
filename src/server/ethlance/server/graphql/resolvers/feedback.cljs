(ns ethlance.server.graphql.resolvers.feedback
  "GraphQL Resolvers defined for feedbacks and feedback listings."
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
   [ethlance.server.model.comment :as model.comment]
   [ethlance.server.model.feedback :as model.feedback]

   [ethlance.shared.enum.user-type :as enum.user-type]))


(defn candidate-feedback-query
  "Accumulation of Feedback objects for the given candidate defined by
  their user id."
  [{:keys [first after :user/id]}]
  (log/debug (str/format "Candidate Feedback id=%s, first=%s, after=%s" id first after))
  (let [to-user-type (model.feedback/enum-kw->val ::enum.user-type/candidate)
        q {:select [:*]
           :from [:WorkContractFeedback]
           :where [:and
                   [:= :feedback/to-user-id id]
                   [:= :feedback/to-user-type to-user-type]]}

        feedback-listing (->> (district.db/all q)
                              (mapv model.feedback/enum-val->kw))

        result {:items feedback-listing
                :total-count (count feedback-listing)
                :end-cursor (-> feedback-listing last :feedback/index str)
                :has-next-page false}]
    result))


(defn employer-feedback-query
  "Accumulation of Feedback objects for the given employer defined by
  their user id."
  [{:keys [:user/id]}]
  {:items []
   :total-count 0
   :end-cursor ""
   :has-next-page false})


(defn arbiter-feedback-query
  "Accumulation of Feedback objects for the given arbiter defined by
  their user id."
  [{:keys [:user/id]}]
  {:items []
   :total-count 0
   :end-cursor ""
   :has-next-page false})


(defn work-employer-query
  [{job-index :job/index
    work-index :work-contract/index}])


(defn work-candidate-query
  [{job-index :job/index
    work-index :work-contract/index}])

