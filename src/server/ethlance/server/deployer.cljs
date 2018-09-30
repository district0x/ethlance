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
  "Deploy all smart contracts.
   
   Keyword Arguments:
  
   opts -- map of contract options for
   #'contracts/deploy-smart-contract!
  
   Optional Arguments:
  
   write? -- If true, will also write the deployed contract addresses
  into ethlance.shared.smart-contracts. [default: false]"
  [opts & {:keys [write?] :or {write? false}}]
  (deploy-district-config! opts)

  (when write?
    (contracts/write-smart-contracts!)))
