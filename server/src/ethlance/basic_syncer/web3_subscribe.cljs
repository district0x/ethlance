(ns ethlance.basic-syncer.web3-subscribe
  (:require
    ["web3" :as Web3]
    [goog.object :as g]
    [taoensso.timbre :as log]
    [ethlance.shared.utils :refer [js-obj->clj-map]]
    ))

(def events-to-listen
  [:JobCreated
   :QuoteForArbitrationSet
   :QuoteForArbitrationAccepted
   :CandidateAdded
   :ArbitersInvited
   :InvoiceCreated
   :InvoicePaid
   :InvoiceCancelled
   :FundsWithdrawn
   :DisputeRaised
   :DisputeResolved
   :JobEnded
   :FundsWithdrawnFundsOut
   :TestEvent])

; Implementation:
; Events
;  new: https://web3js.readthedocs.io/en/v1.8.2/web3-eth-contract.html#contract-events
;  past: https://web3js.readthedocs.io/en/v1.8.2/web3-eth-contract.html#getpastevents
(defn init-web3 [node-address]
  (new Web3 node-address))

(defn init-contract [web3 abi address]
  (new (.-Contract (.-eth web3)) abi address))

(def last-event (atom nil))
(def last-config (atom nil))

(defn subscribe! [ethereum-node ethlance-abi ethlance-address]
  (log/info "web3-subscribe/subscribe WITH:" {:ethereum-node ethereum-node :ethlance-address ethlance-address :ethlance-abi ethlance-abi})
  (reset! last-config [ethereum-node ethlance-abi ethlance-address])

  (let [web3 (init-web3 ethereum-node)
        contract (init-contract web3 ethlance-abi ethlance-address)]
    (.allEvents (g/get contract "events")
                (fn [err event]
                  (reset! last-event event)
                  (let [event-name (g/get event "event")
                        block-number (g/get event "blockNumber")
                        tx-hash (g/get event "transactionHash")
                        return-values (js-obj->clj-map (.-returnValues event))
                        event-info {:name event-name :block block-number :tx tx-hash :return return-values}]
                    (log/info (str"🎯 --> basic-syncer EVENT:\n" (with-out-str (cljs.pprint/pprint event-info)))))))
    (log/info "Finished subscribing" contract)))
