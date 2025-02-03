(ns ethlance.server.config
  (:require
    [taoensso.timbre :as log]
    [ethlance.server.db :as server-db]
    [ethlance.server.new-syncer.handlers :as new-syncer.handlers]
    [ethlance.server.new-syncer :as new-syncer]
    [ethlance.shared.config :as shared-config]
    [ethlance.shared.smart-contracts-dev :as smart-contracts-dev]
    [ethlance.shared.smart-contracts-prod :as smart-contracts-prod]
    [ethlance.shared.smart-contracts-qa :as smart-contracts-qa]
    [ethlance.shared.smart-contracts-qa-base :as smart-contracts-qa-base]
    [ethlance.shared.utils :include-macros true :as shared-utils]
    ))


(def environment (shared-utils/get-environment))

(println "Ethlance server starting in environment:" environment)


(def contracts-var
  (condp = environment
    "prod" #'smart-contracts-prod/smart-contracts
    "qa" #'smart-contracts-qa/smart-contracts
    "qa-base" #'smart-contracts-qa-base/smart-contracts
    "dev" #'smart-contracts-dev/smart-contracts
    ; "dev" #'smart-contracts-qa-base/smart-contracts
    ))


(def default-config
  {:web3
   {:url  "ws://127.0.0.1:8549"
    :on-offline (fn [] (log/info "Went OFFLINE"))
    :on-online (fn [] (log/info "Back ONLINE"))
    :top-level-opts
    {:reconnect
     {:auto true
      :delay 2000
      :max-attempts 5
      :on-timeout true}}}
   :new-syncer {:auto-start-listening-new-events? false
                :handlers new-syncer.handlers/handlers
                :save-checkpoint server-db/save-processed-events-checkpoint}
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
                 :load-checkpoint server-db/load-processed-events-checkpoint
                 :save-checkpoint server-db/save-processed-events-checkpoint
                 :callback-after-past-events new-syncer/start-listening-new-events
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


(defn env-config
  [_env]
  (shared-utils/deep-merge
    default-config
    shared-config/config))
