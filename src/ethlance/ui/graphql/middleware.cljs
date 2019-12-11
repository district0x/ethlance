(ns ethlance.ui.graphql.middleware
  (:require
   ;; [district.ui.logging]
   [taoensso.timbre :as log]

   ;; [reagent.core :as reagent]

   ;; ;; apollo-client
   [ethlance.shared.graphql.utils :as graphql-utils]
   ;; [ApolloClient]
   ;; [defaultDataIdFromObject]
   ;; [InMemoryCache]
   ;; [setContext]
   ;; [ApolloLink]
   ;; [ApolloProvider]
   ))

(defn auth [opts]
  (fn [_ request]
    (let [{:keys [:headers]} (js->clj request)]
      {:headers (merge headers
                       ;; TODO : hardcoded, read from localstore
                       {:access-token "topsecret"})})))
