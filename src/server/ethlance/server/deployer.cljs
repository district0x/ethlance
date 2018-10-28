(ns ethlance.server.deployer
  "Includes functions for deploying smart contracts into a
  testnet (ganache-cli), or the ethereum mainnet."
  (:require
   [taoensso.timbre :as log]

   [district.server.smart-contracts :as contracts]

   [ethlance.server.contract.ds-guard :as ds-guard]
   [ethlance.server.contract.ds-auth :as ds-auth]
   [ethlance.server.contract.ethlance-registry :as registry]
   [ethlance.server.contract.ethlance-user-factory :as user-factory]
   [ethlance.server.contract.ethlance-job-factory :as job-factory]))


(def forwarder-target-placeholder "beefbeefbeefbeefbeefbeefbeefbeefbeefbeef")
(def district-config-placeholder "abcdabcdabcdabcdabcdabcdabcdabcdabcdabcd")
(def registry-placeholder "dabbdabbdabbdabbdabbdabbdabbdabbdabbdabb")


(defn deploy-district-config!
  "Deploy DistrictConfig contract."
  [opts]
  (log/debug "Deploying DistrictConfig Contract...")
  (contracts/deploy-smart-contract!
   :district-config
   (merge
    {:gas 1000000 :arguments ["test"]}
    opts)))


(defn deploy-ds-guard!
  "Deploy DSGuard contract."
  [opts]
  (log/debug "Deploying DSGuard Contract...")
  (contracts/deploy-smart-contract!
   ds-guard/*guard-key*
   (merge {:gas 1000000} opts))
  
  ;; Assign to its own authority
  (log/debug "Setting up DSGuard Authority...")
  (ds-auth/set-authority! ds-guard/*guard-key* (ds-guard/address)))


(defn deploy-ethlance-registry!
  "Deploy EthlanceRegistry."
  [opts]
  (log/debug "Deploying EthlanceRegistry...")
  (contracts/deploy-smart-contract!
   :ethlance-registry
   (merge
    {:gas 1000000}
    opts))

  ;; Assign the DSGuard authority
  (log/debug "Setting up EthlanceRegistry Authority...")
  (ds-auth/set-authority! :ethlance-registry (ds-guard/address) opts))


(defn deploy-ethlance-user!
  "Deploy EthlanceUser."
  [opts]

  ;; Deploy ethlance user contract
  (log/debug "Deploying EthlanceUser...")
  (contracts/deploy-smart-contract!
   :ethlance-user
   (merge
    {:gas 2000000
     :placeholder-replacements
     {registry-placeholder :ethlance-registry}}
    opts)))


(defn deploy-ethlance-user-factory!
  "Deploy EthlanceUserFactory."
  [opts]

  ;; Deploy main factory contract
  (log/debug "Deploying EthlanceUserFactory...")
  (contracts/deploy-smart-contract!
   :ethlance-user-factory
   (merge
    {:gas 2000000
     :placeholder-replacements
     {forwarder-target-placeholder :ethlance-user
      registry-placeholder :ethlance-registry}}
    opts))

  ;; Attach to forwarder
  (log/debug "Attaching EthlanceUserFactory Forwarder...")
  (contracts/deploy-smart-contract!
   :ethlance-user-factory-fwd
   (merge
    {:gas 1000000
     :placeholder-replacements
     {forwarder-target-placeholder :ethlance-user-factory}}
    opts))

  ;; Assign the DSGuard authority
  (log/debug "Setting up EthlanceUserFactory Authority...")
  (ds-auth/set-authority! :ethlance-user-factory-fwd (ds-guard/address) opts)

  ;; Configure DSGuard Authority
  (log/debug "Configure DSGuard for EthlanceUserFactory Forwarder")
  (let [user-factory-fwd-address (contracts/contract-address :ethlance-user-factory-fwd)
        registry-address (contracts/contract-address :ethlance-registry)]

    (log/debug "EthlanceUserFactory Forwarder (permit ANY)")
    (ds-guard/permit-any! user-factory-fwd-address opts)

    (log/debug "EthlanceUserFactory Forwarder (permit -->EthlanceRegistry)")
    (ds-guard/permit! {:src user-factory-fwd-address
                       :dst registry-address
                       :sig ds-guard/ANY} opts))

  ;; Configure Factory Privilege
  (log/debug "Permitting EthlanceUserFactory Factory Privilege")
  (registry/permit-factory-privilege!
   (contracts/contract-address :ethlance-user-factory-fwd)))


(defn deploy-ethlance-work-contract!
  "Deploy EthlanceWorkContract."
  [opts]
  
  ;; Deploy ethlance work contract
  (log/debug "Deploying EthlanceWorkContract...")
  (contracts/deploy-smart-contract!
   :ethlance-work-contract
   (merge
    {:gas 3000000
     :placeholder-replacements
     {registry-placeholder :ethlance-registry}}
    opts)))


(defn deploy-ethlance-job-store!
  "Deploy EthlanceJob."
  [opts]

  ;; Deploy ethlance job contract
  (log/debug "Deploying EthlanceJobStore...")
  (contracts/deploy-smart-contract!
   :ethlance-job-store
   (merge
    {:gas 4000000
     :placeholder-replacements
     {forwarder-target-placeholder :ethlance-work-contract
      registry-placeholder :ethlance-registry}}
    opts)))


(defn deploy-ethlance-job-factory!
  "Deploy EthlanceJobFactory."
  [opts]

  ;; Deploy main factory contract
  (log/debug "Deploying EthlanceJobFactory...")
  (contracts/deploy-smart-contract!
   :ethlance-job-factory
   (merge
    {:gas 2000000
     :placeholder-replacements
     {forwarder-target-placeholder :ethlance-job-store
      registry-placeholder :ethlance-registry}}
    opts))

  ;; Attach to forwarder
  (log/debug "Attaching EthlanceJobFactory Forwarder...")
  (contracts/deploy-smart-contract!
   :ethlance-job-factory-fwd
   (merge
    {:gas 1000000
     :placeholder-replacements
     {forwarder-target-placeholder :ethlance-job-factory}}))

  ;; Assign the DSGuard authority
  (log/debug "Setting up EthlanceJobFactory Authority...")
  (ds-auth/set-authority! :ethlance-job-factory-fwd (ds-guard/address) opts)

  ;; Configure DSGuard Authority
  (log/debug "Configure DSGuard for EthlanceJobFactory Forwarder")
  (let [job-factory-fwd-address (contracts/contract-address :ethlance-job-factory-fwd)
        registry-address (contracts/contract-address :ethlance-registry)]

    (log/debug "EthlanceJobFactory Forwarder (permit ANY)")
    (ds-guard/permit-any! job-factory-fwd-address)

    (log/debug "EthlanceJobFactory Forwarder (permit -->EthlanceRegistry)")
    (ds-guard/permit! {:src job-factory-fwd-address
                       :dst registry-address
                       :sig ds-guard/ANY} opts))

  ;; Configure Factory Privilege
  (log/debug "Permitting EthlanceJobFactory Factory Privilege")
  (registry/permit-factory-privilege!
   (contracts/contract-address :ethlance-job-factory-fwd)))


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

  ;; Debug: Logging Parameters
  (log/debug "General Contract Options" general-contract-options)
  (log/debug "Write Contracts on Finish?: " write?)

  (deploy-district-config! general-contract-options)
  (deploy-ds-guard! general-contract-options)
  (deploy-ethlance-registry! general-contract-options)
  (deploy-ethlance-user! general-contract-options)
  (deploy-ethlance-user-factory! general-contract-options)
  (deploy-ethlance-work-contract! general-contract-options)
  (deploy-ethlance-job-store! general-contract-options)
  (deploy-ethlance-job-factory! general-contract-options)

  (when write?
    (log/debug "Writing out Smart Contracts...")
    (contracts/write-smart-contracts!)))
