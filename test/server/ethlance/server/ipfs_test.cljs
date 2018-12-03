(ns ethlance.server.ipfs-test
  "Tests for IPFS use cases."
  (:require
   [clojure.core.async :as async :refer [go go-loop <! >! chan close!] :include-macros true]
   [clojure.test :refer [deftest is are testing async]]

   [taoensso.timbre :as log]
   [cuerdas.core :as str]
   [mount.core :as mount]

   [district.server.config :refer [config]]

   [ethlance.server.test-utils.ipfs :refer [deftest-ipfs] :include-macros true]
   [ethlance.server.ipfs :as ipfs]
   [ethlance.server.core]))


(deftest-ipfs main-ipfs {}
  (async
   done
   (let [test-data "Here is a piece of test data"
         [result-chan err-chan] (ipfs/add! (ipfs/to-buffer test-data))]
     (go
      (when-let [err (<! err-chan)]
        (log/error "IPFS Test: Failed to add IPFS data" err))
  
      (let [result (<! result-chan)]
        (is (not (nil? result)))
        (log/debug result))))))
