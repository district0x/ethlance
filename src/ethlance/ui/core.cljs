(ns ethlance.ui.core
  (:require
   [mount.core :as mount :refer [defstate]]
   [re-frame.core :as rf]
   [taoensso.timbre :as log]

   ;; District UI Components
   ;; [district.ui.reagent-render]
   [district.ui.router]
   [district.ui.component.router :as router]
   [district.ui.logging]

   ;; Ethlance
   [ethlance.ui.config :as ui.config]
   [ethlance.ui.pages]
   [ethlance.ui.util.injection :as util.injection]

   ;; Events
   [ui.ethlance.ui.events]

   ;; Fxs
   [ui.ethlance.ui.fxs]

   [reagent.core :as reagent]

   ;; apollo-client
   [ethlance.shared.graphql.utils :as graphql-utils]
   [ApolloClient]
   [defaultDataIdFromObject]
   [InMemoryCache]
   [setContext]
   [ApolloLink]
   [ApolloProvider]
   ))

(enable-console-print!)

(def apollo-provider (reagent/adapt-react-class ApolloProvider))

(defn middleware [opts]
  (fn [_ request]
    (let [{:keys [:headers]} (js->clj request)]
      {:headers (merge headers
                       ;; TODO : hardcoded, read from localstore
                       {:access-token "topsecret"})})))

(defn client [{:keys [:graphql] :as opts}]
  (let [cache (new InMemoryCache (clj->js {:dataIdFromObject (fn [object]
                                                               (let [entity (graphql-utils/gql->clj object)
                                                                     [id-key _] (filter #(= "id" (name %))
                                                                                        (keys entity))]
                                                                 (if id-key
                                                                   (do (log/debug "dataIdFromObject" {:id-key id-key})
                                                                       (id-key entity))
                                                                   (defaultDataIdFromObject object))))}))
        auth-middleware (setContext (middleware opts))]
    (new ApolloClient (clj->js {:cache cache
                                :link (js-invoke ApolloLink "from" (clj->js [auth-middleware]))}))))

(defn root []
  (fn []
    [apollo-provider {:client (client {:graphql {:url "http://localhost:4000/graphql"}})}

     [:div
      [:h2 "Apollo!!"]]]

    ))


(defn rerender []
  (log/debug "re-rendering root component...")
  (reagent/render [root] (.getElementById js/document "app")))

;; TODO : provider
(defn ^:export init []
  (let [main-config (ui.config/get-config)
        ;; apollo-client (client main-config)

        ]
    (.log js/console (clj->js main-config))

    ;; (util.injection/inject-data-scroll! {:injection-selector "#app"})
    #_(-> (mount/with-args main-config)
        (mount/start))
    (rerender)
    ))


(defonce started? (init))
