(ns ethlance.server.contract.ethlance-feedback
  "EthlanceFeedback contract methods"
  (:refer-clojure :exclude [count last])
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
  [metahash]
  (call :update metahash))


(defn count
  "Get the number of feedback revisions."
  []
  (call :get-feedback-count))


(defn feedback-by-index
  "Get the feedback revision at the given index"
  [index]
  (call :get-feedback-by-index index))
  
