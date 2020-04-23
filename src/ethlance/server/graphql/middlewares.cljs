(ns ethlance.server.middlewares
  (:require [district.shared.async-helpers :as async-helpers]
            [district.graphql-utils :as graphql-utils]
            [taoensso.timbre :as log]
            [clojure.string :as str]
            [ethlance.server.db :as ethlance-db]))

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

(defn save-mutation-express-middleware [req res next]
  (let [headers (.-headers req)
        body (.-body req)
        query (.-query body)]
    (when (str/starts-with? query "mutation")
      (ethlance-db/save-graphql-mutation-event {:headers (js->clj headers)
                                                :body (js->clj body)})))
  (next))
