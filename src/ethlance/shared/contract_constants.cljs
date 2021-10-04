(ns ethlance.shared.contract-constants)

(def job-type {:gig 0 :bounty 1})
(def token-type {:eth 0 :erc20 1 :erc721 2 :erc1155 3})
(def operation-type {:one-step-job-creation 0 :two-step-job-creation 1 :add-funds 2})
; Corresponds to the enum TargetMethod values in Job contract
(def job-callback-target-method {:accept-quote-for-arbitration 0 :add-funds 1 :add-funds-and-pay-invoice 2})

; TODO: implement token-value-vec->map to parse the vector
;       using this structure instead
(def token-value-mappings
  {0 :token
   1 :value
   :token
   {0 :token-contract
    1 :token-id
    :token-contract
    {0 :token-type
     1 :token-address}}})

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
    {:token {:token-contract {:token-type (get-int [0 0 0])
                              :token-address (get-at [0 0 1])}
             :token-id (get-int [0 1])}
     :value (get-int [1])}))
