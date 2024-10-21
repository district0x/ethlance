(ns ethlance.server.new-syncer.core
  (:require
    ["node:fs" :as fs]
    [taoensso.timbre :as log]
    [goog.object :as g]
    [clojure.core.async :as async :refer [<! >! go] :include-macros true]
    [cljs.core.async.impl.protocols :refer [ReadPort]]
    [ethlance.server.new-syncer.web3-subscribe :as web3-subscribe]))


(defn read-json [path]
  (->> path
       (.readFileSync fs ,,,)
       (.parse js/JSON ,,,)))


(defn parse-contract-abi [contract-build-path]
  (g/get (read-json contract-build-path) "abi"))


(defn do-nothing-handler [_ _ _event]
  (println "Doing nothing with" _event))

(defn init-syncer
  "Starts up go-loop to read from `event-source`, process via handler in `event-handlers`
  and puts processing result in a returned channel

  event-source     - clojure async channel
  event-handlers   - map of event name => function of 3 args [context, err, event]
  context-provider - a function that gets called with callback whose 1st argument it will put context"
  [event-source event-handlers context-provider]
  (let [out-chan (async/chan (async/sliding-buffer 1))]
    (async/go-loop []
      (let [[error event] (async/<! event-source)
            event-name (:event event)
            handler (get event-handlers event-name do-nothing-handler)]
        (when (not (nil? (or event error)))
          (let [result (context-provider #(handler % error event))
                val-from-chan (if (satisfies? ReadPort result)
                                (<! result) ; Wait for the result if async
                                result)]
            (async/>! out-chan {:event event :result val-from-chan :name event-name})))
        (recur)))
    out-chan))
