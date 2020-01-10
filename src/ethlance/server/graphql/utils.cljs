(ns ethlance.server.graphql.utils
  (:require
   [cljs.nodejs :as nodejs]
   [ethlance.shared.graphql.utils :refer [kw->gql-name gql->clj]]
   [graphql-query.core :refer [graphql-query]]
   [taoensso.timbre :as log]
   ))

(def axios (nodejs/require "axios"))
(def graphql (nodejs/require "graphql"))
(def parse-graphql (aget graphql "parse"))

(defn parse-query [query]
  (cond
    (string? query)
    {:query-str query :query (parse-graphql query)}

    (map? query)
    (let [query-str (graphql-query query {:kw->gql-name kw->gql-name})]
      {:query-str query-str
       :query (parse-graphql query-str)})))

(defn run-query [{:keys [:url :type :query]
                  :or {type "query"}}]
  (let [{:keys [:query-str]} (parse-query {:queries [query]})]
    (-> (axios (clj->js {:url url
                       :method :post
                       :data {type query-str}}))
        (.then (fn [response]
                 (gql->clj (aget response "data")))))))
