(ns ethlance.server.contract.ethlance-invoice
  "EthlanceInvoice contract methods"
  (:require
   [bignumber.core :as bn]
   [cljs-web3.eth :as web3-eth]
   [district.server.smart-contracts :as contracts]
   [clojure.core.async :as async :refer [go go-loop <! >! chan] :include-macros true]
   [ethlance.shared.async-utils :refer [<!-<log <!-<throw flush! go-try] :include-macros true]
   [ethlance.server.contract]))


(def ^:dynamic *invoice-key*
  "Invoice Key"
  nil) ;; [:ethlance-invoice "0x0"]


(defn call
  "Call the bound EthlanceInvoice contract with the given `method-name`
  and `args`."
  [address method-name args & [opts]]
  (ethlance.server.contract/call
   :contract-key [:ethlance-invoice address]
   :method-name method-name
   :contract-arguments args
   :contract-options (or opts {})))


(defn date-created [address] (call address :date_created []))
(defn date-updated [address] (call address :date_updated []))
(defn date-paid [address] (call address :date_paid []))

(defn amount-requested [address] (call address :amount_requested []))
(defn amount-paid [address] (call address :amount_paid []))


(defn pay!
  "Pay the invoice with the provided `amount`"
  [address amount & [opts]]
  (call address :pay [amount] (merge {:gas 1000000} opts)))


(defn paid?
  "Returns true if the given invoice has been paid, otherwise false."
  [address]
  (call address :is-invoice-paid []))


(defn add-comment!
  [address metahash & [opts]]
  (call address :add-comment [metahash] (merge {:gas 1500000} opts)))
