(ns ethlance.server.test-utils
  "Includes fixtures and additional utilities to improve the process of
  testing ethereum contracts."
  (:require
   [clojure.test :refer [deftest is are testing use-fixtures async]]

   [cljs-web3.eth :as web3-eth]
   [mount.core :as mount]
   
   [ethlance.server.core]
   [ethlance.server.deployer :as deployer]

   ;; Mount Components
   [district.server.web3 :refer [web3]]
   [district.server.smart-contracts :as contracts]))
   

(def test-config
  "Test configuration for districts."
  (-> ethlance.server.core/main-config
      (merge {:logging {:level "debug" :console? true}})
      (update :smart-contracts merge {:print-gas-usage? true
                                      :auto-mining? true})))


(def default-deployer-config
  "Default Configuration for Smart Contract Deployments."
  {})


(defn fixture-start
  "Test Fixture Setup."
  [{:keys [deployer-options]}]
  (-> (mount/with-args test-config)
      (mount/only
       [#'district.server.web3/web3
        #'district.server.smart-contracts/smart-contracts])
      mount/start)
  (deployer/deploy-all! (merge default-deployer-config deployer-options)))


(defn fixture-stop
  "Test Fixture Teardown."
  []
  (mount/stop)
  (async done (js/setTimeout #(done) 1000)))


(defn with-smart-contract
  "A test fixture for performing a fresh smart contract deployment
  before the tests.
  
  Optional Arguments
  
  :deployer-options - Additional Deployment Options to provide the
  deployer."
  [& [opts]]
  (fn [f]
    (fixture-start opts)
    (f)
    (fixture-stop)))
