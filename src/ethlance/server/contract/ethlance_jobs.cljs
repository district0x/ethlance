(ns ethlance.server.contract.ethlance-jobs
  (:require [district.server.smart-contracts :as contracts]
            [ethlance.server.contract.ethlance-issuer :as ethlance-issuer]))

(defn accept-candidate [address args opts]
  (contracts/contract-send [ethlance-issuer/*ethlance-jobs-key* address] :accept-candidate args (merge {:gas 5e6} opts)))

(defn invoice-job [address args opts]
  (contracts/contract-send [ethlance-issuer/*ethlance-jobs-key* address] :invoice-job args (merge {:gas 5e6} opts)))
