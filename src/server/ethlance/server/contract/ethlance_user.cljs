(ns ethlance.server.contract.ethlance-user
  "EthlanceUser contract methods"
  (:require
   [bignumber.core :as bn]
   [cljs-web3.eth :as web3-eth]
   [district.server.smart-contracts :as contracts]))


(def ^:dynamic *user-key*
  "This is rebound by the `with-ethlance-user` macro"
  [:ethlance-user "0x0"])


(defn metahash-ipfs [& [opts]]
  (contracts/contract-call *user-key* :metahash_ipfs (merge {:gas 1000000} opts)))


