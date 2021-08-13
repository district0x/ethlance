(ns ethlance.server.core
  (:require [district.server.async-db]
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
            ; [ethlance.server.syncer]
            [ethlance.shared.smart-contracts-dev :as smart-contracts-dev]
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
  {:web3 {:url  "ws://127.0.0.1:8549"} ; "ws://d0x-vm:8549"
   :web3-events {:events {
                          ; TODO: replace with events from new Ethlance.sol contract (commented out to allow server to start)
                          ; :ethlance-issuer/arbiters-invited [:ethlance-issuer :ArbitersInvited]

                          ; :standard-bounties/bounty-issued [:standard-bounties :BountyIssued]
                          ; :standard-bounties/bounty-approvers-updated [:standard-bounties :BountyApproversUpdated]
                          ; :standard-bounties/contribution-added [:standard-bounties :ContributionAdded]
                          ; :standard-bounties/contribution-refunded [:standard-bounties :ContributionRefunded]
                          ; :standard-bounties/contributions-refunded [:standard-bounties :ContributionsRefunded]
                          ; :standard-bounties/bounty-drained [:standard-bounties :BountyDrained]
                          ; :standard-bounties/action-performed [:standard-bounties :ActionPerformed]
                          ; :standard-bounties/bounty-fulfilled [:standard-bounties :BountyFulfilled]
                          ; :standard-bounties/fulfillment-updated [:standard-bounties :FulfillmentUpdated]
                          ; :standard-bounties/fulfillment-accepted [:standard-bounties :FulfillmentAccepted]
                          ; :standard-bounties/bounty-changed [:standard-bounties :BountyChanged]
                          ; :standard-bounties/bounty-issuers-updated [:standard-bounties :BountyIssuersUpdated]
                          ; :standard-bounties/bounty-data-changed [:standard-bounties :BountyDataChanged]
                          ; :standard-bounties/bounty-deadline-changed [:standard-bounties :BountyDeadlineChanged]

                          ; :ethlance-jobs/job-issued [:ethlance-jobs :JobIssued]
                          ; :ethlance-jobs/contribution-added [:ethlance-jobs :ContributionAdded]
                          ; :ethlance-jobs/contribution-refunded [:ethlance-jobs :ContributionRefunded]
                          ; :ethlance-jobs/contributions-refunded [:ethlance-jobs :ContributionsRefunded]
                          ; :ethlance-jobs/job-drained [:ethlance-jobs :JobDrained]
                          ; :ethlance-jobs/job-invoice [:ethlance-jobs :JobInvoice]
                          ; :ethlance-jobs/invoice-accepted [:ethlance-jobs :InvoiceAccepted]
                          ; :ethlance-jobs/job-changed [:ethlance-jobs :JobChanged]
                          ; :ethlance-jobs/job-issuers-updated [:ethlance-jobs :JobIssuersUpdated]
                          ; :ethlance-jobs/job-approvers-updated [:ethlance-jobs :JobApproversUpdated]
                          ; :ethlance-jobs/job-data-changed [:ethlance-jobs :JobDataChanged]
                          ; :ethlance-jobs/candidate-accepted [:ethlance-jobs :CandidateAccepted]
                          }
                 :from-block 1
                 :block-step 1000
                 :crash-on-event-fail? true
                 :skip-past-events-replay? true
                 :write-events-into-file? true
                 :file-path "ethlance-events.log"}
   :smart-contracts {:contracts-var contracts-var
                     :print-gas-usage? false
                     :auto-mining? false}
   :graphql graphql-config
   :district/db {:user "user"
                 :host "localhost"
                 :database "ethlance"
                 :password "pass"
                 :port 5432}
   :ethlance/db {:resync? false}
   :ipfs {:host "http://d0x-vm:5001"
          :endpoint "/api/v0"
          :gateway "http://d0x-vm:8080/ipfs"}
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
     (let [start-result #_<?
           (-> (mount/with-args {:config {:default default-config}})
               (mount/start))]
       (log/warn "Started" {:components start-result
                            :config @config}))
     (catch js/Error e
       (log/error "Something went wrong when starting the application" {:error e})))))


(set! *main-cli-fn* -main)
