(ns ethlance.server.generator
  "For development purposes, includes functions to generate employers,
  candidates, and arbiters, and simulate use-cases between the three
  parties in different states of a work contract."
  (:require
   [bignumber.core :as bn]
   [cljs-ipfs-api.files :as ipfs-files]
   [cljs-web3.core :as web3]
   [cljs-web3.eth :as web3-eth]
   [cljs-web3.evm :as web3-evm]
   [cljs-web3.utils :refer [js->cljkk camel-case]]
   [district.cljs-utils :refer [rand-str]]
   [district.format :as format]
   [district.server.config :refer [config]]
   [district.server.smart-contracts :as smart-contracts]
   [district.server.web3 :refer [web3]]
   ;;[district.shared.error-handling :refer [try-catch]]))
   [taoensso.timbre :as log]
   [mount.core :as mount :refer [defstate]]

   [ethlance.server.deployer :as deployer]))


(declare start stop)
(defstate generator
  :start (start)
  :stop (stop generator))


(defn start
  [& config])


(defn stop
  [generator])
