(ns ethlance.server.deployer
  "Includes functions for deploying smart contracts into a
  testnet (ganache-cli), or the ethereum mainnet."
  (:require
   [district.server.smart-contracts :as contracts]))

(def forwarder-target-placeholder "beefbeefbeefbeefbeefbeefbeefbeefbeefbeef")
(def district-config-placeholder "abcdabcdabcdabcdabcdabcdabcdabcdabcdabcd")
(def event-dispatcher-placeholder "dabbdabbdabbdabbdabbdabbdabbdabbdabbdabb")
(def job-placeholder "feedfeedfeedfeedfeedfeedfeedfeedfeedfeed")
(def user-placeholder "deaddeaddeaddeaddeaddeaddeaddeaddeaddead")
(def candidate-placeholder "deafdeafdeafdeafdeafdeafdeafdeafdeafdeaf")
(def employee-placeholder "feeffeeffeeffeeffeeffeeffeeffeeffeeffeef")
(def arbiter-placeholder "feaffeaffeaffeaffeaffeaffeaffeaffeaffeaf")


(defn deploy-district-config!
  "Deploy DistrictConfig contract."
  [opts]
  (contracts/deploy-smart-contract!
   :district-config
   (merge
    {:gas 1000000 :arguments ["test"]}
    opts)))


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

  (when write?
    (contracts/write-smart-contracts!)))
