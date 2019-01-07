(ns ethlance.server.contract.ethlance-comment
  "EthlanceComment contract methods"
  (:require
   [bignumber.core :as bn]
   [cljs-web3.eth :as web3-eth]
   [district.server.smart-contracts :as contracts]))


(def ^:dynamic *comment-key*
  "Comment Key"
  nil) ;; [:ethlance-comment "0x0"]


(defn- requires-comment-key
  "Asserts the correct use of the comment functions."
  []
  (assert *comment-key* "Given function needs to be wrapped in 'with-ethlance-comment"))


(defn call
  "Call the bound EthlanceComment contract with the given
  `method-name` and `args`."
  [method-name & args]
  (requires-comment-key)
  (apply contracts/contract-call *comment-key* method-name args))
