(ns ethlance.ui.graphql.utils
  (:require
   [parseGraphql]
   ;; [printGraphql]
   [ethlance.shared.graphql.utils :as graphql-utils]
   [graphql-query.core :as graphql-query-core :refer [graphql-query fragment->str]]
   [taoensso.timbre :as log]))

(def parse-graphql js/parseGraphql)
;; (def print-graphql js/printGraphql)

(defn parse-query [query]
  (cond
    (string? query)
    {:query-str query :query (parse-graphql query)}

    (map? query)
    (let [query-str (graphql-query query {:kw->gql-name graphql-utils/kw->gql-name})]
      {:query-str query-str
       :query (parse-graphql query-str)})

    #_:else
    #_{:query-str (print-str-graphql query)
     :query query}))
