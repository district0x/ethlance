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
   ;; [district.server.web3]
   ;; [district.server.web3-events]
   [district.server.config :refer [config]]
   ;; [district.server.smart-contracts]
   [district.server.logging]

   ;; District Libraries

   ;; Ethlance Mount Components
   [ethlance.server.graphql.server]
   ;; [ethlance.server.syncer]
   [district.server.db]
   [ethlance.server.db]
   [ethlance.server.ipfs]
   [ethlance.shared.smart-contracts-prod :as smart-contracts-prod]
   [ethlance.shared.smart-contracts-qa :as smart-contracts-qa]
   [ethlance.shared.smart-contracts-dev :as smart-contracts-dev]

   ;; Ethlance Libraries

   [ethlance.shared.utils :as shared-utils]))

(def graphql-config
  {:port 4000})

(def contracts-var
  (condp = (shared-utils/get-environment)
    "prod" #'smart-contracts-prod/smart-contracts
    "qa" #'smart-contracts-qa/smart-contracts
    "dev" #'smart-contracts-dev/smart-contracts))

(def default-config
  {;;:web3 {:port 8549}

   ;; :web3-events {:events {:ethlance-registry/ethlance-event [:ethlance-registry :EthlanceEvent {} {:from-block 0 :to-block "latest"}]}
   ;;               :write-events-into-file? true
   ;;               :file-path "ethlance-events.log"}

   ;; :smart-contracts {:contracts-var contracts-var
   ;;                   :print-gas-usage? false
   ;;                   :auto-mining? false}

   :graphql graphql-config

   :ethlance/db {:resync? false}

   :ipfs {:host "http://127.0.0.1:5001"
          :endpoint "/api/v0"
          :gateway "http://127.0.0.1:8080/ipfs"}

   :logging {:level "info"
             :console? true}})

(defn -main [& args]
  (log/info "Initializing Server...")
  (-> (mount/with-args {:config {:default default-config}})
      (mount/start)
      (as-> $ (log/warn "Started" {:components $
                                   :config @config}))))


(set! *main-cli-fn* -main)
