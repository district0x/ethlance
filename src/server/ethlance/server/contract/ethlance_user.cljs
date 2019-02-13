(ns ethlance.server.contract.ethlance-user
  "EthlanceUser contract methods

  Notes:

  - In order to apply the functions to a particular user's contract
  the function calls need to be wrapped in `with-ethlance-user`.

  Examples:

  ```clojure
  (require '[ethlance.server.contract.ethlance-user-factory :as user-factory])
  (require '[ethlance.server.contract.ethlance-user :as user :include-macros true])

  (def uid) ;; defined user-id
  ;;...

  (user/with-ethlance-user (user-factory/user-by-address uid)
    (user/update-metahash! \"<New Metahash>\"))
  ```"
  (:require
   [bignumber.core :as bn]
   [cljs-web3.eth :as web3-eth]
   [clojure.core.async :refer [go go-loop <! >! chan close!] :include-macros true]
   [district.server.smart-contracts :as contracts]
   [ethlance.shared.enum.payment-type :as enum.payment]
   [ethlance.shared.enum.currency-type :as enum.currency]
   [ethlance.server.contract]
   [ethlance.shared.async-utils :refer [<!-<throw go-try] :include-macros true]))


(def ^:dynamic *user-key*
  "This is dynamically rebound by the `with-ethlance-user` macro"
  nil) ;; [:ethlance-user "0x0"]


(defn- requires-user-key
  "Asserts the correct use of the user functions."
  []
  (assert *user-key* "Given function needs to be wrapped in 'with-ethlance-user"))


(defn call
  "Call the bound EthlanceUser contract with the given `method-name` and
  `args`."
  [contract-address method-name args & [opts]]
  (requires-user-key)
  (ethlance.server.contract/call
   :contract-key [:ethlance-user contract-address]
   :method-name method-name
   :contract-arguments args
   :contract-options (or opts {})))


(defn user-address [address] (call address :user_address []))
(defn user-id [address] (call address :user_id []))
(defn date-created [address] (call address :date_created []))
(defn date-updated [address] (call address :date_updated []))


(defn metahash-ipfs [address] (call address :metahash_ipfs []))


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
     (let [[is-registered? payment-value currency-type payment-type] (<! success-channel)]
       (>! result-channel {:is-registered? is-registered?
                           :payment-value payment-value
                           :currency-type (enum.currency/val->kw currency-type)
                           :payment-type (enum.payment/val->kw payment-type)})))
    [result-channel error-channel]))


(defn register-employer!
  "User the user as an employer."
  [address & [opts]]
  (call address :register-employer (merge {:gas 1000000} opts)))


(defn employer-data
  [address]
  (let [is-registered? (call address :get-employer-data)]
    {:is-registered? is-registered?}))
