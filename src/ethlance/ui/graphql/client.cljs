(ns ethlance.ui.graphql.client
  (:require
   [ApolloClient]
   [ApolloLink]
   [HttpLink]
   [InMemoryCache]
   [defaultDataIdFromObject]
   [ethlance.shared.graphql.utils :as graphql-shared-utils]
   [ethlance.ui.graphql.utils :as graphql-ui-utils]
   [ethlance.ui.graphql.middleware :as middleware]
   [setContext]
   [useQuery]
   [useMutation]
   [gql]
   [taoensso.timbre :as log]))

(defn apollo-client [{:keys [:graphql] :as opts}]
  (let [cache (new InMemoryCache (clj->js {:dataIdFromObject (fn [object]
                                                               (let [entity (graphql-shared-utils/gql->clj object)
                                                                     [id-key _] (filter #(= "id" (name %))
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
