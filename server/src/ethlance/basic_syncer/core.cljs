(ns ethlance.basic-syncer.core
  (:require
    ["node:fs" :as fs]
    [goog.object :as g]
    [ethlance.basic-syncer.web3-subscribe :as web3-subscribe]
    [taoensso.timbre :as log]))

(defn get-pid [] (.-pid js/process))

(set! *warn-on-infer* false)

(defn read-json [path]
  (->> path
       (.readFileSync fs ,,,)
       (.parse js/JSON ,,,)))

(defn start-from-ethlance-server-config! [config]
  (log/info ">>> START start-from-ethlance-server-config!")
  (let [ethereum-node (get-in config [:web3 :url])
        ethlance-json-path (str (get-in config [:smart-contracts :contracts-build-path]) "/Ethlance.json")
        ethlance-json (read-json ethlance-json-path)
        ethlance-abi (g/get ethlance-json "abi")
        contract (get-in config [:smart-contracts :contracts-var])
        ethlance-address (get-in @contract [:ethlance :address])]
    (web3-subscribe/subscribe! ethereum-node ethlance-abi ethlance-address)))

(defn -main []
  (println "🌟 ethlance.basic-syncer.core/-main starting PID" (get-pid))
  (let [ethereum-node "ws://d0x-vm:8549"
        contract-json (read-json "resources/public/contracts/build/Ethlance.json")
        ethlance-abi (g/get contract-json "abi")
        ethlance-on-qa "0xb9232E80982072316D6Ef3a14B5D9a79cc65aAb1"
        ethlance-on-dev "0xF119b2499a673DC88af9F71fF4a79beEd56cf2bc"]
    (web3-subscribe/subscribe! ethereum-node ethlance-abi ethlance-on-dev)))

(defn ^:dev/before-load-async stop [done]
  (log/info "ethlance.basic-syncer.core/stop PID" (get-pid))
  (js/setTimeout
    (fn []
      (log/info "stop complete")
      (done))))

(defn ^:dev/after-load-async start [done]
  (log/info "ethlance.basic-syncer.core/start PID" (get-pid))
  (js/setTimeout
    (fn []
      (-main)
      (log/info "start complete")
      (done))))
