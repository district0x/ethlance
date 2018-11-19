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
   [district.server.config :refer [config]]
   [district.server.smart-contracts]
   [district.server.logging]

   ;; Ethlance Mount Components
   [ethlance.server.generator]
   [ethlance.server.syncer]
   [ethlance.server.db]
   [ethlance.shared.smart-contracts]))


(def main-config
  {:web3 {:port 8545}
   :smart-contracts {:contracts-var #'ethlance.shared.smart-contracts/smart-contracts
                     :print-gas-usage? false
                     :auto-mining? false}})


(defn -main [& args]
  (log/info "Initializing Server...")
  (mount/start (mount/with-args main-config)))


(set! *main-cli-fn* -main)
