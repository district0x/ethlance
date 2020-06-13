(ns ethlance.ui.core
  (:require
   [mount.core :as mount :refer [defstate]]
   [re-frame.core :as re]
   [taoensso.timbre :as log]

   ;; District UI Components
   [district.ui.component.router]
   [district.ui.graphql]
   [district.ui.logging]
   [district.ui.reagent-render]
   [district.ui.router]

   ;; Ethlance
   [ethlance.ui.config :as ui.config]
   [ethlance.ui.effects]
   [ethlance.ui.events]
   [ethlance.ui.pages]
   [ethlance.ui.subscriptions]
   [ethlance.ui.util.injection :as util.injection]))


(enable-console-print!)


(defn ^:export init []
  (let [main-config (ui.config/get-config)]
    (.log js/console "Initializing...")
    (.log js/console (clj->js main-config))
    (util.injection/inject-data-scroll! {:injection-selector "#app"})

    ;; Initialize our district re-mount components
    (-> (mount/with-args main-config)
        (mount/start))

    ;; Initialize our re-frame app state
    (re/dispatch-sync [:ethlance/initialize])

    ::started))


(defonce started? (init))
