(ns ethlance.server.new-syncer.web3-subscribe
  (:require
    [clojure.core.async :as async :refer [<!] :include-macros true]
    ["web3" :as Web3]
    [cljs-web3-next.core :as web3-next]
    [goog.object :as g]
    [taoensso.timbre :as log]
    [cljs-web3-next.helpers :as web3-helpers]
    [ethlance.shared.utils :refer [js-obj->clj-map]]
    [ethlance.server.debug :as debug]
    [district.server.smart-contracts :as server.smart-contracts]))

(defn init-web3 [node-address]
  (let [one-minute (* 60 1000)
        ws-config {:client-config
                   {:keepalive true
                    :keepalive-interval one-minute}
                   :reconnect
                   {:auto true
                    :delay 2000
                    :max-attempts false
                    :on-timeout true}}
        ws-provider (web3-next/ws-provider node-address ws-config)
        web3-instance (new Web3 ws-provider)]
    web3-instance))

(defn init-contract [web3 abi address]
  (new (.-Contract (.-eth web3)) abi address))

(def last-event (atom nil))
(def seen-events (atom []))

(defn record-for-debugging
  [err event]
  (reset! last-event event)
  (let [event-name (g/get event "event")
        block-number (g/get event "blockNumber")
        tx-hash (g/get event "transactionHash")
        return-values (js-obj->clj-map (.-returnValues event))
        event-info {:event-name event-name
                    :received-at (system-time)
                    :block-number block-number
                    :tx-hash tx-hash
                    :event event
                    :err err
                    :return-values return-values}]
    (swap! seen-events conj event-info)
    (log/info (str "🎯🎯 --> putting event to channel\n" (with-out-str (cljs.pprint/pprint event-info))))))

(def web3-instance (atom nil))

(def last-subscription (atom nil))

(defn subscribe-to-all
  [web3]
  )
(defn subscribe!
  "Takes config and output channel. Then subscribes to Web3.js for all events them to the output channel"
  [{:keys [ethereum-node ethlance-abi ethlance-address] :as params}
           output]
  (log/info "web3-subscribe/subscribe" {:ethereum-node ethereum-node :ethlance-address ethlance-address})
  (let [web3 (init-web3 ethereum-node)
        contract (init-contract web3 ethlance-abi ethlance-address)
        ;; Return value of .allEvents is documented at
        ;; https://web3js.readthedocs.io/en/v1.8.2/web3-eth-contract.html#contract-events-return
        subscription (.allEvents
                       (g/get contract "events")
                       (fn [err event]
                         (record-for-debugging err event)
                         (async/put! output
                                     [err
                                      (->> event
                                           web3-helpers/js->cljkk
                                           (server.smart-contracts/enrich-event-log :ethlance contract ,,,))])))]
    (reset! web3-instance web3)
    (.on subscription "error" (fn [err]
                                (log/error "ERROR with subscription" (.-message err))))
    (.on (.-currentProvider web3) "close" (fn [] (log/error "WebSocket connection closed. Trying to reconnect")
                                            (.once (.-currentProvider web3) "connect"
                                                   (fn [] (log/info "WebSocket connection re-established")))
                                            (js/setTimeout (fn []
                                                             (log/info "Reconnecting Web3")
                                                             (subscribe! params output)) 10000)))

    subscription))
