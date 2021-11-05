(ns tests.runner
  (:require
   [cljs.nodejs :as nodejs]
   [district.server.logging]
   [district.server.web3]
   [tests.contract.ethlance-test]
   [tests.contract.job-test]
   [district.shared.async-helpers :as async-helpers]
   [cljs-promises.async]
   [ethlance.shared.smart-contracts-dev :refer [smart-contracts]]
   [district.server.smart-contracts]
   [mount.core :as mount]
   [taoensso.timbre :as log]))

(nodejs/enable-util-print!)

(async-helpers/extend-promises-as-channels!)

; Tests get run automatically by shadow.test.node/main which runs tests using cljs.test
; To run specific namespace tests, add --tests=<namespaces-separated-by-comma>
(defn setup-test-env []
  (-> (mount/with-args {:web3 {:url "ws://localhost:8549"} ; d0x-vm: "ws://d0x-vm:8549" hostia: "ws://192.168.32.1:7545"
                        :smart-contracts {:contracts-var #'smart-contracts}
                        :logging {:level :info
                                  :console? true}})
      (mount/only [#'district.server.logging/logging
                   #'district.server.web3/web3
                   #'district.server.smart-contracts/smart-contracts])
      (mount/start)
      (as-> $ (log/warn "Started" $))))

(println "tests.runner Running tests")
(setup-test-env)
