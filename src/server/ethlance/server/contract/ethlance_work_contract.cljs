(ns ethlance.server.contract.ethlance-work-contract
  "EthlanceWorkContract contract methods"
  (:require
   [bignumber.core :as bn]
   [cljs-web3.eth :as web3-eth]
   [district.server.smart-contracts :as contracts]))


(def ^:dynamic *work-contract-key*
  "WorkContract Key"
  nil) ;; [:ethlance-work-contract "0x0"]


(defn- requires-work-contract-key
  "Asserts the correct use of the work contract functions."
  []
  (assert *work-contract-key* "Given function needs to be wrapped in 'with-ethlance-work-contract"))
