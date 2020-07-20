(ns ethlance.server.graphql.server
  (:require [cljs.nodejs :as nodejs]
            [district.server.config :as config]
            [district.shared.async-helpers :refer [promise->]]
            [ethlance.shared.utils :as shared-utils]
            [ethlance.server.graphql.authorization :as authorization]
            [ethlance.server.graphql.resolvers :as resolvers]
            [ethlance.server.graphql.middlewares :as middlewares]
            [ethlance.shared.graphql.schema :as schema]
            [mount.core :as mount :refer [defstate]]
            [cljs.reader :refer [read-string]]
            [taoensso.timbre :as log]
            [clojure.string :as str]))

(nodejs/enable-util-print!)

(def body-parser (nodejs/require "body-parser"))
(def express (nodejs/require "express"))
(def apollo-server (nodejs/require "apollo-server-express"))
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

        ;; NOTE: the order off how we are applying middlewares matter
        app (doto (express)
              (.use (.json body-parser))
              (.use middlewares/current-user-express-middleware))

        server (new ApolloServer
                    (clj->js {:schema schema-with-middleware
                              :context (fn [event]
                                         (let [user (read-string (aget event "req" "headers" "current-user"))
                                               timestamp (or (read-string (aget event "req" "headers" "timestamp"))
                                                             (shared-utils/now))]
                                           {:config @config/config
                                            :current-user user
                                            :timestamp timestamp}))}))]

    (js-invoke server "applyMiddleware" (clj->js {:app app}))
    (js-invoke app "listen" (clj->js opts)
               (fn [url]
                 (log/info "Graphql with express middleware server started...")
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
