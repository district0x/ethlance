(ns ethlance.server.contract.ethlance-dispute
  "EthlanceDispute contract methods"
  (:require
   [bignumber.core :as bn]
   [cljs-web3.eth :as web3-eth]
   [district.server.smart-contracts :as contracts]))


(def ^:dynamic *dispute-key*
  "Dispute Key"
  nil) ;; [:ethlance-dispute "0x0"]


(defn call
  "Call the bound EthlanceDispute contract with the given
  `method-name` and `args`."
  [method-name & args]
  (apply contracts/contract-call *dispute-key* method-name args))


(defn- requires-dispute-key
  "Asserts the correct use of the dispute functions."
  []
  (assert *dispute-key* "Given function needs to be wrapped in 'with-ethlance-dispute"))
