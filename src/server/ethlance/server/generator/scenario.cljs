(ns ethlance.server.generator.scenario
  (:require
   [bignumber.core :as bn]
   [cljs-ipfs-api.files :as ipfs-files]
   [cljs-web3.core :as web3]
   [cljs-web3.eth :as web3-eth]
   [cljs-web3.evm :as web3-evm]
   [cljs-web3.utils :refer [js->cljkk camel-case]]
   [clojure.core.async :as async :refer [go go-loop <! >! chan] :include-macros true]
   [cuerdas.core :as str]
   [district.cljs-utils :refer [rand-str]]
   [district.format :as format]
   [district.server.config :refer [config]]
   [district.server.smart-contracts :as smart-contracts]
   [district.server.web3 :refer [web3]]
   [district.shared.error-handling :refer [try-catch]]
   [mount.core :as mount :refer [defstate]]
   [taoensso.timbre :as log]

   ;; Ethlance NS
   [ethlance.server.ipfs :as ipfs]
   [ethlance.server.filesystem :as filesystem]
   [ethlance.shared.random :as random]
   [ethlance.shared.async-utils :refer [<!-<log <!-<throw] :include-macros true]
   [ethlance.shared.enum.currency-type :as enum.currency]
   [ethlance.shared.enum.payment-type :as enum.payment]
   [ethlance.shared.enum.bid-option :as enum.bid-option]
   [ethlance.server.contract.ethlance-user :as user :include-macros true]
   [ethlance.server.contract.ethlance-user-factory :as user-factory]
   [ethlance.server.contract.ethlance-job-store :as job :include-macros true]
   [ethlance.server.contract.ethlance-job-factory :as job-factory]
   [ethlance.server.contract.ethlance-work-contract :as work-contract]
   [ethlance.server.contract.ethlance-invoice :as invoice :include-macros true]
   [ethlance.server.contract.ethlance-dispute :as dispute :include-macros true]
   [ethlance.server.deployer :as deployer]
   [ethlance.server.generator.choice-collections :as choice-collections]))


(declare generate-scenario!)
(def scenario-distribution
  "A random distribution of different types of scenarios to choose from"
  [[0.7 :job-no-requests]
   [0.2 :job-one-arbiter-request]
   [0.4 :job-one-arbiter-accepted]
   [0.2 :job-one-candidate-request]
   [0.4 :job-one-candidate-accepted]
   [0.5 :job-1a-req-1c-req]
   [0.2 :job-1a-req-1c-req]
   [1.2 :job-1a-acc-1c-acc]
   [1.4 :job-2a-acc-2c-acc]
   [2.0 :job-prog-w-invoice]
   [2.0 :job-prog-w-invoice-paid]
   [2.0 :job-prog-w-dispute]
   [3.0 :job-prog-w-inv-disp]
   [3.0 :job-prog-w-inv-paid-disp-resolved]])


(defn pick-scenario
  "Pick a random scenario from the probability distribution"
  []
  (random/pick-rand-by-dist scenario-distribution))


(defn generate-scenarios!
  "Generate different scenarios between the employers, candidates, and arbiters."
  [{:keys [num-scenarios employers candidates arbiters]
    :or {num-scenarios 5}
    :as scene-data}]
  (let [done-chan (chan 1)]
    (go-loop [i 1 scenario-name (pick-scenario)]
      (when (<= i num-scenarios)
        (generate-scenario! (assoc scene-data
                                   :scenario-name scenario-name
                                   :scenario-number i))
        (recur (inc i) (pick-scenario)))
      (>! done-chan ::done))
    done-chan))


(defmulti generate-scenario! (fn [scene-data] (get scene-data :scenario-name)))


(defmethod generate-scenario! :job-no-requests
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name)))


(defmethod generate-scenario! :job-one-arbiter-request
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name)))


(defmethod generate-scenario! :job-one-arbiter-accepted
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name)))


(defmethod generate-scenario! :job-one-candidate-request
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name)))


(defmethod generate-scenario! :job-one-candidate-accepted
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name)))


(defmethod generate-scenario! :job-1a-req-1c-req
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name)))


(defmethod generate-scenario! :job-1a-req-1c-req
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name)))


(defmethod generate-scenario! :job-1a-acc-1c-acc
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name)))


(defmethod generate-scenario! :job-2a-acc-2c-acc
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name)))


(defmethod generate-scenario! :job-prog-w-invoice
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name)))


(defmethod generate-scenario! :job-prog-w-invoice-paid
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name)))


(defmethod generate-scenario! :job-prog-w-dispute
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name)))


(defmethod generate-scenario! :job-prog-w-inv-disp
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name)))


(defmethod generate-scenario! :job-prog-w-inv-paid-disp-resolved
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name)))
