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

   [ethlance.shared.enumeration.user-type :as enum.user-type]
   [ethlance.server.graphql.pagination :refer [paged-query]]))


(defn general-feedback-resolver
  "Accumulation of Feedback objects for the given `user-type`, and
  defined by their user id."
  [user-type
   {:keys [:user/id]}
   {:keys [first after] :as args}]
  (log/debug (str/format "Feedback id=%s, args=%s" id (str args)))
  (let [page-size first
        page-start-idx (when after (js/parseInt after))
        to-user-type (enum.user-type/kw->val user-type)
        query {:select [:wf.*]
               :from [[:WorkContractFeedback :wf]]
               :where [:and
                       [:= :wf.feedback/to-user-id id]
                       [:= :wf.feedback/to-user-type to-user-type]]
               :order-by [:wf.feedback/date-created]}] ;;FIXME: date-updated
    (log/debug query)
    (paged-query query page-size page-start-idx)))


(def candidate-feedback-resolver
  #(general-feedback-resolver ::enum.user-type/candidate %1 %2))


(def employer-feedback-resolver
  #(general-feedback-resolver ::enum.user-type/employer %1 %2))


(def arbiter-feedback-resolver
  #(general-feedback-resolver ::enum.user-type/arbiter %1 %2))


(defn work-employer-resolver
  [{job-index :job/index
    work-index :work-contract/index}])


(defn work-candidate-resolver
  [{job-index :job/index
    work-index :work-contract/index}])

