(shadow/repl :dev-server)
(in-ns 'ethlance.server.core)

; Subscribe to contract event
(district.server.web3-events/register-callback! :ethlance/test-event (fn [err event] (println ">>> server.web3-events/register-callback!  GOT" {:err err :event event})))

; Call smart-contract method
(def res (district.server.smart-contracts/contract-send :ethlance :emitTestEvent [42]))
(def res-val (atom nil))
(cljs.core.async/take! res (fn [val] (reset! res-val val)))

; Subscribe (low level) via cljs-web3-next
(def ethlance-contract (:instance (district.server.smart-contracts.contract :ethlance)))
(cljs-web3-next.eth/subscribe-events ethlance-contract :TestEvent {:from-block 1} (fn [err event] (println ">>> cljs-web3-next/subscribe-events TestEvent" err event)))

; Subscribe via server-smart-contracts
(district.server.smart-contracts/subscribe-events :ethlance :TestEvent {:from-block 1} [(fn [err event] (println ">>> server.smart-contracts/subscribe-events TestEvent" err event))])

; IPFS data -> ipfs hash
(def ipfs-data "0x12204129c213954a4864af722e5160c92b158f1215c13416a1165a6ee7142371b368")

(require '[alphabase.base58 :as base58])
(require '[alphabase.hex :as hex])

(defn base58->hex [base58-str]
  (->> base58-str
    base58/decode
    hex/encode
    (str "0x" ,,,)))

(defn hex->base58 [hex-str]
  (-> hex-str
    (clojure.string/replace #"^0x" "")
    hex/decode
    base58/encode))

; DB operations (getting connection)
(in-ns 'ethlance.server.db)
(def conn (atom nil))
(.then (db/get-connection) #(reset! conn %))
