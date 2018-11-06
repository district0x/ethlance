(ns ethlance.server.contract.ethlance-invoice
  "EthlanceInvoice contract methods"
  (:require
   [bignumber.core :as bn]
   [cljs-web3.eth :as web3-eth]
   [district.server.smart-contracts :as contracts]))


(def ^:dynamic *invoice-key*
  "Invoice Key"
  nil) ;; [:ethlance-invoice "0x0"]


(defn call
  "Call the bound EthlanceInvoice contract with the given `method-name`
  and `args`."
  [method-name & args]
  (apply contracts/contract-call *invoice-key* method-name args))


(defn- requires-invoice-key
  "Asserts the correct use of the invoice contract functions."
  []
  (assert *invoice-key* "Given function needs to be wrapped in 'with-ethlance-invoice"))


