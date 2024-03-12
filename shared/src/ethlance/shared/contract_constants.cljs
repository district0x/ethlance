(ns ethlance.shared.contract-constants
  (:require
    [shadow.resource]))


(def token-types {:eth 0 :erc20 1 :erc721 2 :erc1155 3})


(defn token-type->enum-val
  [token-type]
  (let [str-token-type (if (keyword? token-type)
                         (name token-type)
                         (str token-type))]
    (get token-types (keyword (clojure.string/lower-case str-token-type)) :not-found)))


(defn enum-val->token-type
  [enum-val]
  (get (clojure.set/map-invert token-types) enum-val :not-found))


;; Corresponds to the enum OperationType in Ethlance contract
(def operation-type {:one-step-job-creation 0 :two-step-job-creation 1 :add-funds 2})


;; Corresponds to the enum TargetMethod values in Job contract
(def job-callback-target-method {:accept-quote-for-arbitration 0 :add-funds 1 :add-funds-and-pay-invoice 2})


(defn token-value-vec->map
  "Transforms the EthlanceStructs.TokenValue from nested array into Map.
   Due to implementation issues the response from web3 gets returned as
   nested arrays (losing the keys information).

   TODO: fix the implementation instead. Relying on array element order can be
         unreliable (can't depend on the serialization logic to always keep the
         order the same)"
  [value-vec]
  (let [get-at (partial get-in value-vec)
        get-int (comp js/parseInt get-at)]
    {:token {:tokenContract {:tokenType (get-int [0 0 0])
                             :tokenAddress (get-at [0 0 1])}
             :tokenId (get-int [0 1])}
     :value (get-int [1])}))


(defn offered-vec->nested-map
  "Basically same (without type conversions) as token-value-vec->map"
  [offered]
  {:value (get-in offered [1])
   :token
   {:tokenId (get-in offered [0 0 0])
    :tokenContract
    {:tokenType (get-in offered [0 1])
     :tokenAddress (get-in offered [0 0 1])}}})


;; Parsed structure (EthlanceStructs.OfferedValue)
;;  [ [ [3 0xE13fD5Ed78f1306B4C7C9c3C96FDB99CFc943C5B] 1] 6]
(defn offered-vec->flat-map
  "Basically same (without type conversions) as token-value-vec->map"
  [offered]
  {:token-type (js/parseInt (get-in offered [0 0 0]))
   :token-amount (get-in offered [1])
   :token-address (get-in offered [0 0 1])
   :token-id (js/parseInt (get-in offered [0 1]))})


(defn json-str->clj-abi
  [json]
  (as-> json j
        (.parse js/JSON j)
        (js->clj j)
        (get j "abi")
        (clj->js j)))


(def abi
  {:erc20 (json-str->clj-abi (shadow.resource/inline "./abis/ERC20.json"))
   :erc721 (json-str->clj-abi (shadow.resource/inline "./abis/ERC721.json"))
   :erc1155 (json-str->clj-abi (shadow.resource/inline "./abis/ERC1155.json"))})
