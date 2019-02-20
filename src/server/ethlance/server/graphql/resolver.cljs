(ns ethlance.server.graphql.resolver
  "Main GraphQL Resolver Entry-point"
  (:require

   ;; Resolvers
   [ethlance.server.graphql.resolvers.user :as resolvers.user]
   [ethlance.server.graphql.resolvers.candidate :as resolvers.candidate]
   [ethlance.server.graphql.resolvers.employer :as resolvers.employer]
   [ethlance.server.graphql.resolvers.arbiter :as resolvers.arbiter]
   [ethlance.server.graphql.resolvers.job :as resolvers.job]
   [ethlance.server.graphql.resolvers.work-contract :as resolvers.work-contract]
   [ethlance.server.graphql.resolvers.invoice :as resolvers.invoice]
   [ethlance.server.graphql.resolvers.dispute :as resolvers.dispute]
   [ethlance.server.graphql.resolvers.comment :as resolvers.comment]
   [ethlance.server.graphql.resolvers.feedback :as resolvers.feedback]))


(defn default-item-resolver
  "The default resolver that is used with GraphQL schemas that return an
  `:item` key
  "
  [item-list]
  (get item-list :items []))


(def graphql-resolver-map
  {:Query
   {;; User Queries
    :user-id resolvers.user/user-id-query
    :user resolvers.user/user-query
    :user-search resolvers.user/user-search-query

    ;; Candidate Queries
    :candidate resolvers.candidate/candidate-query
    :candidate-search resolvers.candidate/candidate-search-query

    ;; Employer Queries
    :employer resolvers.employer/employer-query
    :employer-search resolvers.employer/employer-search-query

    ;; Arbiter Queries
    :arbiter resolvers.arbiter/arbiter-query
    :arbiter-search resolvers.arbiter/arbiter-search-query

    ;; Job Queries
    :job resolvers.job/job-query
    :job-search resolvers.job/job-search-query

    ;; Work Contract Queries
    :work-contract resolvers.work-contract/work-contract-query}

   ;;
   ;; Defined Models
   ;;

   :User
   {:user/languages resolvers.user/user-languages-resolver
    :user/is-registered-candidate resolvers.user/is-registered-candidate-resolver
    :user/is-registered-arbiter resolvers.user/is-registered-arbiter-resolver
    :user/is-registered-employer resolvers.user/is-registered-employer-resolver}

   :UserList {:items default-item-resolver}

   :Candidate
   {:candidate/categories resolvers.candidate/candidate-categories-resolver
    :candidate/skills resolvers.candidate/candidate-skills-resolver
    :candidate/feedback resolvers.feedback/candidate-feedback-resolver}

   :Employer
   {:employer/feedback resolvers.feedback/employer-feedback-resolver}

   :Arbiter
   {:arbiter/feedback resolvers.feedback/arbiter-feedback-resolver}

   :Job
   {:job/work-contracts resolvers.work-contract/work-contracts-resolver}

   :WorkContract
   {:work-contract/invoices resolvers.invoice/invoices-resolver
    :work-contract/disputes resolvers.dispute/disputes-resolver
    :work-contract/employer-feedback resolvers.feedback/work-employer-resolver
    :work-contract/candidate-feedback resolvers.feedback/work-candidate-resolver
    :work-contract/comments resolvers.comment/work-comments-resolver}

   :Invoice
   {:invoice/comments resolvers.comment/invoice-comments-resolver}

   :Dispute
   {:dispute/comments resolvers.comment/dispute-comments-resolver}})
