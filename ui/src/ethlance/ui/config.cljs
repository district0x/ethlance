(ns ethlance.ui.config
  (:require [district.ui.component.router :as router]
            [ethlance.shared.smart-contracts-dev :as smart-contracts-dev]
            [ethlance.shared.smart-contracts-prod :as smart-contracts-prod]
            [ethlance.shared.smart-contracts-qa :as smart-contracts-qa]
            [ethlance.shared.graphql.schema :refer [schema]]
            [district.graphql-utils]
            [ethlance.shared.routes :as routes]))

; gql-name->kw transforms "erc20" to :erc-20
; This is a small wrapper that maintains these, e.g.
; "erc1155" -> :erc1155
; Source:
;   - https://github.com/district0x/district-graphql-utils/blob/e814beb9222c9d029a78a39b1c78f6644f0aa4c6/src/district/graphql_utils.cljs#L43
;   - https://github.com/district0x/district-graphql-utils/blob/e814beb9222c9d029a78a39b1c78f6644f0aa4c6/src/district/graphql_utils.cljs#L77
;   - https://github.com/clj-commons/camel-snake-kebab/blob/ac08444c94aca4cba25d86f3b3faf36596809380/src/camel_snake_kebab/internals/string_separator.cljc#L42
(defn token-type-fixed-gql-name->kw [s]
  (let [fixed-names #{"erc20" "erc721" "erc1155"}]
    (if (contains? fixed-names s)
      (keyword s)
      (district.graphql-utils/gql-name->kw s))))

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
   :notification {:default-show-duration 5000
                  :default-hide-duration 1000}
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
    :jwt-sign-secret "SECRET"
    :gql-name->kw token-type-fixed-gql-name->kw}
   :root-url "http://d0x-vm:6500"
   :github
   {:client-id "83e6a6043ca4ae50f8b0"}
   :linkedin
   {:client-id "86csctqngadad5"}
   :conversion-rates {:from-currencies [:ETH :USD]
                      :to-currencies [:USD :ETH]}
   })

(def development-config
  (-> general-config
      (assoc-in [:logging :level] :debug)
      (assoc-in [:router :routes] routes/dev-routes)))

;; TODO: generate based on whether dev, prod, qa
(defn get-config []
  development-config)
