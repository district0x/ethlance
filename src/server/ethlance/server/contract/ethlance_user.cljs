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
   [district.server.smart-contracts :as contracts]
   [ethlance.shared.enum.payment-type :as enum.payment]
   [ethlance.shared.enum.currency-type :as enum.currency]))


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
  [method-name & args]
  (requires-user-key)
  (apply contracts/contract-call *user-key* method-name args))


(defn user-address [] (call :user_address))
(defn user-id [] (call :user_id))
(defn date-created [] (call :date_created))
(defn date-updated [] (call :date_updated))


(defn metahash-ipfs [] (call :metahash_ipfs))


(defn update-metahash!
  "Update the user's IPFS metahash to `new-metahash`"
  [new-metahash & [opts]]
  (call :update-metahash new-metahash (merge {:gas 2000000} opts)))


(defn register-candidate!
  "Register the user contract's candidate profile."
  [{:keys [hourly-rate currency-type]}
   & [opts]]
  (call
   :register-candidate hourly-rate (enum.currency/kw->val currency-type)
   (merge {:gas 2000000} opts)))


(defn update-candidate!
  [{:keys [hourly-rate currency-type]} & [opts]]
  (call
   :update-candidate-rate hourly-rate (enum.currency/kw->val currency-type)
   (merge {:gas 2000000} opts)))


(defn candidate-data
  "Get the user's Candidate data."
  []
  (let [[is-registered? hourly-rate currency-type] (call :get-candidate-data)]
    {:is-registered? is-registered?
     :hourly-rate hourly-rate
     :currency-type (enum.currency/val->kw currency-type)}))


(defn register-arbiter!
  "Register the user contract's arbiter profile."
  [{:keys [payment-value
           currency-type
           payment-type]}
   & [opts]]
  (call
   :register-arbiter
   payment-value 
   (enum.currency/kw->val currency-type)
   (enum.payment/kw->val payment-type)
   (merge {:gas 1000000} opts)))


(defn update-arbiter!
  "Update the arbiter data."
  [{:keys [payment-value
           currency-type
           payment-type]}
   & [opts]]
  (call
   :update-arbiter-rate
   payment-value
   (enum.currency/kw->val currency-type)
   (enum.payment/kw->val payment-type)
   (merge {:gas 1000000} opts)))


(defn arbiter-data
  "Get the user's Arbiter data"
  []
  (let [[is-registered? payment-value currency-type payment-type] (call :get-arbiter-data)]
    {:is-registered? is-registered?
     :payment-value payment-value
     :currency-type (enum.currency/val->kw currency-type)
     :payment-type (enum.payment/val->kw payment-type)}))


(defn register-employer!
  "User the user as an employer."
  [& [opts]]
  (call :register-employer (merge {:gas 1000000} opts)))


(defn employer-data
  []
  (let [is-registered? (call :get-employer-data)]
    {:is-registered? is-registered?}))
