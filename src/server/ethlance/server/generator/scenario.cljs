(ns ethlance.server.generator.scenario
  (:require
   [bignumber.core :as bn]
   [cljs-ipfs-api.files :as ipfs-files]
   [cljs-web3.core :as web3]
   [cljs-web3.eth :as web3-eth]
   [cljs-web3.evm :as web3-evm]
   [clojure.core.async :as async :refer [go go-loop <! >! chan] :include-macros true]
   [cuerdas.core :as str]
   [district.cljs-utils :refer [rand-str]]
   [district.format :as format]
   [district.server.config :refer [config]]
   [district.server.smart-contracts :as smart-contracts]
   [district.server.web3 :refer [web3]]
   [district.shared.error-handling :refer [try-catch try-catch-throw]]
   [mount.core :as mount :refer [defstate]]
   [taoensso.timbre :as log]

   ;; Ethlance NS
   [ethlance.server.ipfs :as ipfs]
   [ethlance.server.filesystem :as filesystem]
   [ethlance.shared.random :as random]
   [ethlance.shared.async-utils :refer [<!-<log <!-<throw go-try] :include-macros true]
   [ethlance.shared.enum.currency-type :as enum.currency]
   [ethlance.shared.enum.payment-type :as enum.payment]
   [ethlance.shared.enum.bid-option :as enum.bid-option]
   [ethlance.shared.enum.availability :as enum.availability]
   [ethlance.server.contract.ethlance-user :as user :include-macros true]
   [ethlance.server.contract.ethlance-user-factory :as user-factory]
   [ethlance.server.contract.ethlance-job-store :as job :include-macros true]
   [ethlance.server.contract.ethlance-job-factory :as job-factory]
   [ethlance.server.contract.ethlance-work-contract :as work-contract :include-macros true]
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
   [2.0 :job-prog-w-invoice]
   [2.0 :job-prog-w-invoice-paid]
   [2.0 :job-prog-w-dispute]
   [2.0 :job-prog-w-dispute-resolved]
   [3.0 :job-prog-w-inv-disp]
   [3.0 :job-prog-w-inv-paid-disp-resolved]])


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
                   :job/availability ::enum.availability/full-time
                   :job/category "Software Development"
                   :job/skills (vec (random/rand-nth-n choice-collections/skills 2))}]
    (go-try
     (let [hash (<!-<throw (ipfs/add-edn! ipfs-data))
           job-index (job-factory/job-store-count)]
       (job-factory/create-job-store!
        {:bid-option (if-not bounty? ::enum.bid-option/hourly-rate ::enum.bid-option/bounty)
         :estimated-length-seconds (* 60 60 24 7) ;; 1 week
         :include-ether-token? true
         :is-invitation-only? false
         :metahash hash
         :reward-value (if-not bounty? 0 (web3/to-wei 5.0 :ether))}
        {:from employer-address})
       (>! result-chan {:job-index job-index})))
    result-chan))


(defn- generate-invoice!
  [{:keys [candidate-address
           employer-address
           paid?
           amount]
    :or {amount (web3/to-wei 0.1 :ether) paid? false}}]
  (let [result-chan (chan 1)
        invoice-index (work-contract/invoice-count)
        ipfs-data {}]
    (go-try
     (let [hash (<!-<throw (ipfs/add-edn! ipfs-data))]
       (log/debug "- Creating Invoice...")
       (work-contract/create-invoice! {:amount amount :metahash hash} {:from candidate-address})
       (when paid?
         ;; Fund the amount to pay out for the invoice.
         (job/fund! {:from employer-address :value (web3/to-wei 1.0 :ether)})
         (invoice/with-ethlance-invoice (work-contract/invoice-by-index invoice-index)
           (log/debug "- Paying Invoice...")
           (invoice/pay! amount {:from employer-address})))
       (>! result-chan {:invoice-index invoice-index})))
    result-chan))


(defn- generate-dispute!
  [{:keys [candidate-address
           employer-address
           arbiter-address
           resolved?
           candidate-resolution-amount
           employer-resolution-amount
           arbiter-resolution-amount]
    :or {resolved? false
         candidate-resolution-amount (web3/to-wei 0.1 :ether)
         employer-resolution-amount (web3/to-wei 0.1 :ether)
         arbiter-resolution-amount (web3/to-wei 0.1 :ether)}}]
  (let [result-chan (chan 1)
        dispute-index (work-contract/dispute-count)
        ipfs-data {}]
    (go-try
     (let [hash (<!-<throw (ipfs/add-edn! ipfs-data))]
       (log/debug "- Creating Dispute...")
       (work-contract/create-dispute!
        {:reason "For being testy" :metahash hash}
        {:from employer-address})
       (when resolved?
         ;; Fund the amount to pay out the dispute resolution
         (job/fund!
          {:from employer-address
           :value (web3/to-wei 1.0 :ether)})
         (dispute/with-ethlance-dispute (work-contract/dispute-by-index dispute-index)
           (dispute/resolve!
            {:employer-amount employer-resolution-amount
             :candidate-amount candidate-resolution-amount
             :arbiter-amount arbiter-resolution-amount}
            {:from arbiter-address}))))
     (>! result-chan {:dispute-index dispute-index}))
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
  (let [employer-address (rand-nth employers)]
    (go-try
     (<! (generate-job! {:employer-address employer-address :bounty? false})))))


(defmethod generate-scenario! :job-one-arbiter-request
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name))
  (let [employer-address (rand-nth employers)
        arbiter-address (rand-nth arbiters)]
    (go-try
     (let [{:keys [job-index]} (<! (generate-job! {:employer-address employer-address :bounty? false}))]
       (job/with-ethlance-job-store (job-factory/job-store-by-index job-index)
         (job/request-arbiter! arbiter-address {:from arbiter-address}))))))


(defmethod generate-scenario! :job-one-arbiter-accepted
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name))
  (let [employer-address (rand-nth employers)
        arbiter-address (rand-nth arbiters)]
    (go-try
     (let [{:keys [job-index]} (<! (generate-job! {:employer-address employer-address :bounty? false}))]
       (job/with-ethlance-job-store (job-factory/job-store-by-index job-index)
         (job/request-arbiter! arbiter-address {:from arbiter-address})
         (job/request-arbiter! arbiter-address {:from employer-address}))))))


(defmethod generate-scenario! :job-one-candidate-request
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name))
  (let [employer-address (rand-nth employers)
        candidate-address (rand-nth candidates)]
    (go-try
     (let [{:keys [job-index]} (<! (generate-job! {:employer-address employer-address :bounty? false}))]
       (job/with-ethlance-job-store (job-factory/job-store-by-index job-index)
         (job/request-work-contract! candidate-address {:from candidate-address}))))))


(defmethod generate-scenario! :job-one-candidate-accepted
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name))
  (let [employer-address (rand-nth employers)
        candidate-address (rand-nth candidates)]
    (go-try
     (let [{:keys [job-index]} (<! (generate-job! {:employer-address employer-address :bounty? false}))]
       (job/with-ethlance-job-store (job-factory/job-store-by-index job-index)
         (job/request-work-contract! candidate-address {:from candidate-address})
         (work-contract/with-ethlance-work-contract (job/work-contract-by-index 0)
           (work-contract/request-invite! {:from employer-address})))))))


(defmethod generate-scenario! :job-1a-req-1c-req
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name))
  (let [employer-address (rand-nth employers)
        arbiter-address (rand-nth arbiters)
        candidate-address (rand-nth candidates)]
    (go-try
     (let [{:keys [job-index]} (<! (generate-job! {:employer-address employer-address :bounty? false}))]
       (job/with-ethlance-job-store (job-factory/job-store-by-index job-index)
         (job/request-arbiter! arbiter-address {:from arbiter-address})
         (job/request-work-contract! candidate-address {:from candidate-address}))))))


(defmethod generate-scenario! :job-2a-req-2c-req
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name))
  (let [employer-address (rand-nth employers)
        [arbiter-address arbiter-address-2] (random/rand-nth-n arbiters 2)
        [candidate-address candidate-address-2] (random/rand-nth-n candidates 2)]
    (go-try
     (let [{:keys [job-index]} (<! (generate-job! {:employer-address employer-address :bounty? false}))]
       (job/with-ethlance-job-store (job-factory/job-store-by-index job-index)
         (job/request-arbiter! arbiter-address {:from arbiter-address})
         (job/request-work-contract! candidate-address {:from candidate-address})
         (job/request-arbiter! arbiter-address-2 {:from arbiter-address-2})
         (job/request-work-contract! candidate-address-2 {:from candidate-address-2}))))))


(defmethod generate-scenario! :job-1a-acc-1c-acc
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name))
  (let [employer-address (rand-nth employers)
        arbiter-address (rand-nth arbiters)
        candidate-address (rand-nth candidates)]
    (go-try
     (let [{:keys [job-index]} (<! (generate-job! {:employer-address employer-address :bounty? false}))]
       (job/with-ethlance-job-store (job-factory/job-store-by-index job-index)
         (job/request-arbiter! arbiter-address {:from arbiter-address})
         (job/request-arbiter! arbiter-address {:from employer-address})
         (job/request-work-contract! candidate-address {:from candidate-address})
         (work-contract/with-ethlance-work-contract (job/work-contract-by-index 0)
           (work-contract/request-invite! {:from employer-address})))))))


(defmethod generate-scenario! :job-2a-acc-2c-acc
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name))
  (let [employer-address (rand-nth employers)
        [arbiter-address arbiter-address-2] (random/rand-nth-n arbiters 2)
        [candidate-address candidate-address-2] (random/rand-nth-n candidates 2)]
    (go-try
     (let [{:keys [job-index]} (<! (generate-job! {:employer-address employer-address :bounty? false}))]
       (job/with-ethlance-job-store (job-factory/job-store-by-index job-index)
         (job/request-arbiter! arbiter-address {:from arbiter-address})
         (job/request-arbiter! arbiter-address-2 {:from arbiter-address-2})
         (job/request-arbiter! arbiter-address {:from employer-address})
         (job/request-work-contract! candidate-address {:from candidate-address})
         (job/request-work-contract! candidate-address-2 {:from candidate-address-2})
         (work-contract/with-ethlance-work-contract (job/work-contract-by-index 0)
           (work-contract/request-invite! {:from employer-address})))))))


(defmethod generate-scenario! :job-prog-w-invoice
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name))
  (let [employer-address (rand-nth employers)
        arbiter-address (rand-nth arbiters)
        candidate-address (rand-nth candidates)]
    (go-try
     (let [{:keys [job-index]} (<! (generate-job! {:employer-address employer-address :bounty? false}))]
       (job/with-ethlance-job-store (job-factory/job-store-by-index job-index)
         (job/request-arbiter! arbiter-address {:from arbiter-address})
         (job/request-arbiter! arbiter-address {:from employer-address})
         (job/request-work-contract! candidate-address {:from candidate-address})
         (work-contract/with-ethlance-work-contract (job/work-contract-by-index 0)
           (work-contract/request-invite! {:from employer-address})
           (work-contract/proceed! {:from employer-address})
           (<! (generate-invoice! {:candidate-address candidate-address
                                   :employer-address employer-address
                                   :paid? false}))))))))


(defmethod generate-scenario! :job-prog-w-invoice-paid
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name))
  (let [employer-address (rand-nth employers)
        arbiter-address (rand-nth arbiters)
        candidate-address (rand-nth candidates)]
    (go-try
     (let [{:keys [job-index]} (<! (generate-job! {:employer-address employer-address :bounty? false}))]
       (job/with-ethlance-job-store (job-factory/job-store-by-index job-index)
         (job/request-arbiter! arbiter-address {:from arbiter-address})
         (job/request-arbiter! arbiter-address {:from employer-address})
         (job/request-work-contract! candidate-address {:from candidate-address})
         (work-contract/with-ethlance-work-contract (job/work-contract-by-index (dec (job/work-contract-count)))
           (work-contract/request-invite! {:from employer-address})
           (work-contract/proceed! {:from employer-address})
           (<! (generate-invoice! {:candidate-address candidate-address
                                   :employer-address employer-address
                                   :paid? true}))))))))


(defmethod generate-scenario! :job-prog-w-dispute
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name))
  (let [employer-address (rand-nth employers)
        arbiter-address (rand-nth arbiters)
        candidate-address (rand-nth candidates)]
    (go-try
     (let [{:keys [job-index]} (<! (generate-job! {:employer-address employer-address :bounty? false}))]
       (job/with-ethlance-job-store (job-factory/job-store-by-index job-index)
         (job/request-arbiter! arbiter-address {:from arbiter-address})
         (job/request-arbiter! arbiter-address {:from employer-address})
         (job/request-work-contract! candidate-address {:from candidate-address})
         (work-contract/with-ethlance-work-contract (job/work-contract-by-index 0)
           (work-contract/request-invite! {:from employer-address})
           (work-contract/proceed! {:from employer-address})
           (<! (generate-dispute! {:employer-address employer-address
                                   :candidate-address candidate-address
                                   :arbiter-address arbiter-address}))))))))


(defmethod generate-scenario! :job-prog-w-dispute-resolved
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name))
  (let [employer-address (rand-nth employers)
        arbiter-address (rand-nth arbiters)
        candidate-address (rand-nth candidates)]
    (go-try
     (let [{:keys [job-index]} (<! (generate-job! {:employer-address employer-address :bounty? false}))]
       (job/with-ethlance-job-store (job-factory/job-store-by-index job-index)
         (job/request-arbiter! arbiter-address {:from arbiter-address})
         (job/request-arbiter! arbiter-address {:from employer-address})
         (job/request-work-contract! candidate-address {:from candidate-address})
         (work-contract/with-ethlance-work-contract (job/work-contract-by-index 0)
           (work-contract/request-invite! {:from employer-address})
           (work-contract/proceed! {:from employer-address})
           (<! (generate-dispute! {:employer-address employer-address
                                   :candidate-address candidate-address
                                   :arbiter-address arbiter-address
                                   :resolved? true}))))))))


(defmethod generate-scenario! :job-prog-w-inv-disp
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name))
  (let [employer-address (rand-nth employers)
        arbiter-address (rand-nth arbiters)
        candidate-address (rand-nth candidates)]
    (go-try
     (let [{:keys [job-index]} (<! (generate-job! {:employer-address employer-address :bounty? false}))]
       (job/with-ethlance-job-store (job-factory/job-store-by-index job-index)
         (job/request-arbiter! arbiter-address {:from arbiter-address})
         (job/request-arbiter! arbiter-address {:from employer-address})
         (job/request-work-contract! candidate-address {:from candidate-address})
         (work-contract/with-ethlance-work-contract (job/work-contract-by-index 0)
           (work-contract/request-invite! {:from employer-address})
           (work-contract/proceed! {:from employer-address})
           (<! (generate-invoice! {:candidate-address candidate-address
                                   :employer-address employer-address
                                   :paid? false}))
           (<! (generate-dispute! {:employer-address employer-address
                                   :candidate-address candidate-address
                                   :arbiter-address arbiter-address
                                   :resolved? false}))))))))


(defmethod generate-scenario! :job-prog-w-inv-paid-disp-resolved
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name))
  (let [employer-address (rand-nth employers)
        arbiter-address (rand-nth arbiters)
        candidate-address (rand-nth candidates)]
    (go-try
     (let [{:keys [job-index]} (<! (generate-job! {:employer-address employer-address :bounty? false}))]
       (job/with-ethlance-job-store (job-factory/job-store-by-index job-index)
         (job/request-arbiter! arbiter-address {:from arbiter-address})
         (job/request-arbiter! arbiter-address {:from employer-address})
         (job/request-work-contract! candidate-address {:from candidate-address})
         (work-contract/with-ethlance-work-contract (job/work-contract-by-index 0)
           (work-contract/request-invite! {:from employer-address})
           (work-contract/proceed! {:from employer-address})
           (<! (generate-invoice! {:candidate-address candidate-address
                                   :employer-address employer-address
                                   :paid? true}))
           (<! (generate-dispute! {:employer-address employer-address
                                   :candidate-address candidate-address
                                   :arbiter-address arbiter-address
                                   :resolved? true}))))))))
