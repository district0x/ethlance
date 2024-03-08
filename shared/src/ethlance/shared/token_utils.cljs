(ns ethlance.shared.token-utils
  (:require
    ["ethers" :refer [ethers]]
    ["evm-proxy-detection" :rename {default detectProxyTarget}]
    ["xhr2" :as xhr2]
    [cljs-http.client :as http]
    [cljs-web3-next.core :as w3-core]
    [cljs-web3-next.eth :as w3-eth]
    [clojure.core.async :as async :refer [<! go] :include-macros true]
    [ethlance.shared.contract-constants]
    [oops.core :refer [oget]]))


;; Needed because cljs-ajax doesn't find XMLHttpRequest on Node.js
;; https://github.com/JulianBirch/cljs-ajax/blob/359e83ca0cd628c22252ba861860cb921622a618/src/ajax/xml_http_request.cljs#L34-L35
(set! js/XMLHttpRequest xhr2)

(def api-key (atom "<YOUR ETHERSCAN API KEY>"))


(defn set-api-key
  [new-key]
  (reset! api-key new-key))


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


(defn parse-json
  [json-string]
  (.parse js/JSON json-string))


(def env :dev)


(def provider-url
  (if (= :dev env)
    "http://localhost:8549"
    "https://ethereum-mainnet-rpc.allthatnode.com"))


(defn promise->chan
  [promise]
  (let [channel (async/chan)]
    (.then promise #(async/put! channel %))
    channel))


(defn get-proxy-address
  [contract-address]
  (let [web3-instance (w3-core/create-web3 nil provider-url)
        provider (new (aget ethers "JsonRpcProvider") provider-url)
        request-fn (fn [params] (.send provider (oget params "method") (oget params "params")))]
    (promise->chan (detectProxyTarget contract-address request-fn))))


(defn get-abi-for-token-info
  [contract-address]
  (go
    (let [response (<! (get-contract-abi contract-address))
          abi-string (get-in response [:body :result])
          abi (parse-json abi-string)
          web3-instance (w3-core/create-web3 nil provider-url)
          contract-instance (w3-eth/contract-at web3-instance abi contract-address)
          name-method (oget contract-instance "?methods" "?name")
          symbol-method (oget contract-instance "?methods" "?symbol")]
      (when (and name-method symbol-method) abi-string))))


(defn get-abi-string
  [address]
  (go
    (or (<! (get-abi-for-token-info address))
        (<! (get-abi-for-token-info (<! (get-proxy-address address)))))))


(defn get-token-details
  [token-type contract-address]
  (go
    (let [abi (get ethlance.shared.contract-constants/abi token-type)
          ;; web3-instance (w3-core/create-web3 nil provider-url)
          web3-instance @district.server.web3/web3
          contract-instance (w3-eth/contract-at web3-instance abi contract-address)
          has-contract-method? (fn [contract method]
                                 (.hasOwnProperty (.-methods contract) method))
          token-name (when (has-contract-method? contract-instance "name")
                       (<! (promise->chan (w3-eth/contract-call contract-instance :name [] {}))))
          token-symbol (when (has-contract-method? contract-instance "symbol")
                         (<! (promise->chan (w3-eth/contract-call contract-instance :symbol [] {}))))
          token-decimals (case token-type
                           :eth 18
                           :erc721 0
                           :erc1155 1
                           :erc20 (if (has-contract-method? contract-instance "decimals")
                                    (<! (promise->chan (w3-eth/contract-call contract-instance :decimals [] {})))
                                    18))]
      {:address contract-address
       :type token-type
       :name token-name
       :symbol token-symbol
       :decimals token-decimals
       :web3-instance web3-instance
       :contract-instance contract-instance})))
