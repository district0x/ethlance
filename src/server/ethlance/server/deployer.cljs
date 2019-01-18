(ns ethlance.server.deployer
  "Includes functions for deploying smart contracts into a
  testnet (ganache-cli), or the ethereum mainnet."
  (:require
   [taoensso.timbre :as log]
   [mount.core :as mount :refer [defstate]]

   [district.server.smart-contracts :as contracts]

   [ethlance.server.contract.ds-guard :as ds-guard]
   [ethlance.server.contract.ds-auth :as ds-auth]
   [ethlance.server.contract.ethlance-registry :as registry]
   [ethlance.server.contract.ethlance-user-factory :as user-factory]
   [ethlance.server.contract.ethlance-job-factory :as job-factory]))


(declare start stop)
(defstate deployer
  :start (start)
  :stop (stop))


(def forwarder-target-placeholder
  "Forwarder Contract target replacement"
  "beefbeefbeefbeefbeefbeefbeefbeefbeefbeef")

(def second-forwarder-target-placeholder
  "SecondForwarder Contract target replacement"
  "dabadabadabadabadabadabadabadabadabadaba")

(def third-forwarder-target-placeholder
  "ThirdForwarder Contract target replacment"
  "dbdadbadbabdabdbadabdbafffd1234fdfadbccc")

(def fourth-forwarder-target-placeholder
  "FourthForwarder Contract target replacment"
  "dbdad432babbabdbadabdbafffd4321fdfadbccc")

(def district-config-placeholder
  "DistrictConfig Contract target replacement"
  "abcdabcdabcdabcdabcdabcdabcdabcdabcdabcd")

(def registry-placeholder
  "EthlanceRegistry Contract target replacement"
  "dabbdabbdabbdabbdabbdabbdabbdabbdabbdabb")


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
    {:gas 2000000}
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


(defn deploy-ethlance-comment!
  "Deploy EthlanceComment."
  [opts]
  
  (log/debug "Deploying EthlanceComment...")
  (contracts/deploy-smart-contract!
   :ethlance-comment
   (merge
    {:gas 2500000
     :placeholder-replacements
     {registry-placeholder :ethlance-registry}})))


(defn deploy-ethlance-feedback!
  "Deploy EthlanceFeedback."
  [opts]
  
  (log/debug "Deploying EthlanceFeedback...")
  (contracts/deploy-smart-contract!
   :ethlance-feedback
   (merge
    {:gas 2500000
     :placeholder-replacements
     {registry-placeholder :ethlance-registry}})))


(defn deploy-ethlance-invoice!
  "Deploy EthlanceInvoice."
  [opts]

  (log/debug "Deploying EthlanceInvoice...")
  (contracts/deploy-smart-contract!
   :ethlance-invoice
   (merge
    {:gas 2500000
     :placeholder-replacements
     {registry-placeholder :ethlance-registry
      forwarder-target-placeholder :ethlance-comment}}
    opts)))


(defn deploy-ethlance-dispute!
  "Deploy EthlanceDispute."
  [opts]

  (log/debug "Deploying EthlanceDispute...")
  (contracts/deploy-smart-contract!
   :ethlance-dispute
   (merge
    {:gas 2500000
     :placeholder-replacements
     {registry-placeholder :ethlance-registry
      forwarder-target-placeholder :ethlance-comment}}
    opts)))


(defn deploy-ethlance-work-contract!
  "Deploy EthlanceWorkContract."
  [opts]
  
  ;; Deploy ethlance work contract
  (log/debug "Deploying EthlanceWorkContract...")
  (contracts/deploy-smart-contract!
   :ethlance-work-contract
   (merge
    {:gas 4000000
     :placeholder-replacements
     {forwarder-target-placeholder :ethlance-invoice
      second-forwarder-target-placeholder :ethlance-dispute
      third-forwarder-target-placeholder :ethlance-comment
      fourth-forwarder-target-placeholder :ethlance-feedback
      registry-placeholder :ethlance-registry}}
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
    {:gas 2500000
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


(defn deploy-test-token!
  [opts]
  (log/debug "Deploying TestToken Contract...")
  (contracts/deploy-smart-contract!
   :test-token
   (merge {:gas 2000000} opts)))


(defn deploy-test-multi-linked-list!
  [opts]
  (log/debug "Deploying TestMultiLinkedList Contract...")
  (contracts/deploy-smart-contract!
   :test-multi-linked-list
   (merge {:gas 2000000} opts)))


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
  (log/debug (str "General Contract Options: " general-contract-options))
  (log/debug (str "Write Contracts on Finish?: " (boolean write?)))

  (deploy-ds-guard! general-contract-options)
  (deploy-ethlance-registry! general-contract-options)
  (deploy-ethlance-comment! general-contract-options)
  (deploy-ethlance-feedback! general-contract-options)
  (deploy-ethlance-user! general-contract-options)
  (deploy-ethlance-user-factory! general-contract-options)
  (deploy-ethlance-invoice! general-contract-options)
  (deploy-ethlance-dispute! general-contract-options)
  (deploy-ethlance-work-contract! general-contract-options)
  (deploy-ethlance-job-store! general-contract-options)
  (deploy-ethlance-job-factory! general-contract-options)

  ;; TODO: include option to not deploy test contracts.
  (deploy-test-token! general-contract-options)
  (deploy-test-multi-linked-list! general-contract-options)

  (when write?
    (log/debug "Writing out Smart Contracts...")
    (contracts/write-smart-contracts!)))


(defn start []
  (deploy-all! {}))


(defn stop [])
