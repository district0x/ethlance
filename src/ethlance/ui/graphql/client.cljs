(ns ethlance.ui.graphql.client
  (:require [ApolloClient]
            [ApolloLink]
            [HttpLink]
            [InMemoryCache]
            [defaultDataIdFromObject]
            [ethlance.shared.graphql.utils :as graphql-shared-utils]
            [ethlance.ui.graphql.middleware :as middleware]
            [ethlance.ui.graphql.utils :as graphql-ui-utils]
            [gql]
            [setContext]
            [taoensso.timbre :as log]
            [useMutation]
            [useQuery]))

(defn apollo-client [{:keys [:graphql] :as opts}]
  (let [cache (new InMemoryCache (clj->js {:dataIdFromObject (fn [object]
                                                               ;; TODO : use address or id fields
                                                               (let [entity (graphql-shared-utils/gql->clj object)
                                                                     [id-key _] (filter (fn [k] (case (name k)
                                                                                                  "address" true
                                                                                                  "id" true
                                                                                                  nil))
                                                                                        (keys entity))]
                                                                 (if id-key
                                                                   (do (log/debug "dataIdFromObject" {:id-key id-key})
                                                                       (id-key entity))
                                                                   (defaultDataIdFromObject object))))}))
        auth-middleware (setContext (middleware/auth opts))
        http-middleware (new HttpLink (clj->js {:uri (:url graphql)}))]
    (new ApolloClient (clj->js {:cache cache
                                :link (js-invoke ApolloLink "from" (clj->js [auth-middleware http-middleware]))}))))

(defn use-query [{:keys [:queries :operation :variables] :as query} & [opts]]
  (try
    (let [{:keys [:query-str]} (graphql-ui-utils/parse-query query)
          gql-query (gql query-str)
          {:keys [:data :loading :error :fetchMore :refetch]} (js->clj (useQuery gql-query (clj->js opts)) :keywordize-keys true)]
      {:query gql-query
       :loading? loading
       :error error
       :data (graphql-shared-utils/gql->clj data)
       :fetch-more fetchMore
       :refetch refetch})
    (catch :default e
      (log/error "Error in use-query" {:error e}))))

(defn use-mutation [{:keys [:queries :operation :variables] :as query} & [opts]]
  (try
    (let [{:keys [:query-str]} (graphql-ui-utils/parse-query query)
          gql-query (gql query-str)
          [call-mutation {:keys [:called :loading]}] (js->clj (useMutation gql-query (clj->js opts)) :keywordize-keys true)]
      {:call-mutation call-mutation
       :called? called
       :loading? loading})
    (catch :default e
      (log/error "Error in use-mutation" {:error e}))))

(defn read-cache [cache {:keys [:queries :operation :variables] :as query} & [opts]]
  (try
    (let [{:keys [:query-str]} (graphql-ui-utils/parse-query query)
          gql-query (gql query-str)
          data (apply js-invoke cache "readQuery" (remove nil?
                                                          [(clj->js {:query gql-query}) (clj->js opts)]))]
      (graphql-shared-utils/gql->clj data))
    (catch :default e
      (log/error "Error in read-cache" {:error e}))))

(defn write-cache [cache {:keys [:queries :operation :variables] :as query} data & [opts]]
  (try
    (let [{:keys [:query-str]} (graphql-ui-utils/parse-query query)
          gql-query (gql query-str)]
      (apply js-invoke cache "writeQuery" (remove nil?
                                                  [(clj->js {:query gql-query
                                                             :data (graphql-shared-utils/clj->gql data)}) (clj->js opts)])))
    (catch :default e
      (log/error "Error in write-cache" {:error e}))))
