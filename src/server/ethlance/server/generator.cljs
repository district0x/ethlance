(ns ethlance.server.generator
  "For development purposes, includes functions to generate employers,
  candidates, and arbiters, and simulate use-cases between the three
  parties in different states of jobs containing work contracts."
  (:require
   [bignumber.core :as bn]
   [cljs-ipfs-api.files :as ipfs-files]
   [cljs-web3.core :as web3]
   [cljs-web3.eth :as web3-eth]
   [cljs-web3.evm :as web3-evm]
   [cljs-web3.utils :refer [js->cljkk camel-case]]
   [clojure.core.async :as async :refer [go go-loop <! >! chan] :include-macros true]
   [district.cljs-utils :refer [rand-str]]
   [district.format :as format]
   [district.server.config :refer [config]]
   [district.server.smart-contracts :as smart-contracts]
   [district.server.web3 :refer [web3]]
   [district.shared.error-handling :refer [try-catch]]
   [mount.core :as mount :refer [defstate]]
   [taoensso.timbre :as log]

   ;; Ethlance NS
   [ethlance.server.ipfs :as ipfs]
   [ethlance.server.filesystem :as filesystem]
   [ethlance.shared.random :as random]
   [ethlance.server.contract.ethlance-user :as user :include-macros true]
   [ethlance.server.contract.ethlance-user-factory :as user-factory]
   [ethlance.server.contract.ethlance-job-store :as job :include-macros true]
   [ethlance.server.contract.ethlance-job-factory :as job-factory]
   [ethlance.server.contract.ethlance-work-contract :as work-contract]
   [ethlance.server.contract.ethlance-invoice :as invoice :include-macros true]
   [ethlance.server.contract.ethlance-dispute :as dispute :include-macros true]
   [ethlance.server.deployer :as deployer]
   [ethlance.server.generator.choice-collections :as choice-collections]))


(declare start stop)
(defstate generator
  :start (start)
  :stop (stop generator))


(defn testnet-max-accounts [] 10) ;; TODO: check config value


(defn generate-registered-users!
  "Generate registered users along with registering for candidate,
  employer, and arbiter."
  [{:keys [num-employers num-candidates num-arbiters]
    :or {num-employers 3 num-candidates 4 num-arbiters 3}}]
  (let [total-accounts (+ num-employers num-candidates num-arbiters)
        max-accounts (testnet-max-accounts)]
    (assert (<= total-accounts max-accounts)
            "The number of total registrations exceeds the max number of testnet accounts.")

    ;; Register several user accounts
    (doseq [index (range total-accounts)])))
       


(defn start
  [& config])


(defn stop
  [generator])


