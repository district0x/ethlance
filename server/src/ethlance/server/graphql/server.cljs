(ns ethlance.server.graphql.server
  (:require
    [cljs.nodejs :as nodejs]
    [cljs.reader :refer [read-string]]
    [district.server.config :as config]
    [district.shared.async-helpers :refer [promise->]]
    [ethlance.server.graphql.middlewares :as middlewares]
    [ethlance.server.graphql.resolvers :as resolvers]
    [ethlance.server.ui-config :as ui-config]
    [ethlance.server.debug :as server-debug]
    [ethlance.shared.graphql.schema :as schema]
    [ethlance.shared.utils :as shared-utils]
    [mount.core :as mount :refer [defstate]]
    [taoensso.timbre :as log]))


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
  :stop (stop graphql))


(defn start
  [opts]
  (server-debug/start-collecting 5)
  (let [executable-schema (makeExecutableSchema (clj->js {:typeDefs (gql schema/schema)
                                                          :resolvers resolvers/resolvers-map}))
        schema-with-middleware (applyMiddleware executable-schema
                                                middlewares/args->clj-middleware
                                                ;; middlewares/logging-middleware
                                                middlewares/response->gql-middleware)

        ;; NOTE: the order off how we are applying middlewares matter
        app (doto (express)
              (.use (.json body-parser #js {:limit "2mb"}))
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

    (js-invoke app "get" "/config" (fn [_req res]
                                     ;; Add JSON /config endpoint for district-ui-config
                                     (.then (ui-config/fetch-config {:env-name "UI_CONFIG_PATH"})
                                            (fn [config]
                                              (.setHeader res "Access-Control-Allow-Origin", "*")
                                              (.json res (clj->js config))))))
    (js-invoke app "get" "/debug" (fn [_req res]
                                     (.then (server-debug/collect)
                                            (fn [debug-data]
                                              (.setHeader res "Access-Control-Allow-Origin", "*")
                                              (.json res (clj->js debug-data))))))
    (js-invoke server "applyMiddleware" (clj->js {:app app}))
    (js-invoke app "listen" (clj->js opts)
               (fn [url]
                 (log/info "Graphql with express middleware server started...")
                 (js->clj url :keywordize-keys true)))))


(defn stop
  [graphql]
  (promise-> @graphql
             (fn [{:keys [server]}]
               (js/Promise.
                 (fn [resolve _]
                   (js-invoke server "close" (fn []
                                               (log/debug "Graphql server stopped...")
                                               (resolve ::stopped))))))))


;; TODO : implement restart
(defn restart
  []
  (log/debug "restarting graphql server")
  #_(let [opts (merge (:graphql @config/config)
                    (:graphql (mount/args)))]
    (promise-> (stop)
               (fn [resp]
                 (start opts)))))
