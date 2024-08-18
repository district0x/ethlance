(ns ethlance.ui.util.tokens
  (:require
    [cljs.core.async :as async]
    [cljs-web3-next.eth :as w3-eth]
    [cljs-web3-next.helpers :as web3-helpers]
    [clojure.math]
    [clojure.walk]
    [district.ui.smart-contracts.queries]
    [district.ui.web3.queries :as web3-queries]
    [ethlance.shared.utils :refer [wei->eth eth->wei]]
    [re-frame.db :refer [app-db]]))


(defn round
  [decimals amount]
  (let [multiplier (clojure.math/pow 10 decimals)]
    (/ (.round js/Math (* multiplier amount)) multiplier)))


(defn human-amount
  [amount token-type & [decimals]]
  (if (not (nil? decimals))
    (round decimals (/ amount (clojure.math/pow 10 decimals)))
    (case (keyword token-type)
      :eth (wei->eth amount)
      amount)))


(defn machine-amount
  [amount token-type]
  (case (keyword token-type)
    :eth (eth->wei amount)
    amount))


(defn fiat-amount-with-symbol
  [currency-id amount]
  (case currency-id
    :usd (str "$ " amount)
    :eur (str amount " €")
    amount))


(defn address->token-info-url
  [address]
  (str "https://ethplorer.io/address/" address))


(defn- remove-unnecessary-keys
  [k-v]
  (let [length-key "__length__"
        positional-arguments-count (int (get k-v length-key "0"))
        numeric-keys (map str (range positional-arguments-count))]
    (apply dissoc (into [k-v length-key] numeric-keys))))


(defn obj->clj
  [obj]
  (js->clj (-> obj js/JSON.stringify js/JSON.parse)))


(defn parse-event
  [web3 contract-instance raw-event event-name]
  (let [event-interface (web3-helpers/event-interface contract-instance event-name)
        event-data (:data raw-event)
        event-topics (:topics raw-event)
        decoded-event (w3-eth/decode-log web3 (:inputs event-interface) event-data (drop 1 event-topics))]
    (-> decoded-event
        obj->clj
        remove-unnecessary-keys
        web3-helpers/js->cljkk
        clojure.walk/keywordize-keys)))


(defn parse-event-in-tx-receipt [event receipt]
  (let [getter-fn identity
        {:keys [:logs]} (web3-helpers/js->cljkk receipt)
        contract-instance (district.ui.smart-contracts.queries/instance @app-db :ethlance)
        {:keys [:signature] :as event-interface} (web3-helpers/event-interface contract-instance event)
        sought-event? (fn [{:keys [:topics]}] (= signature (first topics)))
        web3 (web3-queries/web3 @app-db)
        decode-event-data (fn [{:keys [:data :topics]}] (w3-eth/decode-log web3 (:inputs event-interface) data (drop 1 topics)))
        clojurize (fn [return-values] (web3-helpers/return-values->clj return-values event-interface))]
    (println "parse-event-in-tx-receipt" {:logs logs :signature signature})
    (->> logs
         (filter sought-event? ,,,)
         (map decode-event-data ,,,)
         (map clojurize ,,,)
         getter-fn ,,,)))
