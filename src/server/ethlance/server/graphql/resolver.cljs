(ns ethlance.server.graphql.resolver
  "Main GraphQL Resolver Entry-point"
  (:require
   [ethlance.server.graphql.mutations.sign-in :as mutations.sign-in]))


(defn default-item-resolver
  "The default resolver that is used with GraphQL schemas that return an
  `:item` key
  "
  [item-list]
  (get item-list :items []))


(defn require-auth
  "Given a `resolver` fn returns a wrapped resolver.
  It will call the given `resolver` if the request contains currentUser,
  see `ethlance.server.graphql.mutations.sign-in/session-middleware`.
  It will throw a error otherwise."
  [resolver]
  (fn [& [_ _ req :as args]]
    (if (.-currentUser req)
      (apply resolver args)
      (throw (js/Error. "Authentication required")))))


(def graphql-resolver-map

  {:Query 
   {}
   
   :Mutation
   {;; Sign in with signed message
    :sign-in mutations.sign-in/sign-in}})
