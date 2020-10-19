(ns ethlance.server.contract.ethlance-issuer
  (:require [district.server.smart-contracts :as contracts]))

(def ^:dynamic *enthlance-issuer-key*
  "The main contract key"
  :ethlance-issuer)

(def ^:dynamic *standard-bounties-key*
  "The standard bounties contract key"
  :standard-bounties)

(def ^:dynamic *ethlance-jobs-key*
  "The EthlanceJobs contract key"
  :ethlance-jobs)

(def token-version {:eth 0
                    :erc20 20
                    :erc721 721})

(def job-type {:ethlance-job 0
               :standard-bounty 1})

(defn test-ethlance-issuer-address []
  (contracts/contract-address *enthlance-issuer-key*))

(defn test-standard-bounties-address []
  (contracts/contract-address *standard-bounties-key*))

(defn test-ethlance-jobs-address []
  (contracts/contract-address *ethlance-jobs-key*))

(defn issue-bounty [address args opts]
  (contracts/contract-send [*enthlance-issuer-key* address] :issue-bounty args (merge {:gas 5e6} opts)))

(defn issue-job [address args opts]
  (contracts/contract-send [*enthlance-issuer-key* address] :issue-job args (merge {:gas 5e6} opts)))

(defn invite-arbiters [address args opts]
  (contracts/contract-send [*enthlance-issuer-key* address] :invite-arbiters args opts))

(defn accept-arbiter-invitation [address args opts]
  (contracts/contract-send [*enthlance-issuer-key* address] :accept-arbiter-invitation args opts))

(defn standard-bounties-event-in-tx [event-key tx-receipt]
  (contracts/contract-event-in-tx [*standard-bounties-key* (test-standard-bounties-address)]
                                  event-key
                                  tx-receipt))

(defn ethlance-jobs-event-in-tx [event-key tx-receipt]
  (contracts/contract-event-in-tx [*ethlance-jobs-key* (test-standard-bounties-address)]
                                  event-key
                                  tx-receipt))
