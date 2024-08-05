(ns ethlance.server.core
  (:require
    ["fs" :as fs]
    [district.server.async-db]
    [district.server.config :refer [config]]
    [district.server.db]
    [district.server.db.honeysql-extensions]
    [district.server.logging]
    [district.server.smart-contracts]
    [ethlance.server.tracing.core]
    [district.server.web3]
    [district.server.web3-events]
    [district.shared.async-helpers :as async-helpers :refer [safe-go]]
    [ethlance.server.db]
    [ethlance.server.graphql.server]
    [ethlance.server.ipfs]
    [ethlance.server.syncer]
    [ethlance.shared.config :as shared-config]
    [ethlance.shared.smart-contracts-dev :as smart-contracts-dev]
    [ethlance.shared.smart-contracts-prod :as smart-contracts-prod]
    [ethlance.shared.smart-contracts-qa :as smart-contracts-qa]
    [ethlance.shared.utils :include-macros true :as shared-utils]
    [mount.core :as mount]
    [taoensso.timbre :refer [merge-config!] :as log]))


(def environment (shared-utils/get-environment))

(println "Ethlance server starting in environment:" environment)


(def contracts-var
  (condp = environment
    "prod" #'smart-contracts-prod/smart-contracts
    "qa" #'smart-contracts-qa/smart-contracts
    "dev" #'smart-contracts-dev/smart-contracts))


(def default-config
  {:web3 {:url  "ws://127.0.0.1:8549"}
   :web3-events {:events
                 {:ethlance/job-created [:ethlance :JobCreated]
                  :ethlance/invoice-created [:ethlance :InvoiceCreated]
                  :ethlance/invoice-paid [:ethlance :InvoicePaid]
                  :ethlance/dispute-raised [:ethlance :DisputeRaised]
                  :ethlance/dispute-resolved [:ethlance :DisputeResolved]
                  :ethlance/candidate-added [:ethlance :CandidateAdded]
                  :ethlance/quote-for-arbitration-set [:ethlance :QuoteForArbitrationSet]
                  :ethlance/quote-for-arbitration-accepted [:ethlance :QuoteForArbitrationAccepted]
                  :ethlance/job-ended [:ethlance :JobEnded]
                  :ethlance/arbiters-invited [:ethlance :ArbitersInvited]
                  :ethlance/funds-in [:ethlance :FundsIn]
                  :ethlance/funds-out [:ethlance :FundsOut]
                  :ethlance/test-event [:ethlance :TestEvent]}
                 :from-block 1000
                 :block-step 5 ; 1000
                 :dispatch-logging? true
                 :crash-on-event-fail? true
                 :skip-past-events-replay? false
                 :write-events-into-file? true
                 :load-checkpoint ethlance.server.db/load-processed-events-checkpoint
                 :save-checkpoint ethlance.server.db/save-processed-events-checkpoint
                 }
   :smart-contracts {:contracts-var contracts-var
                     :contracts-build-path "../resources/public/contracts/build"
                     :print-gas-usage? false
                     :auto-mining? false}
   :graphql {:port 6300
             :sign-in-secret "SECRET"
             :graphiql (= environment "dev")}
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


(def config-dev
  {:ipfs
   {:host "https://ipfs.infura.io:5001"
    :endpoint "/api/v0"
    :gateway "https://ethlance-qa.infura-ipfs.io/ipfs"
    :auth {:username "xxx"
           :password "xxx"}}})


(defn env-config
  [env]
  (shared-utils/deep-merge
    default-config
    (if (= env "dev")
      config-dev
      shared-config/config)))


(defn -main
  [& _]
  (log/info "Initializing Server...")
  (async-helpers/extend-promises-as-channels!)
  (merge-config! {:ns-blacklist ["district.server.smart-contracts"]})
  (safe-go
    (try
      (let [start-result (-> (mount/with-args
                               {:config
                                {:env-name "SERVER_CONFIG_PATH"
                                 :default (env-config environment)}})
                             (mount/start))]
        (log/warn "Started" {:components start-result :config @config}))
      (catch js/Error e
        (log/error "Something went wrong when starting the application" {:error e})))))


;; When compiled for a command-line target, whatever function *main-cli-fn* is
;; set to will be called with the command-line argv as arguments.
;;   See: https://cljs.github.io/api/cljs.core/STARmain-cli-fnSTAR
(set! *main-cli-fn* -main)
