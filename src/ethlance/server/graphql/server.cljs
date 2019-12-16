(ns ethlance.server.graphql.server
  (:require [cljs.nodejs :as nodejs]
            [district.server.config :as config]
            [district.shared.async-helpers :refer [promise->]]
            [ethlance.server.graphql.authorization :as authorization]
            [ethlance.server.graphql.resolvers :as resolvers]
            [ethlance.server.middlewares :as middlewares]
            [ethlance.shared.graphql.schema :as schema]
            [mount.core :as mount :refer [defstate]]
            [taoensso.timbre :as log]))

(nodejs/enable-util-print!)

(def apollo-server (nodejs/require "apollo-server"))
(def ApolloServer (aget apollo-server "ApolloServer"))
(def gql (aget apollo-server "gql"))
(def makeExecutableSchema (aget (nodejs/require "graphql-tools") "makeExecutableSchema"))
(def applyMiddleware (aget (nodejs/require "graphql-middleware") "applyMiddleware"))

(declare start stop)

(defstate ^{:on-reload :noop} graphql
  :start (start (merge (:graphql @config/config)
                       (:graphql (mount/args))))
  :stop (stop))

(defn start [opts]
  (let [executable-schema (makeExecutableSchema (clj->js {:typeDefs (gql schema/schema)
                                                          :resolvers resolvers/resolvers-map}))
        schema-with-middleware (applyMiddleware executable-schema
                                                middlewares/args->clj-middleware
                                                ;; middlewares/logging-middleware
                                                middlewares/response->gql-middleware)
        server (new ApolloServer (clj->js {:schema schema-with-middleware
                                           :context (fn [event]
                                                      {:config @config/config
                                                       :current-user (authorization/token->user event @config/config)})}))]
    (promise-> (js-invoke server "listen" (clj->js opts))
               (fn [url]
                 (log/info "Graphql server started...")
                 (js->clj url :keywordize-keys true)))))

(defn stop []
  (promise-> @graphql
             (fn [{:keys [:server] :as resp}]
               (js/Promise.
                (fn [resolve reject]
                  (js-invoke server "close" (fn []
                                              (log/debug "Graphql server stopped...")
                                              (resolve ::stopped))))))))

;; TODO : implement restart
(defn restart []
  (log/debug "restarting graphql server")
  (let [opts (merge (:graphql @config/config)
                    (:graphql (mount/args)))]
    #_(promise-> (stop)
               (fn [resp]
                 (start opts)))))
