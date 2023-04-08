(ns ethlance.ui.config
  (:require [district.ui.component.router :as router]
            [ethlance.shared.smart-contracts-dev :as smart-contracts-dev]
            [ethlance.shared.smart-contracts-prod :as smart-contracts-prod]
            [ethlance.shared.smart-contracts-qa :as smart-contracts-qa]
            [ethlance.shared.graphql.schema :refer [schema]]
            [ethlance.shared.routes :as routes]))

(def general-config
  ; config of https://github.com/district0x/district-ui-smart-contracts
  {:smart-contracts {:format :truffle-json
                     :load-path "../resources/public/contracts/build/"
                     :contracts smart-contracts-dev/smart-contracts}
   :logging
   {:level :info
    :console? true}
   :reagent-render
   {:id "app"
    :component-var #'router/router}
   :router
   {:routes routes/routes
    :default-route :route/home
    :scroll-top? true
    :html5? true}
   :web3 {:url "http://d0x-vm:8549"} ; "https://mainnet.infura.io/"
   :web3-tx {:disable-using-localstorage? true}
   :ipfs
   {:endpoint "/api/v0"
    :host "http://host-machine:5001"
    :gateway "http://host-machine/ipfs"}
   :server-config {:url "http://d0x-vm:6300/config" :format :json}
   :graphql
   {:schema schema
    :url "http://d0x-vm:6300/graphql"
    :jwt-sign-secret "SECRET"}
   :root-url "http://d0x-vm:6500"
   :github
   {:client-id "83e6a6043ca4ae50f8b0"}
   :linkedin
   {:client-id "86csctqngadad5"}
   :conversion-rates {:from-currencies [:ETH]
                      :to-currencies [:USD]}
   })

(def development-config
  (-> general-config
      (assoc-in [:logging :level] :debug)
      (assoc-in [:router :routes] routes/dev-routes)))

;; TODO: generate based on whether dev, prod, qa
(defn get-config []
  development-config)
