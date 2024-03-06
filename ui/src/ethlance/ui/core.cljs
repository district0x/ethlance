(ns ethlance.ui.core
  (:require
    [akiroz.re-frame.storage :refer [reg-co-fx!]]
    [cljsjs.apollo-fetch]
    [cljsjs.dataloader]
    [district.ui.component.router]
    [district.ui.conversion-rates]
    [district.ui.graphql]
    [district.ui.ipfs]
    [district.ui.logging]
    [district.ui.notification]
    [district.ui.reagent-render]
    [district.ui.router]
    [district.ui.server-config]
    [district.ui.smart-contracts]
    [district.ui.web3]
    [district.ui.web3-account-balances]
    [district.ui.web3-accounts]
    [district.ui.web3-tx]
    [district0x.re-frame.web3-fx]
    [ethlance.shared.utils :as shared-utils]
    ; to register effect :web3-tx-localstorage
    [ethlance.ui.config :as ui.config]
    [ethlance.ui.effects]
    [ethlance.ui.events]
    [ethlance.ui.pages]
    [ethlance.ui.subscriptions]
    [ethlance.ui.util.injection :as util.injection]
    [mount.core :as mount]
    [print.foo :include-macros true]
    [re-frame.core :as re]))


(enable-console-print!)

(def environment (shared-utils/get-environment))


(defn fetch-config-from-server
  [url callback]
  (-> (js/fetch url)
      (.then ,,, (fn [response] (.json response)))
      (.then ,,, (fn [config] (callback (js->clj config {:keywordize-keys true}))))))


(defn ^:export init
  []
  (let [main-config (ui.config/get-config environment)]
    (util.injection/inject-data-scroll! {:injection-selector "#app"})

    (fetch-config-from-server
      (get-in main-config [:server-config :url])
      (fn [config]
        ;; Initialize our district re-mount components
        (-> (mount/with-args (shared-utils/deep-merge main-config config))
            (mount/start))

        (reg-co-fx! :ethlance {:fx :store :cofx :store})

        ;; Initialize our re-frame app state
        (re/dispatch-sync [:ethlance/initialize (shared-utils/deep-merge main-config config)])))

    ::started))
