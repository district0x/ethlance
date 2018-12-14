(ns ethlance.server.syncer
  "Service that syncs the ethlance in-memory database with the ethereum
  blockchain by reading events emitted by the ethlance smart contracts."
  (:require

   [cljs-ipfs-api.files :as ifiles]
   [cljs-solidity-sha3.core :refer [solidity-sha3]]
   [cljs-web3.core :as web3]
   [cljs-web3.eth :as web3-eth]
   [clojure.core.async :as async :refer [go go-loop <! >! chan put! close!] :include-macros true]
   [cuerdas.core :as str]
   [district.server.config :refer [config]]
   [district.server.db :as district.db]
   [district.server.smart-contracts :as smart-contracts :refer [replay-past-events]]
   [district.server.web3 :refer [web3]]
   [district.shared.error-handling :refer [try-catch]]
   [district.web3-utils :as web3-utils]
   [mount.core :as mount :refer [defstate]]
   [print.foo :refer [look] :include-macros true]
   [taoensso.timbre :as log]

   [ethlance.server.syncer.event-watcher :as event-watcher]
   [ethlance.server.syncer.event-multiplexer :as event-multiplexer :refer [syncer-muxer]]
   [ethlance.server.syncer.processor :as processor]
   [ethlance.shared.async-utils :refer [<!-<log <!-<throw flush!] :include-macros true]))


(declare start stop)
(defstate ^{:on-reload :noop} syncer
  :start (start)
  :stop (stop))


(defn start []
  (let [[result-channel _] @syncer-muxer]
    (go-loop [event (<! result-channel)]
      (when event
        (<! (processor/process-event event))
        (recur (<! result-channel))))
    result-channel))


(defn stop [])
