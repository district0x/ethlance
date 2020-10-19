(ns ethlance.server.contract.ds-guard
  "Functions for manipulating the DSGuard contract."
  (:require [district.server.smart-contracts :as contracts]
            ethlance.server.contract))

(def ^:dynamic *guard-key*
  "The default guard contract key."
  :ds-guard)

(defn address
  "Address of the Deployed DSGuard Instance."
  []
  (contracts/contract-address *guard-key*))

(defn call
  "Call the DSGuard contract with the given `method-name` and using the
  given `args`."
  [method-name args opts]
  (ethlance.server.contract/call
   :contract-key *guard-key*
   :method-name method-name
   :contract-arguments args
   :contract-options opts))

(def ANY
  "The ANY address for authority whitelisting."
  "0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff")

(defn permit!
  "Permit a given `src` address the authority to call methods at `dst`
  address with function signature `sig`.

  Key Arguments:

  :src - The source address.

  :dst - The destination address.

  :sig - The calldata Function Identifier.

  Optional Arguments - `opt`

  `opt` key-vals are web3 contract-call override options.

  Notes:

  - Providing ds-guard/ANY to any of the fields will permit ANYone
  authorization in that particular scenario.

  Examples:

  - (permit {:src ANY :dst (contract :foo) :sig ANY})
    ;; Anyone can call the contract :foo, on any method.

  - (permit {:src my-address :dst ANY :sig ANY})
    ;; `my-address` can call any contract, on any method.

  "
  [{:keys [:src :dst :sig]} & [opts]]
  (call :permit [src dst sig] (merge {:gas 100000} opts)))

(defn can-call?
  "Returns true if the given `src` `dst` combination is authorized to
  perform the given contract-call defined by `sig`, otherwise false."
  [{:keys [:src :dst :sig]}]
  (call :can-call [src dst sig] {}))

(defn permit-any!
  "Permits all actions by source addresses on the given destination
  contract address."
  [dst & [opts]]
  (permit! {:src ANY :dst dst :sig ANY} opts))
