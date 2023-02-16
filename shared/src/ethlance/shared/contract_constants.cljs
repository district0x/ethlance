(ns ethlance.shared.contract-constants)

(def token-types {:eth 0 :erc20 1 :erc721 2 :erc1155 3})

(defn token-type->enum-val [type]
  (get token-types (keyword type) :not-found))

(defn enum-val->token-type [enum-val]
  (println ">>> enum-val->token-type" enum-val (type enum-val))
  (get (clojure.set/map-invert token-types) enum-val :not-found))

(def operation-type {:one-step-job-creation 0 :two-step-job-creation 1 :add-funds 2})
; Corresponds to the enum TargetMethod values in Job contract
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

(defn offered-vec->flat-map
  "Basically same (without type conversions) as token-value-vec->map"
  [offered]
  {:token-type (js/parseInt (get-in offered [0 1]))
   :token-amount (get-in offered [1])
   :token-address (get-in offered [0 0 1])
   :token-id (get-in offered [0 0 0])})
