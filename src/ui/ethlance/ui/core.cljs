(ns ethlance.ui.core
  (:require
   [mount.core :as mount :refer [defstate]]
   [re-frame.core :as rf]
   [taoensso.timbre :as log]

   ;; District UI Components
   [district.ui.reagent-render]

   ;; Ethlance
   [ethlance.ui.config :as ui.config]))


(enable-console-print!)


(defn ^:export init []
  (let [main-config (ui.config/get-config)]
    (.log js/console "Initializing...")
    (-> (mount/with-args main-config)
        (mount/start))))
