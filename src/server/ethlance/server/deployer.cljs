(ns ethlance.server.deployer
  "Includes functions for deploying smart contracts into a
  testnet (ganache-cli), or the ethereum mainnet."
  (:require
   [taoensso.timbre :as log]
   [mount.core :as mount :refer [defstate]]

   [clojure.core.async :as async :refer [go go-loop <! >! chan close!] :include-macros true]
   [district.server.smart-contracts :as contracts]
   [ethlance.server.contract.ds-guard :as ds-guard]
   [ethlance.server.contract.ds-auth :as ds-auth]
   [ethlance.server.contract.ethlance-registry :as registry]
   [ethlance.server.contract.ethlance-user-factory :as user-factory]
   [ethlance.server.contract.ethlance-job-factory :as job-factory]
   [ethlance.server.utils.deasync :refer [go-deasync] :include-macros true]
   [ethlance.shared.async-utils :refer [<!-<throw go-try] :include-macros true]
   [ethlance.server.contract :refer [deploy!]]))


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
  "beefabeefabeefabeefabeefabeefabeefabeefa")

(def district-config-placeholder
  "DistrictConfig Contract target replacement"
  "abcdabcdabcdabcdabcdabcdabcdabcdabcdabcd")

(def registry-placeholder
  "EthlanceRegistry Contract target replacement"
  "dabbdabbdabbdabbdabbdabbdabbdabbdabbdabb")


(defn deploy-ds-guard!
  "Deploy DSGuard contract."
  [opts]
  (go-try
   (log/debug "Deploying DSGuard Contract...")
   (<!-<throw
    (deploy!
     :contract-key ds-guard/*guard-key*
     :opts (merge {:gas 1000000} opts)))
   
   ;; Assign to its own authority
   (log/debug "Setting up DSGuard Authority...")
   (<!-<throw
    (ds-auth/set-authority! ds-guard/*guard-key* (ds-guard/address)))))


(defn deploy-ethlance-registry!
  "Deploy EthlanceRegistry."
  [opts]
  (go-try
   (log/debug "Deploying EthlanceRegistry...")
   (<!-<throw
    (deploy!
     :contract-key :ethlance-registry
     :contract-options {:gas 2000000}))

   ;; Assign the DSGuard authority
   (log/debug "Setting up EthlanceRegistry Authority...")
   (<!-<throw
    (ds-auth/set-authority! :ethlance-registry (ds-guard/address)))))


(defn deploy-ethlance-user!
  "Deploy EthlanceUser."
  [opts]

  ;; Deploy ethlance user contract
  (go-try
   (log/debug "Deploying EthlanceUser...")
   (<!-<throw
    (deploy!
     :contract-key :ethlance-user
     :contract-options 
     {:gas 2000000
      :placeholder-replacements
      {registry-placeholder :ethlance-registry}}))))


(defn deploy-ethlance-user-factory!
  "Deploy EthlanceUserFactory."
  [opts]
  (go-try
   ;; Deploy main factory contract
   (log/debug "Deploying EthlanceUserFactory...")
   (<!-<throw
    (deploy!
     :contract-key :ethlance-user-factory
     :contract-options 
     {:gas 2000000
      :placeholder-replacements
      {forwarder-target-placeholder :ethlance-user
       registry-placeholder :ethlance-registry}}))

   ;; Attach to forwarder
   (log/debug "Attaching EthlanceUserFactory Forwarder...")
   (<!-<throw
    (deploy!
     :contract-key :ethlance-user-factory-fwd
     :contract-options
     {:gas 1000000
      :placeholder-replacements
      {forwarder-target-placeholder :ethlance-user-factory}}))

   ;; Assign the DSGuard authority
   (log/debug "Setting up EthlanceUserFactory Authority...")
   (<!-<throw
    (ds-auth/set-authority! :ethlance-user-factory-fwd (ds-guard/address) opts))

   ;; Configure DSGuard Authority
   (log/debug "Configure DSGuard for EthlanceUserFactory Forwarder")
   (let [user-factory-fwd-address (contracts/contract-address :ethlance-user-factory-fwd)
         registry-address (contracts/contract-address :ethlance-registry)]

     (log/debug "EthlanceUserFactory Forwarder (permit ANY)")
     (<!-<throw
      (ds-guard/permit-any! user-factory-fwd-address))

     (log/debug "EthlanceUserFactory Forwarder (permit -->EthlanceRegistry)")
     (<!-<throw
      (ds-guard/permit! {:src user-factory-fwd-address
                         :dst registry-address
                         :sig ds-guard/ANY})))

   ;; Configure Factory Privilege
   (log/debug "Permitting EthlanceUserFactory Factory Privilege")
   (<!-<throw
    (registry/permit-factory-privilege!
     (contracts/contract-address :ethlance-user-factory-fwd)))))


(defn deploy-ethlance-comment!
  "Deploy EthlanceComment."
  [opts]
  (go-try
   (log/debug "Deploying EthlanceComment...")
   (<!-<throw
    (deploy!
     :contract-key :ethlance-comment
     :contract-options 
     {:gas 2500000
      :placeholder-replacements
      {registry-placeholder :ethlance-registry}}))))


(defn deploy-ethlance-feedback!
  "Deploy EthlanceFeedback."
  [opts]
  (go-try
   (log/debug "Deploying EthlanceFeedback...")
   (<!-<throw
    (deploy!
     :contract-key :ethlance-feedback
     :contract-options 
     {:gas 2500000
      :placeholder-replacements
      {registry-placeholder :ethlance-registry}}))))


(defn deploy-ethlance-invoice!
  "Deploy EthlanceInvoice."
  [opts]

  (go-try
   (log/debug "Deploying EthlanceInvoice...")
   (<!-<throw
    (deploy!
     :contract-key :ethlance-invoice
     :contract-options 
     {:gas 2500000
      :placeholder-replacements
      {registry-placeholder :ethlance-registry
       forwarder-target-placeholder :ethlance-comment}}))))


(defn deploy-ethlance-dispute!
  "Deploy EthlanceDispute."
  [opts]
  (go-try
   (log/debug "Deploying EthlanceDispute...")
   (<!-<throw
    (deploy!
     :contract-key :ethlance-dispute
     :contract-options
     {:gas 2500000
      :placeholder-replacements
      {registry-placeholder :ethlance-registry
       forwarder-target-placeholder :ethlance-comment
       second-forwarder-target-placeholder :ethlance-feedback}}))))


(defn deploy-ethlance-token-store!
  "Deploy EthlanceTokenStore."
  [opts]
  (go-try
   (log/debug "Deploying EthlanceTokenStore...")
   (<!-<throw
    (deploy!
     :contract-key :ethlance-token-store
     :contract-options
     {:gas 2500000
      :placeholder-replacements
      {registry-placeholder :ethlance-registry}}))))


(defn deploy-ethlance-work-contract!
  "Deploy EthlanceWorkContract."
  [opts]
  (go-try
   ;; Deploy ethlance work contract
   (log/debug "Deploying EthlanceWorkContract...")
   (<!-<throw
    (deploy!
     :contract-key :ethlance-work-contract
     :contract-options
     {:gas 4000000
      :placeholder-replacements
      {forwarder-target-placeholder :ethlance-invoice
       second-forwarder-target-placeholder :ethlance-dispute
       third-forwarder-target-placeholder :ethlance-comment
       fourth-forwarder-target-placeholder :ethlance-feedback
       registry-placeholder :ethlance-registry}}))))


(defn deploy-ethlance-job-store!
  "Deploy EthlanceJob."
  [opts]
  (go-try
   ;; Deploy ethlance job contract
   (log/debug "Deploying EthlanceJobStore...")
   (<!-<throw
    (deploy!
     :contract-key :ethlance-job-store
     :contract-options
     {:gas 4000000
      :placeholder-replacements
      {forwarder-target-placeholder :ethlance-work-contract
       second-forwarder-target-placeholder :ethlance-token-store
       registry-placeholder :ethlance-registry}}))))


(defn deploy-ethlance-job-factory!
  "Deploy EthlanceJobFactory."
  [opts]
  (go-try
   ;; Deploy main factory contract
   (log/debug "Deploying EthlanceJobFactory...")
   (<!-<throw
    (deploy!
     :contract-key :ethlance-job-factory
     :contract-options
     {:gas 2500000
      :placeholder-replacements
      {forwarder-target-placeholder :ethlance-job-store
       registry-placeholder :ethlance-registry}}))

   ;; Attach to forwarder
   (log/debug "Attaching EthlanceJobFactory Forwarder...")
   (<!-<throw
    (deploy!
     :contract-key :ethlance-job-factory-fwd
     :contract-options
     {:gas 1000000
      :placeholder-replacements
      {forwarder-target-placeholder :ethlance-job-factory}}))

   ;; Assign the DSGuard authority
   (log/debug "Setting up EthlanceJobFactory Authority...")
   (<!-<throw
    (ds-auth/set-authority! :ethlance-job-factory-fwd (ds-guard/address)))

   ;; Configure DSGuard Authority
   (log/debug "Configure DSGuard for EthlanceJobFactory Forwarder")
   (let [job-factory-fwd-address (contracts/contract-address :ethlance-job-factory-fwd)
         registry-address (contracts/contract-address :ethlance-registry)]

     (log/debug "EthlanceJobFactory Forwarder (permit ANY)")
     (<!-<throw
      (ds-guard/permit-any! job-factory-fwd-address))

     (log/debug "EthlanceJobFactory Forwarder (permit -->EthlanceRegistry)")
     (<!-<throw
      (ds-guard/permit! {:src job-factory-fwd-address
                         :dst registry-address
                         :sig ds-guard/ANY} opts)))

   ;; Configure Factory Privilege
   (log/debug "Permitting EthlanceJobFactory Factory Privilege")
   (<!-<throw
    (registry/permit-factory-privilege!
     (contracts/contract-address :ethlance-job-factory-fwd)))))


(defn deploy-token!
  [opts]
  (go-try
   (log/debug "Deploying Token Contract...")
   (<!-<throw
    (deploy!
     :contract-key :token
     :contract-options
     {:gas 2000000}))))


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
  (go-try
   ;; Debug: Logging Parameters
   (log/debug (str "General Contract Options: " general-contract-options))
   (log/debug (str "Write Contracts on Finish?: " (boolean write?)))

   (<! (deploy-ds-guard! general-contract-options))
   (<! (deploy-ethlance-registry! general-contract-options))
   (<! (deploy-ethlance-comment! general-contract-options))
   (<! (deploy-ethlance-feedback! general-contract-options))
   (<! (deploy-ethlance-user! general-contract-options))
   (<! (deploy-ethlance-user-factory! general-contract-options))
   (<! (deploy-ethlance-invoice! general-contract-options))
   (<! (deploy-ethlance-dispute! general-contract-options))
   (<! (deploy-ethlance-work-contract! general-contract-options))
   (<! (deploy-ethlance-token-store! general-contract-options))
   (<! (deploy-ethlance-job-store! general-contract-options))
   (<! (deploy-ethlance-job-factory! general-contract-options))
   (<! (deploy-token! general-contract-options))

   (when write?
     (log/debug "Writing out Smart Contracts...")
     (contracts/write-smart-contracts!))))


(defn start []
  (go-deasync
   (log/debug "Deployment Starting!")
   (<! (deploy-all! {})))
  (log/debug "Deployment Finished!")
  :started)


(defn stop [])
