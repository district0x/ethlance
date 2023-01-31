(ns ethlance.server.graphql.utils
  (:require [cljs.nodejs :as nodejs]
            [district.graphql-utils :refer [gql->clj kw->gql-name]]
            [graphql-query.core :refer [graphql-query]]))

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

(defn run-query [{:keys [:url :type :query :access-token]
                  :or {type "query"}}]
  (let [{:keys [:query-str]} (parse-query {:queries [query]})]
    (-> (axios (clj->js (cond-> {:url url
                                 :method :post
                                 :data {"query" (str (name type) " " query-str)}}
                          access-token (assoc :headers {:access-token access-token}))))
        (.then (fn [response]
                 (gql->clj (aget response "data")))))))
