(ns ethlance.server.new-syncer.web3-subscribe
  (:require
    [clojure.core.async :as async :refer [<!] :include-macros true]
    ["web3" :as Web3]
    [goog.object :as g]
    [taoensso.timbre :as log]
    [cljs-web3-next.helpers :as web3-helpers]
    [ethlance.shared.utils :refer [js-obj->clj-map]]
    [district.server.smart-contracts :as server.smart-contracts]))

(defn init-web3 [node-address]
  (new Web3 node-address))

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

(defn subscribe!
  "Takes config and output channel. Then subscribes to Web3.js for all events them to the output channel"
  [{:keys [ethereum-node ethlance-abi ethlance-address]}
           output]
  (log/info "web3-subscribe/subscribe" {:ethereum-node ethereum-node :ethlance-address ethlance-address})
  (let [web3 (init-web3 ethereum-node)
        contract (init-contract web3 ethlance-abi ethlance-address)]
    ;; Return value of .allEvents is documented at
    ;; https://web3js.readthedocs.io/en/v1.8.2/web3-eth-contract.html#contract-events-return
    (.allEvents (g/get contract "events")
                (fn [err event]
                  (record-for-debugging err event)
                  (async/put! output
                              [err
                               (->> event
                                    web3-helpers/js->cljkk
                                    (server.smart-contracts/enrich-event-log :ethlance contract ,,,))])))))
