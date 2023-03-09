; (ns ethlance.shared.token-utils)
(ns ethlance.shared.token-utils
  (:require [cljs-web3-next.eth :as w3-eth]
            [cljs-web3-next.core :as w3-core]
            ; ["xmlhttprequest" :refer [XMLHttpRequest]]
            ; [ajax.core :as ajax.core]
            [oops.core :refer [ocall ocall+ oget oget+ oset! oapply oapply+]]
            ["xhr2" :as xhr2]
            ["ethers" :refer [ethers]]
            ["evm-proxy-detection" :rename {default detectProxyTarget}]
            [cljs-http.client :as http]
            [clojure.core.async :as async :refer [<! go] :include-macros true]))

; Needed because cljs-ajax doesn't find XMLHttpRequest on Node.js
; https://github.com/JulianBirch/cljs-ajax/blob/359e83ca0cd628c22252ba861860cb921622a618/src/ajax/xml_http_request.cljs#L34-L35
(set! js/XMLHttpRequest xhr2)

(def api-key (atom "<YOUR ETHERSCAN API KEY>"))
(defn set-api-key [new-key] (reset! api-key new-key))

(defn get-contract-abi
  ([contract-address]
   (get-contract-abi contract-address identity))

  ([contract-address transformer-fn]
   (get-contract-abi contract-address transformer-fn @api-key))

  ([contract-address transformer-fn api-key]
   (let [etherscan-api-url "https://api.etherscan.io/api"
         result-chan (async/chan)
         params {:module "contract"
                 :action "getabi"
                 :address contract-address
                 :apikey api-key}]
    (http/get etherscan-api-url {:query-params params}))))

(defn parse-json [json-string]
  (.parse js/JSON json-string))

(def provider-url "https://ethereum-mainnet-rpc.allthatnode.com") ; TODO: take from config (env specific)

(defn promise->chan [promise]
  (let [channel (async/chan)]
    (.then promise #(async/put! channel %))
    channel))

(defn get-proxy-address [contract-address]
  (let [web3-instance (w3-core/create-web3 nil provider-url)
        provider (new (aget ethers "JsonRpcProvider") provider-url)
        request-fn (fn [params] (.send provider (oget params "method") (oget params "params")))]
    (promise->chan (detectProxyTarget contract-address request-fn))))

(defn get-abi-for-token-info [contract-address]
  (go
    (let [response (<! (get-contract-abi contract-address))
          abi-string (get-in response [:body :result])
          abi (parse-json abi-string)
          web3-instance (w3-core/create-web3 nil provider-url)
          contract-instance (w3-eth/contract-at web3-instance abi contract-address)
          name-method (oget contract-instance "?methods" "?name")
          symbol-method (oget contract-instance "?methods" "?symbol")]
      (when (and name-method symbol-method) abi-string))))

(defn get-token-details [contract-address]
  (go
    (let [abi-string (<! (get-abi-for-token-info contract-address))
          abi-string (if (nil? abi-string)
                       (<! (get-abi-for-token-info (<! (get-proxy-address contract-address))))
                       abi-string)
          abi (parse-json abi-string)
          web3-instance (w3-core/create-web3 nil provider-url)
          contract-instance (w3-eth/contract-at web3-instance abi contract-address)
          token-name (<! (promise->chan (w3-eth/contract-call contract-instance :name [] {})))
          token-symbol (<! (promise->chan (w3-eth/contract-call contract-instance :symbol [] {})))
          result {:address contract-address
                  :name token-name
                  :symbol token-symbol
                  :abi abi
                  :abi-string abi-string
                  :web3-instance web3-instance
                  :contract-instance contract-instance}]
      result)))