(ns ethlance.server.contract.ethlance-bounty-issuer
  (:require [district.server.smart-contracts :as contracts]
            [ethlance.server.contract :as ethlance-contracts]))

(def ^:dynamic *enthlance-bounty-issuer-key*
  "The main contract key"
  :ethlance-bounty-issuer)

(def ^:dynamic *standard-bounties-key*
  "The standard bounties contract key"
  :standard-bounties)

(def token-version {:eth 0
                    :erc20 20
                    :erc721 721})

(defn test-ethlance-bounty-issuer-address []
  (contracts/contract-address *enthlance-bounty-issuer-key*))

(defn test-standard-bounties-address []
  (contracts/contract-address *standard-bounties-key*))

(defn issue-and-contribute [address [bounty-data-hash deadline token-address token-version deposit :as args] opts]
  (contracts/contract-send [*enthlance-bounty-issuer-key* address] :issue-and-contribute args (merge {:gas 5e6} opts)))

(defn invite-arbiters [address [arbiters-addresses fee bounty-id :as args] opts]
  (contracts/contract-send [*enthlance-bounty-issuer-key* address] :invite-arbiters args opts))

(defn accept-arbiter-invitation [address [bounty-id :as args] opts]
  (contracts/contract-send [*enthlance-bounty-issuer-key* address] :accept-arbiter-invitation args opts))

(defn standard-bounties-event-in-tx [event-key tx-receipt]

  (contracts/contract-event-in-tx [*standard-bounties-key* (test-standard-bounties-address)]
                                  event-key
                                  tx-receipt))
