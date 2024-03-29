(ns ethlance.server.contract.job
  (:require
    [cljs.core.async :refer [<! go]]
    [district.server.smart-contracts :as smart-contracts]
    [ethlance.shared.contract-constants :as contract-constants]))


(defn max-withdrawable-amounts
  [job-address withdrawer-address]
  (go
    (let [amounts-from-contract (<! (smart-contracts/contract-call
                                      [:job job-address]
                                      :max-withdrawable-amounts
                                      []
                                      {:from withdrawer-address}))]
      (map contract-constants/token-value-vec->map (js->clj amounts-from-contract)))))


(defn get-deposits
  [job-address depositor]
  (go
    (let [amounts-from-contract (<! (smart-contracts/contract-call [:job job-address] :get-deposits [depositor]))]
      (map contract-constants/token-value-vec->map (js->clj amounts-from-contract)))))
