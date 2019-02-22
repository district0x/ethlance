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
   [ethlance.server.model.comment :as model.comment]
   [ethlance.server.model.feedback :as model.feedback]

   [ethlance.shared.enum.user-type :as enum.user-type]
   [ethlance.server.graphql.pagination :refer [paged-query]]))


(defn candidate-feedback-resolver
  "Accumulation of Feedback objects for the given candidate defined by
  their user id."
  [candidate {:keys [first after]}]
  (log/debug (str/format "Candidate Feedback id=%s, first=%s, after=%s" (:user/id candidate) first after))
  (let [id (:user/id candidate)
        page-size first
        page-start-idx (when after (js/parseInt after))
        to-user-type (model.feedback/enum-kw->val ::enum.user-type/candidate)
        q {:select [:wf.*]
           :from [[:WorkContractFeedback :wf]]
           :where [:and
                   [:= :wf.feedback/to-user-id id]
                   [:= :wf.feedback/to-user-type to-user-type]]
           :order-by [:wf.feedback/date-updated]}

        feedback-listing (->> (district.db/all q)
                              (mapv model.feedback/enum-val->kw))

        feedback-total-count (count feedback-listing)
        
        feedback-items (->> feedback-listing
                            (drop page-start-idx)
                            (take page-size))
        
        feedback-end-cursor (->> feedback-listing
                                 (drop (+ page-start-idx page-size))
                                 first :user/id)]
    {:items feedback-items
     :total-count feedback-total-count
     :end-cursor (str feedback-end-cursor)
     :has-next-page (not (nil? feedback-end-cursor))}))


(defn employer-feedback-resolver
  "Accumulation of Feedback objects for the given employer defined by
  their user id."
  [{:keys [:user/id]}]
  {:items []
   :total-count 0
   :end-cursor ""
   :has-next-page false})


(defn arbiter-feedback-resolver
  "Accumulation of Feedback objects for the given arbiter defined by
  their user id."
  [{:keys [:user/id]}]
  {:items []
   :total-count 0
   :end-cursor ""
   :has-next-page false})


(defn work-employer-resolver
  [{job-index :job/index
    work-index :work-contract/index}])


(defn work-candidate-resolver
  [{job-index :job/index
    work-index :work-contract/index}])

