(ns tests.setup
  (:require
    [cljs.nodejs :as nodejs]
    [district.server.logging]
    ; Needs ETHLANCE_ENV=qa during truffle migrate
    [district.server.smart-contracts]
    [district.server.web3]
    [ethlance.shared.smart-contracts-qa :refer [smart-contracts]]
    [mount.core :as mount]
    [taoensso.timbre :as log]
    [tests.contract.ethlance-test]
    [tests.contract.job-test]))


(defn setup-test-env
  []
  (-> (mount/with-args {:web3 {:url "ws://localhost:8550"} ; d0x-vm: "ws://d0x-vm:8549" hostia: "ws://192.168.32.1:7545"
                        :smart-contracts
                        {:contracts-var #'smart-contracts
                         :contracts-build-path "../resources/public/contracts/build"}
                        :logging {:level :warn
                                  :console? true}})
      (mount/only [#'district.server.logging/logging
                   #'district.server.web3/web3
                   #'district.server.smart-contracts/smart-contracts])
      (mount/start)
      (as-> $ (log/warn "Started" $))))
