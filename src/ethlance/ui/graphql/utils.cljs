(ns ethlance.ui.graphql.utils
  (:require
   [parseGraphql]
   [ethlance.shared.graphql.utils :as graphql-utils]
   [graphql-query.core :as graphql-query-core :refer [graphql-query fragment->str]]
   [taoensso.timbre :as log]))

(def parse-graphql js/parseGraphql)

(defn parse-query [query]
  (cond
    (string? query)
    {:query-str query :query (parse-graphql query)}

    (map? query)
    (let [query-str (graphql-query query {:kw->gql-name graphql-utils/kw->gql-name})]
      {:query-str query-str
       :query (parse-graphql query-str)})))
