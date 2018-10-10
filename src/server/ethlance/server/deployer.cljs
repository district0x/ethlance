(ns ethlance.server.deployer
  "Includes functions for deploying smart contracts into a
  testnet (ganache-cli), or the ethereum mainnet."
  (:require
   [taoensso.timbre :as log]

   [district.server.smart-contracts :as contracts]

   [ethlance.server.contract.ds-guard :as ds-guard]
   [ethlance.server.contract.ds-auth :as ds-auth]))


(def guard-contract-key ds-guard/*guard-key*)

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
   guard-contract-key
   (merge {:gas 1000000} opts))
  
  ;; Assign to its own authority
  (log/debug "Setting up DSGuard Authority...")
  (ds-auth/set-authority! guard-contract-key (contracts/contract-address guard-contract-key)))


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
  (ds-auth/set-authority! :ethlance-registry (contracts/contract-address guard-contract-key) opts))


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
     {registry-placeholder :ethlance-registry}}
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
  (ds-auth/set-authority! :ethlance-user-factory-fwd (contracts/contract-address guard-contract-key) opts)

  ;; Configure DSGuard Authority
  (log/debug "Configure DSGuard for EthlanceUserFactory Forwarder")
  (let [user-factory-fwd-address (contracts/contract-address :ethlance-user-factory-fwd)
        registry-address (contracts/contract-address :ethlance-registry)]

    (log/debug "EthlanceUserFactory Forwarder (permit ANY)")
    (ds-guard/permit-any! user-factory-fwd-address opts)

    (log/debug "EthlanceUserFactory Forwarder --> EthlanceRegistry")
    (ds-guard/permit! {:src user-factory-fwd-address
                       :dst registry-address
                       :sig ds-guard/ANY} opts)))


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
     {registry-placeholder :ethlance-registry}}
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
  (ds-auth/set-authority! :ethlance-job-factory-fwd (contracts/contract-address guard-contract-key) opts)

  ;; Configure DSGuard Authority
  (log/debug "Configure DSGuard for EthlanceJobFactory Forwarder")
  (let [job-factory-fwd-address (contracts/contract-address :ethlance-job-factory-fwd)
        registry-address (contracts/contract-address :ethlance-registry)]

    (log/debug "EthlanceJobFactory Forwarder (permit ANY)")
    (ds-guard/permit-any! job-factory-fwd-address)

    (log/debug "EthlanceJobFactory Forwarder --> EthlanceRegistry")
    (ds-guard/permit! {:src job-factory-fwd-address
                       :dst registry-address
                       :sig ds-guard/ANY} opts)))


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
  (deploy-ethlance-user-factory! general-contract-options)
  (deploy-ethlance-job-factory! general-contract-options)

  (when write?
    (log/debug "Writing out Smart Contracts...")
    (contracts/write-smart-contracts!)))
