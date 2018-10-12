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
   [district.server.smart-contracts :as contracts]))


(def ^:dynamic *user-key*
  "This is rebound by the `with-ethlance-user` macro"
  nil) ;; [:ethlance-user "0x0"]


(defn metahash-ipfs
  "Retrieve the user's IPFS metahash."
  [& [opts]]
  (assert *user-key*)
  (contracts/contract-call *user-key* :metahash_ipfs (merge {:gas 1000000} opts)))


(defn update-metahash!
  "Update the user's IPFS metahash to `new-metahash`"
  [new-metahash & [opts]]
  (assert *user-key*)
  (contracts/contract-call
   *user-key* :update-metahash new-metahash
   (merge {:gas 2000000} opts)))


