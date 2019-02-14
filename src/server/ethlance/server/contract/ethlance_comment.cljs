(ns ethlance.server.contract.ethlance-comment
  "EthlanceComment contract methods"
  (:refer-clojure :exclude [count last])
  (:require
   [bignumber.core :as bn]
   [cljs-web3.eth :as web3-eth]
   [district.server.smart-contracts :as contracts]
   [ethlance.shared.enum.user-type :as enum.user-type]
   [ethlance.shared.enum.comment-type :as enum.comment-type]
   [clojure.core.async :as async :refer [go go-loop <! >! chan] :include-macros true]
   [ethlance.shared.async-utils :refer [<!-<log <!-<throw flush! go-try] :include-macros true]
   [ethlance.server.contract]))


(def ^:dynamic *comment-key*
  "Comment Key"
  nil) ;; [:ethlance-comment "0x0"]


(defn call
  "Call the bound EthlanceComment contract with the given
  `method-name` and `args`."
  [address method-name args & [opts]]
  (ethlance.server.contract/call
   :contract-key [:ethlance-comment address]
   :method-name method-name
   :contract-arguments args
   :contract-options (or opts {})))


(defn user-type
  [address]
  (let [result-channel (chan 1)
        [success-channel error-channel] (call address :user_type [])]
    (go (let [result (<! success-channel)]
          (>! result-channel (enum.user-type/val->kw result))))
    [result-channel error-channel]))


(defn user-address [address] (call address :user_address []))
(defn date-created [address] (call address :date_created []))
(defn date-updated [address] (call address :date_updated []))


(defn comment-type
  [address]
  (let [result-channel (chan 1)
        [success-channel error-channel] (call address :comment_type [])]
    (go (let [result (<! success-channel)]
          (>! result-channel (enum.comment-type/val->kw (bn/number result)))))
    [result-channel error-channel]))


(defn sub-index
  "Get the sub index of the comment

  # Notes:

  Depending on the `comment-type`

      For \"WorkContract\":
      [0] --> Job Index
      [1] --> Work Contract Index
      [2] --> Comment Index

      For \"Invoice\":
      [0] --> Job Index
      [1] --> Work Contract Index
      [2] --> Invoice Index
      [3] --> Comment Index

      For \"Dispute\":
      [0] --> Job Index
      [1] --> Work Contract Index
      [2] --> Dispute Index
      [3] --> Comment Index
  "
  [address x] (call address :get-index [x]))


(defn update!
  "Update the comment with a revised comment contained in the provided `metahash`"
  [address metahash & [opts]]
  (call address :update [metahash] (merge {:gas 1000000} opts)))


(defn count
  "Get the number of comment revisions."
  [address]
  (call address :get-count []))


(defn revision-by-index
  "Get the comment revision at the given index"
  [address index]
  (call address :get-revision-by-index [index]))


(defn last
  "Get the current comment metahash"
  [address]
  (call address :get-last []))
