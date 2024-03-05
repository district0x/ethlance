(ns ethlance.server.contract.ethlance
  (:require
    [district.server.smart-contracts :as smart-contracts]))


(defn initialize
  [job-proxy-address]
  (smart-contracts/contract-send :ethlance :initialize [job-proxy-address] {:gas 6000000}))


(defn create-job
  ([creator offered-values arbiters ipfs-data]
   (create-job creator offered-values arbiters ipfs-data {}))

  ([creator offered-values arbiters ipfs-data merged-opts]
   (smart-contracts/contract-send
     :ethlance :create-job
     [creator offered-values arbiters ipfs-data]
     (merge {:gas 6000000} merged-opts))))
