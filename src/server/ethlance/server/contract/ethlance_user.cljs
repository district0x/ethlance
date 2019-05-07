(ns ethlance.server.contract.ethlance-user
  "EthlanceUser contract methods"
  (:require
   [bignumber.core :as bn]
   [cljs-web3.eth :as web3-eth]
   [clojure.core.async :refer [go go-loop <! >! chan close!] :include-macros true]
   [district.server.smart-contracts :as contracts]
   [ethlance.shared.enumeration.payment-type :as enum.payment]
   [ethlance.shared.enumeration.currency-type :as enum.currency]
   [ethlance.server.contract]
   [ethlance.shared.async-utils :refer [<!-<throw go-try] :include-macros true]))


(defn call
  "Call the bound EthlanceUser contract with the given `method-name` and
  `args`."
  [contract-address method-name args & [opts]]
  (ethlance.server.contract/call
   :contract-key [:ethlance-user contract-address]
   :method-name method-name
   :contract-arguments args
   :contract-options (or opts {})))


(defn user-address [address] (call address :user-address []))
(defn user-id [address] (call address :user-id []))
(defn date-created [address] (call address :date-created []))
(defn date-updated [address] (call address :date-updated []))


(defn metahash-ipfs [address] (call address :metahash []))


(defn update-metahash!
  "Update the user's IPFS metahash to `new-metahash`"
  [address new-metahash & [opts]]
  (call address :update-metahash [new-metahash] (merge {:gas 2000000} opts)))


(defn register-candidate!
  "Register the user contract's candidate profile."
  [address {:keys [hourly-rate currency-type]} & [opts]]
  (call address
        :register-candidate
        [hourly-rate (enum.currency/kw->val currency-type)]
        (merge {:gas 2000000} opts)))


(defn update-candidate!
  [address {:keys [hourly-rate currency-type]} & [opts]]
  (call address
        :update-candidate-rate
        [hourly-rate (enum.currency/kw->val currency-type)]
        (merge {:gas 2000000} opts)))


(defn candidate-data
  "Get the user's Candidate data."
  [address]
  (let [result-channel (chan 1)
        [success-channel error-channel] (call address :get-candidate-data [])] 
    (go-try
     (let [[is-registered? hourly-rate currency-type] (<! success-channel)]
       (>! result-channel
           {:is-registered? is-registered?
            :hourly-rate hourly-rate
            :currency-type (enum.currency/val->kw currency-type)})
       (close! result-channel)))
    [result-channel error-channel]))


(defn register-arbiter!
  "Register the user contract's arbiter profile."
  [address {:keys [payment-value currency-type payment-type]} & [opts]]
  (call address
        :register-arbiter
        [payment-value 
         (enum.currency/kw->val currency-type)
         (enum.payment/kw->val payment-type)]
        (merge {:gas 1000000} opts)))


(defn update-arbiter!
  "Update the arbiter data."
  [address {:keys [payment-value currency-type payment-type]} & [opts]]
  (call address
        :update-arbiter-rate
        [payment-value
         (enum.currency/kw->val currency-type)
         (enum.payment/kw->val payment-type)]
        (merge {:gas 1000000} opts)))


(defn arbiter-data
  "Get the user's Arbiter data"
  [address]
  (let [result-channel (chan 1)
        [success-channel error-channel] (call address :get-arbiter-data [])] 
    (go-try
     (if-let [[is-registered? payment-value currency-type payment-type] (<! success-channel)]
       (>! result-channel {:is-registered? is-registered?
                           :payment-value payment-value
                           :currency-type (enum.currency/val->kw currency-type)
                           :payment-type (enum.payment/val->kw payment-type)})
       (close! result-channel)))
    [result-channel error-channel]))


(defn register-employer!
  "User the user as an employer."
  [address & [opts]]
  (call address :register-employer [] (merge {:gas 1000000} opts)))


(defn employer-data
  [address]
  (let [result-channel (chan 1)
        [success-channel error-channel] (call address :get-employer-data [])]
    (go-try
     (if-let [is-registered? (<! success-channel)]
       {:is-registered? is-registered?}
       (close! result-channel)))
    [result-channel error-channel]))
