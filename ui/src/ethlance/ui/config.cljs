(ns ethlance.ui.config
  (:require
    [district.graphql-utils]
    [district.ui.component.router :as router]
    [ethlance.shared.graphql.schema :refer [schema]]
    [ethlance.shared.routes :as routes]
    [ethlance.shared.smart-contracts-dev :as smart-contracts-dev]
    [ethlance.shared.smart-contracts-prod :as smart-contracts-prod]
    [ethlance.shared.smart-contracts-qa :as smart-contracts-qa]
    [ethlance.shared.utils :include-macros true :as shared-utils]))


;; gql-name->kw transforms "erc20" to :erc-20
;; This is a small wrapper that maintains these, e.g.
;; "erc1155" -> :erc1155
;; Source:
;;   - https://github.com/district0x/district-graphql-utils/blob/e814beb9222c9d029a78a39b1c78f6644f0aa4c6/src/district/graphql_utils.cljs#L43
;;   - https://github.com/district0x/district-graphql-utils/blob/e814beb9222c9d029a78a39b1c78f6644f0aa4c6/src/district/graphql_utils.cljs#L77
;;   - https://github.com/clj-commons/camel-snake-kebab/blob/ac08444c94aca4cba25d86f3b3faf36596809380/src/camel_snake_kebab/internals/string_separator.cljc#L42
(defn token-type-fixed-gql-name->kw
  [s]
  (let [fixed-names #{"erc20" "erc721" "erc1155"}]
    (if (contains? fixed-names s)
      (keyword s)
      (district.graphql-utils/gql-name->kw s))))


(def environment (shared-utils/get-environment))


(def contracts-var
  (condp = environment
    "prod" smart-contracts-prod/smart-contracts
    "qa" smart-contracts-qa/smart-contracts
    "dev" smart-contracts-dev/smart-contracts))


(def default-config
  ;; config of https://github.com/district0x/district-ui-smart-contracts
  {:logging
   {:level :info
    :console? true}
   :reagent-render
   {:id "app"
    :component-var #'router/router}
   :notification {:default-show-duration 5000
                  :default-hide-duration 1000}
   :router
   {:routes routes/dev-routes; routes/routes
    :default-route :route/home
    :scroll-top? true
    :html5? true}
   :web3-tx {:disable-using-localstorage? true}
   :graphql
   {:schema schema
    :url "http://d0x-vm:6300/graphql"
    :jwt-sign-secret "SECRET"
    :gql-name->kw token-type-fixed-gql-name->kw}

   :smart-contracts {:format :truffle-json
                     :load-method :request
                     :contracts contracts-var}
   :root-url "http://d0x-vm:6500"
   :github
   {:client-id "83e6a6043ca4ae50f8b0"}
   :linkedin
   {:client-id "86csctqngadad5"}
   :conversion-rates {:from-currencies [:ETH :USD]
                      :to-currencies [:USD :ETH]}})


(def config-dev
  {:logging {:level :debug}
   :web3 {:url "http://d0x-vm:8549"} ; "https://mainnet.infura.io/"
   :server-config {:url "http://d0x-vm:6300/config" :format :json}})


(def config-qa
  {:server-config {:url "https://ethlance-api.qa.district0x.io/config"}})


(def config-prod
  {:server-config {:url "http://api.ethlance.com"}})


(defn get-config
  ([] (get-config environment))
  ([env]
   (shared-utils/deep-merge
     default-config
     (case env
       "dev" config-dev
       "qa" config-qa
       "prod" config-prod))))
