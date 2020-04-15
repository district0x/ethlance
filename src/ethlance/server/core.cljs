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
   [district.shared.async-helpers :as async-helpers :refer [promise->]]

   ;; Ethlance Mount Components
   [ethlance.server.graphql.server]
   [ethlance.server.syncer]
   [district.server.db]
   [ethlance.server.db]
   [ethlance.server.ipfs]
   [ethlance.shared.smart-contracts-prod :as smart-contracts-prod]
   [ethlance.shared.smart-contracts-qa :as smart-contracts-qa]
   [ethlance.shared.smart-contracts-dev :as smart-contracts-dev]

   ;; Ethlance Libraries

   [ethlance.shared.utils :as shared-utils]


   [ethlance.server.workbench]))

(def graphql-config
  {:port 4000})

(def contracts-var
  (condp = (shared-utils/get-environment)
    "prod" #'smart-contracts-prod/smart-contracts
    "qa" #'smart-contracts-qa/smart-contracts
    "dev" #'smart-contracts-dev/smart-contracts))

(def default-config
  {:web3 {:url "ws://127.0.0.1:8549"}

   :web3-events {:events {
                          :standard-bounties/bounty-issued [:standard-bounties :BountyIssued {} {:from-block 0 :to-block "latest"}]
                          :standard-bounties/bounty-approvers-updated [:standard-bounties :BountyApproversUpdated {} {:from-block 0 :to-block "latest"}]
                          :standard-bounties/contribution-added [:standard-bounties :ContributionAdded {} {:from-block 0 :to-block "latest"}]
                          :standard-bounties/contribution-refunded [:standard-bounties :ContributionRefunded {} {:from-block 0 :to-block "latest"}]
                          :standard-bounties/contributions-refunded [:standard-bounties :ContributionsRefunded {} {:from-block 0 :to-block "latest"}]
                          :standard-bounties/bounty-drained [:standard-bounties :BountyDrained {} {:from-block 0 :to-block "latest"}]
                          :standard-bounties/action-performed [:standard-bounties :ActionPerformed {} {:from-block 0 :to-block "latest"}]
                          :standard-bounties/bounty-fulfilled [:standard-bounties :BountyFulfilled {} {:from-block 0 :to-block "latest"}]
                          :standard-bounties/fulfillment-updated [:standard-bounties :FulfillmentUpdated {} {:from-block 0 :to-block "latest"}]
                          :standard-bounties/fulfillment-accepted [:standard-bounties :FulfillmentAccepted {} {:from-block 0 :to-block "latest"}]
                          :standard-bounties/bounty-changed [:standard-bounties :BountyChanged {} {:from-block 0 :to-block "latest"}]
                          :standard-bounties/bounty-issuers-updated [:standard-bounties :BountyIssuersUpdated {} {:from-block 0 :to-block "latest"}]
                          :standard-bounties/bounty-data-changed [:standard-bounties :BountyDataChanged {} {:from-block 0 :to-block "latest"}]
                          :standard-bounties/bounty-deadline-changed [:standard-bounties :BountyDeadlineChanged {} {:from-block 0 :to-block "latest"}]

                          :ethlance-jobs/job-issued [:ethlance-jobs :JobIssued {} {:from-block 0 :to-block "latest"}]
                          :ethlance-jobs/contribution-added [:ethlance-jobs :ContributionAdded {} {:from-block 0 :to-block "latest"}]
                          :ethlance-jobs/contribution-refunded [:ethlance-jobs :ContributionRefunded {} {:from-block 0 :to-block "latest"}]
                          :ethlance-jobs/contributions-refunded [:ethlance-jobs :ContributionsRefunded {} {:from-block 0 :to-block "latest"}]
                          :ethlance-jobs/job-drained [:ethlance-jobs :JobDrained {} {:from-block 0 :to-block "latest"}]
                          :ethlance-jobs/job-invoice [:ethlance-jobs :JobInvoice {} {:from-block 0 :to-block "latest"}]
                          :ethlance-jobs/invoice-accepted [:ethlance-jobs :InvoiceAccepted {} {:from-block 0 :to-block "latest"}]
                          :ethlance-jobs/job-changed [:ethlance-jobs :JobChanged {} {:from-block 0 :to-block "latest"}]
                          :ethlance-jobs/job-issuers-updated [:ethlance-jobs :JobIssuersUpdated {} {:from-block 0 :to-block "latest"}]
                          :ethlance-jobs/job-approvers-updated [:ethlance-jobs :JobApproversUpdated {} {:from-block 0 :to-block "latest"}]
                          :ethlance-jobs/job-data-changed [:ethlance-jobs :JobDataChanged {} {:from-block 0 :to-block "latest"}]
                          :ethlance-jobs/candidate-accepted [:ethlance-jobs :CandidateAccepted {} {:from-block 0 :to-block "latest"}]
                          :ethlance-jobs/candidate-applied [:ethlance-jobs :CandidateApplied {} {:from-block 0 :to-block "latest"}]
                          }
                 :crash-on-event-fail? true
                 :write-events-into-file? true
                 :file-path "ethlance-events.log"}

   :smart-contracts {:contracts-var contracts-var
                     :print-gas-usage? false
                     :auto-mining? false}

   :graphql graphql-config

   :ethlance/db {:resync? false}

   :ipfs {:host "http://127.0.0.1:5001"
          :endpoint "/api/v0"
          :gateway "http://127.0.0.1:8080/ipfs"}
   :logging {:level "debug"
             :console? true}})

(defn -main [& args]
  (log/info "Initializing Server...")
  (async-helpers/extend-promises-as-channels!)
  (println "starting with " default-config)
  (-> (mount/with-args {:config {:default default-config}})
      (mount/start)
      (as-> $ (log/warn "Started" {:components $
                                   :config @config}))))


(set! *main-cli-fn* -main)
