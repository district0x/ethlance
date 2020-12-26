(ns ethlance.server.graphql.middlewares
  (:require [district.graphql-utils :as graphql-utils]
            [district.server.config :as config]
            [district.shared.async-helpers :as async-helpers]
            [ethlance.server.graphql.authorization :as authorization]
            [taoensso.timbre :as log]
            [clojure.string :as string]))

;; TODO : root-value->clj middleware

(defn response->gql-middleware [resolve root args context info]
  (let [response (resolve root args context info)]
    (if (async-helpers/promise? response)
      (-> response
          (.then (fn [response] (graphql-utils/clj->gql response)))
          (.catch (fn [error]
                    (log/error "response->gql-middleware" {:error error})
                    ;; make sure auth errors are shown to the client
                    (throw (new js/Error error)))))
      (graphql-utils/clj->gql response))))

(defn args->clj-middleware [resolve root args context info]
  (resolve root (graphql-utils/gql->clj args) context info))

(defn logging-middleware [resolve root args context info]
  (log/debug "Received graphql request" {:res resolve
                                         :root root
                                         :args args
                                         :context context
                                         :info info})
  (resolve root args context info))

(defn- bearer-token [auth-header]
  (second (string/split auth-header "Bearer ")))

(defn current-user-express-middleware [req _ next]
  (let [secret (-> @config/config :graphql :sign-in-secret)
        headers (js->clj (.-headers req) :keywordize-keys true)
        auth-header (:authorization headers)
        current-user (authorization/token->user (bearer-token auth-header) secret)]
    (when current-user
      (aset (.-headers req) "current-user" (pr-str current-user))))
  (next))
