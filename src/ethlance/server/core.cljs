(ns ethlance.server.core
  "Main entrypoint for an ethlance production server.
  
  Notes:

  - For Development, ./dev/server/ethlance/server/user.cljs is the main entrypoint.

  "
  (:require
   [mount.core :as mount]
   [taoensso.timbre :as log]
   [cljs.nodejs :as nodejs]
   [cljs-web3.eth :as web3-eth]

   ;; District Mount Components
   [district.server.web3]
   [district.server.web3-events]
   [district.server.config :refer [config]]
   [district.server.smart-contracts]
   [district.server.logging]

   ;; District Libraries
   [district.graphql-utils :as graphql-utils]
   [district.server.graphql]
   [district.server.graphql.utils :refer [build-schema build-default-field-resolver]]

   ;; Ethlance Mount Components
   [ethlance.server.syncer]
   [ethlance.server.db]
   [ethlance.server.ipfs]
   [ethlance.shared.smart-contracts-prod :as smart-contracts-prod]

   ;; Ethlance Libraries
   [ethlance.shared.graphql.schema :refer [graphql-schema]]
   [ethlance.server.graphql.resolver :refer [graphql-resolver-map]]

   [ethlance.server.graphql.mutations.sign-in :as sign-in]))

(def graphql-config
  {:port 6200
   :path "/graphql"
   :middlewares [sign-in/session-middleware]
   :schema (build-schema graphql-schema graphql-resolver-map
                         {:kw->gql-name graphql-utils/kw->gql-name
                          :gql-name->kw graphql-utils/gql-name->kw})
   :field-resolver (build-default-field-resolver graphql-utils/gql-name->kw)
   :graphiql false})


(def main-config
  {:web3 {:port 8549}

   :web3-events {:events {:ethlance-registry/ethlance-event [:ethlance-registry :EthlanceEvent {} {:from-block 0 :to-block "latest"}]}
                 :write-events-into-file? true
                 :file-path "ethlance-events.log"}

   :smart-contracts {:contracts-var #'smart-contracts-prod/smart-contracts
                     :print-gas-usage? false
                     :auto-mining? false}

   :graphql graphql-config

   :ipfs {:host "http://127.0.0.1:5001"
          :endpoint "/api/v0"
          :gateway "http://127.0.0.1:8080/ipfs"}})


(defn -main [& args]
  (log/info "Initializing Server...")
  (mount/start (mount/with-args main-config)))


(set! *main-cli-fn* -main)
