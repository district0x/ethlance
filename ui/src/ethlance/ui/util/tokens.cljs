(ns ethlance.ui.util.tokens
  (:require
    [re-frame.core :as re]
    [clojure.math]
    [cljs-web3-next.helpers :as web3-helpers]
    [ethlance.shared.utils :refer [wei->eth eth->wei]]
    [cljs-web3-next.eth :as w3-eth]))

(defn round [decimals amount]
  (let [multiplier (clojure.math/pow 10 decimals)]
    (/ (.round js/Math (* multiplier amount)) multiplier)))

(defn human-amount [amount token-type]
  (case (keyword token-type)
    :eth (wei->eth amount)
    amount))

(defn machine-amount [amount token-type]
  (case (keyword token-type)
    :eth (eth->wei amount)
    amount))

(defn fiat-amount-with-symbol [currency-id amount]
  (case currency-id
    :usd (str "$ " amount)
    :eur (str amount " €")
    amount))

(defn address->token-info-url [address]
  (str "https://ethplorer.io/address/" address))

(defn- remove-unnecessary-keys [k-v]
  (let [length-key "__length__"
        positional-arguments-count (int (get k-v length-key "0"))
        numeric-keys (map str (range positional-arguments-count))]
    (apply dissoc (into [k-v length-key] numeric-keys))))

(defn obj->clj [obj]
  (js->clj (-> obj js/JSON.stringify js/JSON.parse)))

(defn parse-event [web3 contract-instance raw-event event-name]
  (let [event-interface (web3-helpers/event-interface contract-instance event-name)
        event-data (:data raw-event)
        event-topics (:topics raw-event)
        decoded-event (w3-eth/decode-log web3 (:inputs event-interface) event-data (drop 1 event-topics))]
   (-> decoded-event
       obj->clj
       remove-unnecessary-keys
       web3-helpers/js->cljkk
       clojure.walk/keywordize-keys)))
