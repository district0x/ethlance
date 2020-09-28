(ns ethlance.ui.core
  (:require
   district.ui.component.router
   district.ui.ipfs
   district.ui.logging
   district.ui.reagent-render
   district.ui.router
   district.ui.web3-accounts
   [ethlance.ui.config :as ui.config]
   ethlance.ui.effects
   ethlance.ui.events
   ethlance.ui.pages
   ethlance.ui.subscriptions
   [ethlance.ui.util.injection :as util.injection]
   [mount.core :as mount :refer [defstate]]
   [re-frame.core :as re]
   [re-frisk.core :as re-frisk]
   [taoensso.timbre :as log]))

(enable-console-print!)

(defn ^:export init []
  (let [main-config (ui.config/get-config)]
    (re-frisk/enable-re-frisk!)
    (.log js/console (clj->js main-config))
    (util.injection/inject-data-scroll! {:injection-selector "#app"})

    ;; Initialize our district re-mount components
    (-> (mount/with-args main-config)
        (mount/start))

    ;; Initialize our re-frame app state
    (re/dispatch-sync [:ethlance/initialize main-config])

    ::started))
