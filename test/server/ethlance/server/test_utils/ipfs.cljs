(ns ethlance.server.test-utils.ipfs
 "Includes test utilities for working with ipfs."
 (:require
  [mount.core :as mount]
  [district.server.config :refer [config]]  
  [ethlance.server.ipfs :as ipfs]
  [ethlance.server.core]))


(def test-config
  (-> ethlance.server.core/main-config
      (merge {:logging {:level "debug" :console? true}})))


(defn fixture-start
  "Test Fixture Setup."
  [& [opts]]
  (-> (mount/with-args test-config)
      (mount/only
       [#'ethlance.server.ipfs/ipfs])
      mount/start))


(defn fixture-stop
  "Test Fixture Teardown."
  []
  (mount/stop))
