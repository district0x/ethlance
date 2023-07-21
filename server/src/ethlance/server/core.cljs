(ns ethlance.server.core
  (:require
    [alphabase.base58 :as base58]
    [alphabase.hex :as hex]
    [district.server.async-db]
    [district.server.config :refer [config]]
    [district.server.db.honeysql-extensions]
    [district.server.db]
    [district.server.logging]
    [district.server.smart-contracts]
    [district.server.web3-events]
    [district.server.web3]
    [district.shared.async-helpers :as async-helpers :refer [safe-go]]
    [ethlance.server.db]
    [ethlance.server.graphql.server]
    [ethlance.server.ipfs]
    [ethlance.server.syncer]
    [ethlance.shared.smart-contracts-dev :as smart-contracts-dev]
    [tests.graphql.generator]
    [ethlance.shared.smart-contracts-prod :as smart-contracts-prod]
    [ethlance.shared.smart-contracts-qa :as smart-contracts-qa]
    [ethlance.shared.utils :as shared-utils]
    [mount.core :as mount]
    [taoensso.timbre :refer [merge-config!] :as log]))

(def environment (shared-utils/get-environment))

(def graphql-config
  {:port 6300
   :sign-in-secret "SECRET"
   :graphiql (= environment "dev")})

(def contracts-var
  (condp = environment
    "prod" #'smart-contracts-prod/smart-contracts
    "qa" #'smart-contracts-qa/smart-contracts
    "dev" #'smart-contracts-dev/smart-contracts))

(def default-config
  {
   :web3 {:url  "ws://127.0.0.1:8549"} ; "ws://d0x-vm:8549"
   :web3-events {:events {:ethlance/job-created [:ethlance :JobCreated]
                          :ethlance/invoice-created [:ethlance :InvoiceCreated]
                          :ethlance/invoice-paid [:ethlance :InvoicePaid]
                          :ethlance/dispute-raised [:ethlance :DisputeRaised]
                          :ethlance/dispute-resolved [:ethlance :DisputeResolved]
                          :ethlance/candidate-added [:ethlance :CandidateAdded]
                          :ethlance/quote-for-arbitration-set [:ethlance :QuoteForArbitrationSet]
                          :ethlance/quote-for-arbitration-accepted [:ethlance :QuoteForArbitrationAccepted]
                          :ethlance/job-ended [:ethlance :JobEnded]
                          :ethlance/test-event [:ethlance :TestEvent]
                          }
                 :from-block 0 ; 53; (:last-processed-block (read-edn-sync "ethlance-events.log"))
                 :block-step 1000
                 :dispatch-logging? true
                 :crash-on-event-fail? true
                 :skip-past-events-replay? true
                 :write-events-into-file? true
                 :checkpoint-file "ethlance-events.log"}
   :smart-contracts {:contracts-var contracts-var
                     :contracts-build-path "../resources/public/contracts/build"
                     :print-gas-usage? false
                     :auto-mining? false}
   :graphql graphql-config
   :district/db {:user "ethlanceuser"
                 :host "localhost"
                 :database "ethlance"
                 :password "pass"
                 :port 5432}
   :ethlance/db {:resync? false}
   :ipfs {:host "http://host-machine:5001"
          :endpoint "/api/v0"
          :gateway "http://host-machine:8080/ipfs"}
   :logging {:level "debug"
             :console? true}})

(defn -main [& _]
  (log/info "Initializing Server...")
  (async-helpers/extend-promises-as-channels!)
  (println "starting with " default-config)
  (merge-config!
   {:ns-blacklist ["district.server.smart-contracts"]})
  (safe-go
   (try
     (let [start-result (-> (mount/with-args {:config {:default default-config}})
                               (mount/start))]
       (log/warn "Started" {:components start-result
                            :config @config}))
     (catch js/Error e
       (log/error "Something went wrong when starting the application" {:error e})))))


; When compiled for a command-line target, whatever function *main-cli-fn* is
; set to will be called with the command-line argv as arguments.
;   See: https://cljs.github.io/api/cljs.core/STARmain-cli-fnSTAR
(set! *main-cli-fn* -main)
