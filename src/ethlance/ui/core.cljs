(ns ethlance.ui.core
  (:require
   [mount.core :as mount :refer [defstate]]
   ;; [re-frame.core :as re-frame]
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
   [ApolloProvider]
   [ethlance.ui.graphql.client :as client]
   [ethlance.ui.page.demo :as demo]
   ))

(enable-console-print!)

(def apollo-provider (reagent/adapt-react-class ApolloProvider))

(defn root []
  [apollo-provider {:client (client/apollo-client {:graphql {:url "http://localhost:4000/graphql"}})}
   [demo/page]])

(defn rerender []
  (log/debug "re-rendering root component...")
  (reagent/render [root] (.getElementById js/document "app")))

(defn ^:export init []
  (let [main-config (ui.config/get-config)]
    (.log js/console (clj->js main-config))
    (util.injection/inject-data-scroll! {:injection-selector "#app"})
    (-> (mount/with-args main-config)
        (mount/start))
    (rerender)))


(defonce started? (init))
