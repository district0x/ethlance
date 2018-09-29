(ns ethlance.server.deployer
  (:require
   [district.server.smart-contracts :as contracts]))


(defn deploy-district-config!
  "Deploy DistrictConfig contract."
  [opts]
  (contracts/deploy-smart-contract!
   :district-config
   (merge
    opts
    {:gas 1000000 :arguments ["test"]})))


(defn deploy-all!
  [opts & {:keys [write?] :or {write? false}}]
  (deploy-district-config! opts)

  (when write?
    (contracts/write-smart-contracts!)))
