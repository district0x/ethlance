(ns ethlance.server.deployer
  "Includes functions for deploying smart contracts into a
  testnet (ganache-cli), or the ethereum mainnet."
  (:require
   [district.server.smart-contracts :as contracts]))


(def forwarder-target-placeholder "beefbeefbeefbeefbeefbeefbeefbeefbeefbeef")
(def district-config-placeholder "abcdabcdabcdabcdabcdabcdabcdabcdabcdabcd")
(def registry-placeholder "dabbdabbdabbdabbdabbdabbdabbdabbdabbdabb")
;;(def job-factory-placeholder "feedfeedfeedfeedfeedfeedfeedfeedfeedfeed")
;;(def user-factory-placeholder "deaddeaddeaddeaddeaddeaddeaddeaddeaddead")


(defn deploy-district-config!
  "Deploy DistrictConfig contract."
  [opts]
  (contracts/deploy-smart-contract!
   :district-config
   (merge
    {:gas 1000000 :arguments ["test"]}
    opts)))


(defn deploy-ethlance-registry!
  "Deploy EthlanceRegistry."
  [opts]
  (contracts/deploy-smart-contract!
   :ethlance-registry
   (merge
    {:gas 1000000}
    opts))

  ;; Attach to forwarder
  (contracts/deploy-smart-contract!
   :ethlance-registry-fwd
   (merge
    {:gas 1000000
     :placeholder-replacements
     {forwarder-target-placeholder :ethlance-registry}}
    opts)))


(defn deploy-ethlance-user-factory!
  "Deploy EthlanceUserFactory."
  [opts]

  ;; Deploy main factory contract
  (contracts/deploy-smart-contract!
   :ethlance-user-factory
   (merge
    {:gas 2000000
     :placeholder-replacements
     {registry-placeholder :ethlance-registry-fwd}}
    opts))

  ;; Attach to forwarder
  (contracts/deploy-smart-contract!
   :ethlance-user-factory-fwd
   (merge
    {:gas 1000000
     :placeholder-replacements
     {forwarder-target-placeholder :ethlance-user-factory}})))


(defn deploy-ethlance-job-factory!
  "Deploy EthlanceJobFactory."
  [opts]
  
  ;; Deploy main factory contract
  (contracts/deploy-smart-contract!
   :ethlance-job-factory
   (merge
    {:gas 2000000
     :placeholder-replacements
     {registry-placeholder :ethlance-registry}}
    opts))
  
  ;; Attach to forwarder
  (contracts/deploy-smart-contract!
   :ethlance-job-factory-fwd
   (merge
    {:gas 1000000
     :placeholder-replacements
     {forwarder-target-placeholder :ethlance-job-factory}})))


(defn deploy-all!
  "Deploy all smart contracts.
  
   Optional Arguments:
  
   general-contract-options -- map of contract options for all
  #'contracts/deploy-smart-contract!

   write? -- If true, will also write the deployed contract addresses
  into ethlance.shared.smart-contracts. [default: false]"
  [{:keys [general-contract-options write?]
    :or {general-contract-options {}
         write? false}}]
  (deploy-district-config! general-contract-options)
  (deploy-ethlance-registry! general-contract-options)
  (deploy-ethlance-user-factory! general-contract-options)
  (deploy-ethlance-job-factory! general-contract-options)

  (when write?
    (contracts/write-smart-contracts!)))
