(ns ethlance.server.contract.ethlance-feedback
  "EthlanceFeedback contract methods"
  (:refer-clojure :exclude [count])
  (:require
   [bignumber.core :as bn]
   [cljs-web3.eth :as web3-eth]
   [district.server.smart-contracts :as contracts]
   [ethlance.shared.enum.user-type :as enum.user-type]
   [clojure.core.async :as async :refer [go go-loop <! >! chan] :include-macros true]
   [ethlance.shared.async-utils :refer [<!-<log <!-<throw flush! go-try] :include-macros true]
   [ethlance.server.contract]))


(def ^:dynamic *feedback-key*
  "Feedback Key"
  nil) ;; [:ethlance-feedback "0x0"]


(defn call
  "Call the bound EthlanceFeedback contract with the given
  `method-name` and `args`."
  [address method-name args & [opts]]
  (ethlance.server.contract/call
   :contract-key [:ethlance-feedback address]
   :method-name method-name
   :contract-arguments args
   :contract-options (or opts {})))


(defn count
  "Get the number of feedback revisions."
  [address]
  (call address :get-feedback-count []))


(defn feedback-by-index
  "Get the feedback at the given index"
  [address index]
  (let [result-channel (chan 1)
        [success-channel error-channel] (call address :get-feedback-by-index [index])]
    (go
      (let [[from-user-address
             to-user-address
             from-user-type
             to-user-type
             metahash
             rating
             date-created
             date-updated]
            (<! success-channel)]
        (>! result-channel
            {:from-user-address from-user-address
             :to-user-address to-user-address
             :from-user-type (enum.user-type/val->kw from-user-type)
             :to-user-type (enum.user-type/val->kw to-user-type)
             :metahash metahash
             :rating (bn/number rating)
             :date-created (bn/number date-created)
             :date-updated (bn/number date-updated)})))
    [result-channel error-channel]))
