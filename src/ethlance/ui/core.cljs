(ns ethlance.ui.core
  (:require
   [mount.core :as mount :refer [defstate]]
   [re-frame.core :as re]
   [taoensso.timbre :as log]

   ;; District UI Components
   [district.ui.reagent-render]
   [district.ui.router]
   [district.ui.component.router]
   [district.ui.logging]

   ;; Ethlance
   [ethlance.ui.config :as ui.config]
   [ethlance.ui.pages]
   [ethlance.ui.util.injection :as util.injection]
   [ethlance.ui.events]
   [ethlance.ui.effects]
   [ethlance.ui.subscriptions]))


(enable-console-print!)


(defn ^:export init []
  (let [main-config (ui.config/get-config)]
    (.log js/console "Initializing...")
    (.log js/console (clj->js main-config))
    (util.injection/inject-data-scroll! {:injection-selector "#app"})
    (-> (mount/with-args main-config)
        (mount/start))))


(defonce started? (init))
