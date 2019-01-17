(ns ethlance.server.contract.ethlance-feedback
  "EthlanceFeedback contract methods"
  (:refer-clojure :exclude [count])
  (:require
   [bignumber.core :as bn]
   [cljs-web3.eth :as web3-eth]
   [district.server.smart-contracts :as contracts]))


(def ^:dynamic *feedback-key*
  "Feedback Key"
  nil) ;; [:ethlance-feedback "0x0"]


(defn- requires-feedback-key
  "Asserts the correct use of the feedback functions."
  []
  (assert *feedback-key* "Given function needs to be wrapped in 'with-ethlance-feedback"))


(defn call
  "Call the bound EthlanceFeedback contract with the given
  `method-name` and `args`."
  [method-name & args]
  (requires-feedback-key)
  (apply contracts/contract-call *feedback-key* method-name args))


(defn update!
  "Update the feedback with a revised feedback contained in the provided `metahash`"
  [rating metahash]
  (call :update rating metahash))


(defn count
  "Get the number of feedback revisions."
  []
  (call :get-feedback-count))


(defn feedback-by-index
  "Get the feedback at the given index"
  [index]
  (let [[from-user-address
         to-user-address
         from-user-type
         to-user-type
         metahash
         rating
         date-updated]
        (call :get-feedback-by-index index)]
    {:from-user-address from-user-address
     :to-user-address to-user-address
     :from-user-type from-user-type ;; FIXME: enum
     :to-user-type to-user-type ;; FIXME: enum}))
     :metahash metahash
     :rating rating ;; FIXME: big number conversion
     :date-updated date-updated})) ;; FIXME: big number conversion
