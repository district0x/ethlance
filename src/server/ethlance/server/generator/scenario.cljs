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
   [0.5 :job-2a-req-2c-req]
   [1.2 :job-1a-acc-1c-acc]
   [1.4 :job-2a-acc-2c-acc]
   [2.0 :job-prog-w-invoice]])
   ;;[2.0 :job-prog-w-invoice-paid]])
   ;;[2.0 :job-prog-w-dispute]
   ;;[3.0 :job-prog-w-inv-disp]
   ;;[3.0 :job-prog-w-inv-paid-disp-resolved]])


(defn pick-scenario
  "Pick a random scenario from the probability distribution"
  []
  (random/pick-rand-by-dist scenario-distribution))


(defn- generate-job!
  [{:keys [employer-address bounty?]
    :or {bounty? false}}]

  (let [result-chan (chan 1)
        ipfs-data {:job/title "Full-Stack Software Developer" ;; TODO: randomize
                   :job/description "This is a job description."
                   :job/availability 0
                   :job/category "Software Development"
                   :job/skills (vec (random/rand-nth-n choice-collections/skills 2))}]
    (go
      (let [hash (<!-<throw (ipfs/add-edn! ipfs-data))
            job-index (job-factory/job-store-count)]
        (job-factory/create-job-store!
         {:bid-option (if-not bounty? ::enum.bid-option/hourly-rate ::enum.bid-option/bounty)
          :estimated-length-seconds (* 60 60 24 7 4) ;; 4 weeks
          :include-ether-token? true
          :is-invitation-only? false
          :metahash hash
          :reward-value (if-not bounty? 0 200000000)} ;; FIXME: to-eth fcn
         {:from employer-address})
        (>! result-chan {:job-index job-index})))
    result-chan))


(defn- generate-invoice!
  [{:keys [candidate-address
           employer-address
           paid?
           amount] :or {amount 200 paid? false}}]
  (let [result-chan (chan 1)
        invoice-index (work-contract/invoice-count)
        ipfs-data {}]
    (go
      (let [hash (<!-<throw (ipfs/add-edn! ipfs-data))]
        (work-contract/create-invoice! {:amount amount :metahash hash} {:from candidate-address})
        (when paid?
          (invoice/with-ethlance-invoice (work-contract/invoice-by-index invoice-index)
            (invoice/pay! amount {:from employer-address}))))
      (>! result-chan {:invoice-index invoice-index}))
    result-chan))


(defn generate-scenarios!
  "Generate different scenarios between the employers, candidates, and arbiters."
  [{:keys [num-scenarios employers candidates arbiters]
    :or {num-scenarios 5}
    :as scene-data}]
  (let [done-chan (chan 1)]
    (go-loop [i 1 scenario-name (pick-scenario)]
      (when (<= i num-scenarios)
        (<! (generate-scenario! (assoc scene-data
                                       :scenario-name scenario-name
                                       :scenario-number i)))
        (recur (inc i) (pick-scenario)))
      (>! done-chan ::done))
    done-chan))


;;
;; Scenarios
;;


(defmulti generate-scenario! (fn [scene-data] (get scene-data :scenario-name)))


(defmethod generate-scenario! :job-no-requests
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name))
  (let [done-chan (chan 1)
        employer-address (rand-nth employers)]
    (go
      (<! (generate-job! {:employer-address employer-address :bounty? false}))

      (>! done-chan ::done))
    done-chan))


(defmethod generate-scenario! :job-one-arbiter-request
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name))
  (let [done-chan (chan 1)
        employer-address (rand-nth employers)
        arbiter-address (rand-nth arbiters)]
    (go
      (let [{:keys [job-index]} (<! (generate-job! {:employer-address employer-address :bounty? false}))]
        (job/with-ethlance-job-store (job-factory/job-store-by-index job-index)
          (job/request-arbiter! arbiter-address {:from arbiter-address})))

      (>! done-chan ::done))
    done-chan))


(defmethod generate-scenario! :job-one-arbiter-accepted
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name))
  (let [done-chan (chan 1)
        employer-address (rand-nth employers)
        arbiter-address (rand-nth arbiters)]
    (go
      (let [{:keys [job-index]} (<! (generate-job! {:employer-address employer-address :bounty? false}))]
        (job/with-ethlance-job-store (job-factory/job-store-by-index job-index)
          (job/request-arbiter! arbiter-address {:from arbiter-address})
          (job/request-arbiter! arbiter-address {:from employer-address})))

      (>! done-chan ::done))
    done-chan))


(defmethod generate-scenario! :job-one-candidate-request
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name))
  (let [done-chan (chan 1)
        employer-address (rand-nth employers)
        candidate-address (rand-nth candidates)]
    (go
      (let [{:keys [job-index]} (<! (generate-job! {:employer-address employer-address :bounty? false}))]
        (job/with-ethlance-job-store (job-factory/job-store-by-index job-index)
          (job/request-work-contract! candidate-address {:from candidate-address})))

      (>! done-chan ::done))
    done-chan))


(defmethod generate-scenario! :job-one-candidate-accepted
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name))
  (let [done-chan (chan 1)
        employer-address (rand-nth employers)
        candidate-address (rand-nth candidates)]
    (go
      (let [{:keys [job-index]} (<! (generate-job! {:employer-address employer-address :bounty? false}))]
        (job/with-ethlance-job-store (job-factory/job-store-by-index job-index)
          (job/request-work-contract! candidate-address {:from candidate-address})
          (work-contract/with-ethlance-work-contract (job/work-contract-by-index 0)
            (work-contract/request-invite! {:from employer-address}))))

      (>! done-chan ::done))
    done-chan))


(defmethod generate-scenario! :job-1a-req-1c-req
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name))
  (let [done-chan (chan 1)
        employer-address (rand-nth employers)
        arbiter-address (rand-nth arbiters)
        candidate-address (rand-nth candidates)]
    (go
      (let [{:keys [job-index]} (<! (generate-job! {:employer-address employer-address :bounty? false}))]
        (job/with-ethlance-job-store (job-factory/job-store-by-index job-index)
          (job/request-arbiter! arbiter-address {:from arbiter-address})
          (job/request-work-contract! candidate-address {:from candidate-address})))

      (>! done-chan ::done))
    done-chan))


(defmethod generate-scenario! :job-2a-req-2c-req
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name))
  (let [done-chan (chan 1)
        employer-address (rand-nth employers)
        [arbiter-address arbiter-address-2] (random/rand-nth-n arbiters 2)
        [candidate-address candidate-address-2] (random/rand-nth-n candidates 2)]
    (go
      (let [{:keys [job-index]} (<! (generate-job! {:employer-address employer-address :bounty? false}))]
        (job/with-ethlance-job-store (job-factory/job-store-by-index job-index)
          (job/request-arbiter! arbiter-address {:from arbiter-address})
          (job/request-work-contract! candidate-address {:from candidate-address})
          (job/request-arbiter! arbiter-address-2 {:from arbiter-address-2})
          (job/request-work-contract! candidate-address-2 {:from candidate-address-2})))

      (>! done-chan ::done))
    done-chan))


(defmethod generate-scenario! :job-1a-acc-1c-acc
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name))
  (let [done-chan (chan 1)
        employer-address (rand-nth employers)
        arbiter-address (rand-nth arbiters)
        candidate-address (rand-nth candidates)]
    (go
      (let [{:keys [job-index]} (<! (generate-job! {:employer-address employer-address :bounty? false}))]
        (job/with-ethlance-job-store (job-factory/job-store-by-index job-index)
          (job/request-arbiter! arbiter-address {:from arbiter-address})
          (job/request-arbiter! arbiter-address {:from employer-address})
          (job/request-work-contract! candidate-address {:from candidate-address})
          (work-contract/with-ethlance-work-contract (job/work-contract-by-index 0)
            (work-contract/request-invite! {:from employer-address}))))

      (>! done-chan ::done))
    done-chan))


(defmethod generate-scenario! :job-2a-acc-2c-acc
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name))
  (let [done-chan (chan 1)
        employer-address (rand-nth employers)
        [arbiter-address arbiter-address-2] (random/rand-nth-n arbiters 2)
        [candidate-address candidate-address-2] (random/rand-nth-n candidates 2)]
    (go
      (let [{:keys [job-index]} (<! (generate-job! {:employer-address employer-address :bounty? false}))]
        (job/with-ethlance-job-store (job-factory/job-store-by-index job-index)
          (job/request-arbiter! arbiter-address {:from arbiter-address})
          (job/request-arbiter! arbiter-address-2 {:from arbiter-address-2})
          (job/request-arbiter! arbiter-address {:from employer-address})
          (job/request-work-contract! candidate-address {:from candidate-address})
          (job/request-work-contract! candidate-address-2 {:from candidate-address-2})
          (work-contract/with-ethlance-work-contract (job/work-contract-by-index 0)
            (work-contract/request-invite! {:from employer-address}))))

      (>! done-chan ::done))
    done-chan))


(defmethod generate-scenario! :job-prog-w-invoice
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name))
  (let [done-chan (chan 1)
        employer-address (rand-nth employers)
        arbiter-address (rand-nth arbiters)
        candidate-address (rand-nth candidates)]
    (go
      (let [{:keys [job-index]} (<! (generate-job! {:employer-address employer-address :bounty? false}))]
        (job/with-ethlance-job-store (job-factory/job-store-by-index job-index)
          (job/request-arbiter! arbiter-address {:from arbiter-address})
          (job/request-arbiter! arbiter-address {:from employer-address})
          (job/request-work-contract! candidate-address {:from candidate-address})
          (work-contract/with-ethlance-work-contract (job/work-contract-by-index 0)
            (work-contract/request-invite! {:from employer-address})
            (work-contract/proceed! {:from employer-address})
            (generate-invoice! {:candidate-address candidate-address
                                :employer-address employer-address
                                :paid? false}))))
 
      (>! done-chan ::done))
    done-chan))


(defmethod generate-scenario! :job-prog-w-invoice-paid
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name))
  (let [done-chan (chan 1)
        employer-address (rand-nth employers)
        arbiter-address (rand-nth arbiters)
        candidate-address (rand-nth candidates)]
    (go
      (let [{:keys [job-index]} (<! (generate-job! {:employer-address employer-address :bounty? false}))]
        (job/with-ethlance-job-store (job-factory/job-store-by-index job-index)
          (job/request-arbiter! arbiter-address {:from arbiter-address})
          (job/request-arbiter! arbiter-address {:from employer-address})
          (job/request-work-contract! candidate-address {:from candidate-address})
          (work-contract/with-ethlance-work-contract (job/work-contract-by-index 0)
            (work-contract/request-invite! {:from employer-address})
            (work-contract/proceed! {:from employer-address})
            (generate-invoice! {:candidate-address candidate-address
                                :employer-address employer-address
                                :paid? true}))))
 
      (>! done-chan ::done))
    done-chan))



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
