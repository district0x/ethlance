(shadow/repl :dev-ui)
(require '[re-frame.db :refer [app-db]])
(require '[re-frame.core :as re])
(require '[cljs-web3-next.core :as w3n-core])
(require '[cljs-web3-next.eth :as w3n-eth])
(require '[ethlance.shared.smart-contracts-dev :as scd])
(require '[district.ui.smart-contracts.queries :as sma-co-q])

;;;;;;;;;;;;;;;;;;;;;;;
;; Mind & permit multi-token to a user (to be used as payment for a new job)
(def multi-token-address (get-in scd/smart-contracts [:test-multi-token :address]))
(def ethlance-address (get-in scd/smart-contracts [:ethlance :address]))
(def multi-token-instance (sma-co-q/instance @app-db :test-multi-token))
(def receiver-address (get-in @app-db [:district.ui.web3-accounts :active-account]))

(def tx-result (atom nil))
(def tx-return (atom nil))

; Mint new token
(reset! tx-result (w3n-eth/contract-send multi-token-instance :award-item [receiver-address 1000] {:from receiver-address} (fn [res] (println ">>> multi-token mint callback" res))))
(.then @tx-result (fn [res] (reset! tx-return res)))
(.catch @tx-result (fn [res] (reset! tx-return res)))

(def created-nft-id (.-id (get-in (js->clj @tx-return) ["events" "TransferSingle" "returnValues"])))
(def created-nft-amount (.-value (get-in (js->clj @tx-return) ["events" "TransferSingle" "returnValues"])))

; Give permissions to Ethlance
(reset! tx-result (w3n-eth/contract-send multi-token-instance :set-approval-for-all [ethlance-address true] {:from receiver-address} (fn [res] (println ">>> multi-token set-approval-for-all callback" res))))
(.then @tx-result (fn [res] (reset! tx-return res)))
(.catch @tx-result (fn [res] (reset! tx-return res)))
