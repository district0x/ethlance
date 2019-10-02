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
   [ethlance.shared.enumeration.currency-type :as enum.currency]
   [ethlance.shared.enumeration.payment-type :as enum.payment]
   [ethlance.shared.enumeration.bid-option :as enum.bid-option]
   [ethlance.shared.enumeration.availability :as enum.availability]
   [ethlance.server.contract.ethlance-user :as user]
   [ethlance.server.contract.ethlance-user-factory :as user-factory]
   [ethlance.server.contract.ethlance-job-store :as job]
   [ethlance.server.contract.ethlance-job-factory :as job-factory]
   [ethlance.server.contract.ethlance-work-contract :as work-contract]
   [ethlance.server.contract.ethlance-invoice :as invoice]
   [ethlance.server.contract.ethlance-dispute :as dispute]
   [ethlance.server.generator.choice-collections :as choice-collections]))


(declare generate-scenario!)


(def scenario-distribution
  "A random distribution of different types of scenarios to choose from"
  [;;[0.7 :job-no-requests]
   ;;[0.2 :job-one-arbiter-request]
   ;;[0.4 :job-one-arbiter-accepted]
   ;;[0.2 :job-one-candidate-request]])
   ;;[0.4 :job-one-candidate-accepted]])
   ;;[0.5 :job-1a-req-1c-req]])
   ;;[0.5 :job-2a-req-2c-req]])
   ;;[1.2 :job-1a-acc-1c-acc]
   ;;[1.4 :job-2a-acc-2c-acc]])
   ;;[2.0 :job-prog-w-invoice]
   ;;[2.0 :job-prog-w-invoice-paid]])
   ;;[2.0 :job-prog-w-dispute]])
   ;;[2.0 :job-prog-w-dispute-resolved]])
   ;;[3.0 :job-prog-w-inv-disp]
   [3.0 :job-prog-w-inv-paid-disp-resolved]])


(defn pick-scenario
  "Pick a random scenario from the probability distribution"
  []
  (random/pick-rand-by-dist scenario-distribution))


(defn- generate-job!
  [{:keys [employer-address

           ;; Scenario Conditions
           arbiter-requests ;; collection of arbiters requesting invite
           arbiter-accepted   ;; the arbiter chosen for invite
           bounty?
           fund-amount]
    :or {bounty? false fund-amount (web3/to-wei 5.0 :ether)}}]

  (let [result-chan (chan 1)
        ipfs-data {:job/title "Full-Stack Software Developer" ;; TODO: randomize
                   :job/description "This is a job description."
                   :job/availability ::enum.availability/full-time
                   :job/category "Software Development"
                   :job/skills (vec (random/rand-nth-n choice-collections/skills 2))}]
    (go
     (let [hash (<!-<throw (ipfs/add-edn! ipfs-data))
           job-index (bn/number (<!-<throw (job-factory/job-store-count)))]
       (log/debug (str/format "- Creating Job Store [%s]" job-index))
       (<!-<throw
        (job-factory/create-job-store!
         {:bid-option (if-not bounty? ::enum.bid-option/hourly-rate ::enum.bid-option/bounty)
          :estimated-length-seconds (* 60 60 24 7) ;; 1 week
          :include-ether-token? true
          :is-invitation-only? false
          :metahash hash
          :reward-value (if-not bounty? 0 (web3/to-wei 5.0 :ether))}
         {:from employer-address}))
       (let [job-address (<!-<throw (job-factory/job-store-by-index job-index))]

         ;; Fund the Job Store if there are funds
         (when fund-amount
           (log/debug (str/format "-- Funding Job Store [%s] Amount: %s" job-index fund-amount))
           (<!-<throw (job/fund! job-address {:from employer-address :value fund-amount})))

         ;; Setup Arbiter Requests, if any
         (when arbiter-requests
           (log/debug (str/format "-- Requesting Arbiters..."))
           (doseq [arbiter-address arbiter-requests]
             (log/debug (str/format "--- Arbiter Request [%s]" arbiter-address))
             (<!-<throw (job/request-arbiter! job-address arbiter-address {:from arbiter-address})))
           
           ;; Accept the arbiter, if any
           (when arbiter-accepted
             (log/debug (str/format "--- Arbiter Accepted [%s]" arbiter-accepted))
             (assert (contains? (set arbiter-requests) arbiter-accepted)
                     "Arbiter Accepted address not in list of arbiter requests")
             (<!-<throw (job/request-arbiter! job-address arbiter-accepted {:from employer-address}))))
       
         (>! result-chan {:job-index job-index :job-address job-address}))))
    result-chan))


(defn- generate-work-contract!
  [{:keys [employer-address
           candidate-address
           job-address
           ;; Scenario Conditions
           candidate-accepted?]}]
  (let [result-chan (chan 1)]
    (go
     (let [work-index (<!-<throw (job/work-contract-count job-address))]
       (log/debug (str/format "-- Creating Work Contract [%s] for Candidate [%s]" work-index candidate-address))
       (<!-<throw (job/request-work-contract! job-address candidate-address {:from candidate-address}))
       (let [work-address (<!-<throw (job/work-contract-by-index job-address work-index))]
         (when candidate-accepted?
           (log/debug (str/format "--- Accepting Work Contract [%s] for Candidate [%s]" work-index candidate-address))
           (<!-<throw (work-contract/request-invite! work-address employer-address {:from employer-address})))
         (>! result-chan {:work-index work-index :work-address work-address}))))
    result-chan))


(defn- generate-invoice!
  "Generates an invoice, and will pay it if `paid?` is true
  
  Notes:

  - Should only be called within a `with-ethlance-job` context.
  
  "
  [{:keys [candidate-address
           employer-address
           job-address
           work-address
           paid?
           amount]
    :or {amount (web3/to-wei 0.1 :ether) paid? false}}]
  (let [result-chan (chan 1)
        ipfs-data {:comment/text "Here's my invoice!"}]
    (go
     (let [invoice-index (bn/number (<!-<throw (work-contract/invoice-count work-address)))
           hash (<!-<throw (ipfs/add-edn! ipfs-data))]
       (log/debug "-- Creating Invoice...")
       (<!-<throw (work-contract/create-invoice! work-address {:amount amount :metahash hash} {:from candidate-address}))
       (let [invoice-address (<!-<throw (work-contract/invoice-by-index work-address invoice-index))]
         (when paid?
           ;; Fund the amount to pay out for the invoice.
           (log/debug "-- Funding Job Address to pay invoice...")
           (<!-<throw 
            (job/fund!
             job-address
             {:from employer-address
              :value (web3/to-wei 1.0 :ether)}))

           (log/debug "-- Paying Invoice...")
           (<!-<throw (invoice/pay! invoice-address amount {:from employer-address})))

         (>! result-chan {:invoice-index invoice-index :invoice-address invoice-address}))))
    result-chan))


(defn- generate-dispute!
  [{:keys [candidate-address
           employer-address
           arbiter-address
           job-address
           work-address
           resolved?
           candidate-resolution-amount
           employer-resolution-amount
           arbiter-resolution-amount]
    :or {resolved? false
         candidate-resolution-amount (web3/to-wei 0.1 :ether)
         employer-resolution-amount (web3/to-wei 0.1 :ether)
         arbiter-resolution-amount (web3/to-wei 0.1 :ether)}}]
  (let [result-chan (chan 1)
        ipfs-data {:comment/text "Please read my dispute"}]
    (go
      (let [hash (<!-<throw (ipfs/add-edn! ipfs-data))
            feedback-hash (<!-<throw (ipfs/add-edn! {:feedback/text "Fair Judgement."}))]
        (log/debug "-- Creating Dispute...")
        (<!-<throw
         (work-contract/create-dispute!
          work-address
          {:reason "For being testy" :metahash hash}
          {:from employer-address}))

        (let [dispute-index (-> (work-contract/dispute-count work-address) <!-<throw bn/number dec)
              dispute-address (<!-<throw (work-contract/dispute-by-index work-address dispute-index))]
          (when resolved?
            ;; Fund the amount to pay out the dispute resolution
            (log/debug (str/format "-- Funding Job for Dispute[%s]" dispute-index))
            (<!-<throw
             (job/fund!
              job-address
              {:from employer-address
               :value (web3/to-wei 1.0 :ether)}))

            (log/debug (str/format "-- Resolving Dispute [%s]..." dispute-index))
            (<!-<throw
             (dispute/resolve!
              dispute-address
              {:employer-amount employer-resolution-amount
               :candidate-amount candidate-resolution-amount
               :arbiter-amount arbiter-resolution-amount}
              {:from arbiter-address}))

            (log/debug (str/format "-- Candidate [%s] is leaving Invoice Feedback..." candidate-address))
            (<!-<throw
             (dispute/leave-feedback! dispute-address 4 feedback-hash {:from candidate-address})))
          
          (>! result-chan {:dispute-index dispute-index :dispute-address dispute-address}))))
    result-chan))


(defn generate-scenarios!
  "Generate different scenarios between the employers, candidates, and arbiters."
  [{:keys [num-scenarios employers candidates arbiters]
    :or {num-scenarios 5}
    :as scene-data}]
  (let [done-chan (chan 1)]
    (log/debug "Generating Scenarios...")
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
     (let [job-scenario-options
           {:employer-address employer-address
            :arbiter-requests [arbiter-address]
            :bounty? false}]
       (<! (generate-job! job-scenario-options))))))


(defmethod generate-scenario! :job-one-arbiter-accepted
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name))
  (let [employer-address (rand-nth employers)
        arbiter-address (rand-nth arbiters)]
    (go-try
     (let [job-scenario-options
           {:employer-address employer-address
            :arbiter-requests [arbiter-address]
            :arbiter-accepted arbiter-address
            :bounty? false}]
       (<! (generate-job! job-scenario-options))))))


(defmethod generate-scenario! :job-one-candidate-request
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name))
  (let [employer-address (rand-nth employers)
        arbiter-address (rand-nth arbiters)
        candidate-address (rand-nth candidates)]
    (go-try
     (let [job-scenario-options
           {:employer-address employer-address
            :bounty? false}
           {:keys [job-index job-address]} (<! (generate-job! job-scenario-options))]
       (<! (generate-work-contract! {:employer-address employer-address
                                     :candidate-address candidate-address
                                     :job-address job-address}))))))


(defmethod generate-scenario! :job-one-candidate-accepted
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name))
  (let [employer-address (rand-nth employers)
        arbiter-address (rand-nth arbiters)
        candidate-address (rand-nth candidates)]
    (go-try
     (let [job-scenario-options
           {:employer-address employer-address
            :bounty? false}
           {:keys [job-index job-address]} (<! (generate-job! job-scenario-options))]
       (<! (generate-work-contract! {:employer-address employer-address
                                     :candidate-address candidate-address
                                     :candidate-accepted? true
                                     :job-address job-address}))))))


(defmethod generate-scenario! :job-1a-req-1c-req
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name))
  (let [employer-address (rand-nth employers)
        arbiter-address (rand-nth arbiters)
        candidate-address (rand-nth candidates)]
    (go-try
     (let [job-scenario-options
           {:employer-address employer-address
            :arbiter-requests [arbiter-address]
            :bounty? false}
           {:keys [job-index job-address]} (<! (generate-job! job-scenario-options))]
       (<! (generate-work-contract! {:employer-address employer-address
                                     :candidate-address candidate-address
                                     :candidate-accepted? false
                                     :job-address job-address}))))))


(defmethod generate-scenario! :job-2a-req-2c-req
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name))
  (let [employer-address (rand-nth employers)
        [arbiter-address arbiter-address-2] (random/rand-nth-n arbiters 2)
        [candidate-address candidate-address-2] (random/rand-nth-n candidates 2)]
    (go-try
     (let [job-scenario-options
           {:employer-address employer-address
            :arbiter-requests [arbiter-address arbiter-address-2]
            :bounty? false}
           {:keys [job-index job-address]} (<! (generate-job! job-scenario-options))]
       (<! (generate-work-contract! {:employer-address employer-address
                                     :candidate-address candidate-address
                                     :candidate-accepted? false
                                     :job-address job-address}))
       (<! (generate-work-contract! {:employer-address employer-address
                                     :candidate-address candidate-address-2
                                     :candidate-accepted? false
                                     :job-address job-address}))))))


(defmethod generate-scenario! :job-1a-acc-1c-acc
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name))
  (let [employer-address (rand-nth employers)
        arbiter-address (rand-nth arbiters)
        candidate-address (rand-nth candidates)]
    (go-try
     (let [job-scenario-options
           {:employer-address employer-address
            :arbiter-requests [arbiter-address]
            :arbiter-accepted arbiter-address
            :bounty? false}
           {:keys [job-index job-address]} (<! (generate-job! job-scenario-options))]
       (<! (generate-work-contract! {:employer-address employer-address
                                     :candidate-address candidate-address
                                     :candidate-accepted? true
                                     :job-address job-address}))))))


(defmethod generate-scenario! :job-2a-acc-2c-acc
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name))
  (let [employer-address (rand-nth employers)
        [arbiter-address arbiter-address-2] (random/rand-nth-n arbiters 2)
        [candidate-address candidate-address-2] (random/rand-nth-n candidates 2)]
    (go-try
     (let [job-scenario-options
           {:employer-address employer-address
            :arbiter-requests [arbiter-address arbiter-address-2]
            :arbiter-accepted arbiter-address
            :bounty? false}
           {:keys [job-index job-address]} (<! (generate-job! job-scenario-options))]
       (<! (generate-work-contract! {:employer-address employer-address
                                     :candidate-address candidate-address
                                     :candidate-accepted? true
                                     :job-address job-address}))
       (<! (generate-work-contract! {:employer-address employer-address
                                     :candidate-address candidate-address-2
                                     :candidate-accepted? true
                                     :job-address job-address}))))))


(defmethod generate-scenario! :job-prog-w-invoice
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name))
  (let [employer-address (rand-nth employers)
        arbiter-address (rand-nth arbiters)
        candidate-address (rand-nth candidates)]
    (go-try
     (let [job-scenario-options
           {:employer-address employer-address
            :arbiter-requests [arbiter-address]
            :arbiter-accepted arbiter-address
            :bounty? false}

           {:keys [job-address]} (<! (generate-job! job-scenario-options))

           work-scenario-options
           {:employer-address employer-address
            :candidate-address candidate-address
            :candidate-accepted? true
            :job-address job-address}

           {:keys [work-address]} (<! (generate-work-contract! work-scenario-options))]
       
       (let [feedback-hash (<!-<throw (ipfs/add-edn! {:feedback/text "Amazing job!"}))]
         (<!-<throw (work-contract/leave-feedback! work-address 4 feedback-hash {:from employer-address}))
         (<!-<throw (work-contract/proceed! work-address {:from employer-address}))
         (<! (generate-invoice! {:candidate-address candidate-address
                                 :employer-address employer-address
                                 :job-address job-address
                                 :work-address work-address
                                 :paid? false})))))))


(defmethod generate-scenario! :job-prog-w-invoice-paid
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name))
  (let [employer-address (rand-nth employers)
        arbiter-address (rand-nth arbiters)
        candidate-address (rand-nth candidates)]
    (go-try
     (let [job-scenario-options
           {:employer-address employer-address
            :arbiter-requests [arbiter-address]
            :arbiter-accepted arbiter-address
            :bounty? false}

           {:keys [job-address]} (<! (generate-job! job-scenario-options))

           work-scenario-options
           {:employer-address employer-address
            :candidate-address candidate-address
            :candidate-accepted? true
            :job-address job-address}

           {:keys [work-address]} (<! (generate-work-contract! work-scenario-options))]
       
       (let [feedback-hash (<!-<throw (ipfs/add-edn! {:feedback/text "Amazing job!"}))]
         (<!-<throw (work-contract/leave-feedback! work-address 4 feedback-hash {:from employer-address}))
         (<!-<throw (work-contract/proceed! work-address {:from employer-address}))
         (<! (generate-invoice! {:candidate-address candidate-address
                                 :employer-address employer-address
                                 :job-address job-address
                                 :work-address work-address
                                 :paid? true})))))))


(defmethod generate-scenario! :job-prog-w-dispute
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name))
  (let [employer-address (rand-nth employers)
        arbiter-address (rand-nth arbiters)
        candidate-address (rand-nth candidates)]
    (go-try
     (let [job-scenario-options
           {:employer-address employer-address
            :arbiter-requests [arbiter-address]
            :arbiter-accepted arbiter-address
            :bounty? false}

           {:keys [job-address]} (<! (generate-job! job-scenario-options))

           work-scenario-options
           {:employer-address employer-address
            :candidate-address candidate-address
            :candidate-accepted? true
            :job-address job-address}

           {:keys [work-address]} (<! (generate-work-contract! work-scenario-options))]
       
       (let [feedback-hash (<!-<throw (ipfs/add-edn! {:feedback/text "Amazing job!"}))]
         (<!-<throw (work-contract/leave-feedback! work-address 4 feedback-hash {:from employer-address}))
         (<!-<throw (work-contract/proceed! work-address {:from employer-address}))
         (<! (generate-dispute! {:employer-address employer-address
                                 :candidate-address candidate-address
                                 :arbiter-address arbiter-address
                                 :job-address job-address
                                 :work-address work-address})))))))


(defmethod generate-scenario! :job-prog-w-dispute-resolved
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name))
  (let [employer-address (rand-nth employers)
        arbiter-address (rand-nth arbiters)
        candidate-address (rand-nth candidates)]
    (go-try
     (let [job-scenario-options
           {:employer-address employer-address
            :arbiter-requests [arbiter-address]
            :arbiter-accepted arbiter-address
            :bounty? false}

           {:keys [job-address]} (<! (generate-job! job-scenario-options))

           work-scenario-options
           {:employer-address employer-address
            :candidate-address candidate-address
            :candidate-accepted? true
            :job-address job-address}

           {:keys [work-address]} (<! (generate-work-contract! work-scenario-options))]
       
       (let [feedback-hash (<!-<throw (ipfs/add-edn! {:feedback/text "Amazing job!"}))]
         (<!-<throw (work-contract/leave-feedback! work-address 4 feedback-hash {:from employer-address}))
         (<!-<throw (work-contract/proceed! work-address {:from employer-address}))
         (<! (generate-dispute! {:employer-address employer-address
                                 :candidate-address candidate-address
                                 :arbiter-address arbiter-address
                                 :job-address job-address
                                 :work-address work-address
                                 :resolved? true})))))))


(defmethod generate-scenario! :job-prog-w-inv-disp
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name))
  (let [employer-address (rand-nth employers)
        arbiter-address (rand-nth arbiters)
        candidate-address (rand-nth candidates)]
    (go-try
     (let [job-scenario-options
           {:employer-address employer-address
            :arbiter-requests [arbiter-address]
            :arbiter-accepted arbiter-address
            :bounty? false}

           {:keys [job-address]} (<! (generate-job! job-scenario-options))

           work-scenario-options
           {:employer-address employer-address
            :candidate-address candidate-address
            :candidate-accepted? true
            :job-address job-address}

           {:keys [work-address]} (<! (generate-work-contract! work-scenario-options))]
       
       (let [feedback-hash (<!-<throw (ipfs/add-edn! {:feedback/text "Amazing job!"}))]
         (<!-<throw (work-contract/leave-feedback! work-address 4 feedback-hash {:from employer-address}))
         (<!-<throw (work-contract/proceed! work-address {:from employer-address}))
         (<! (generate-invoice! {:candidate-address candidate-address
                                 :employer-address employer-address
                                 :job-address job-address
                                 :work-address work-address
                                 :paid? false}))
         (<! (generate-dispute! {:employer-address employer-address
                                 :candidate-address candidate-address
                                 :arbiter-address arbiter-address
                                 :job-address job-address
                                 :work-address work-address
                                 :resolved? false})))))))


(defmethod generate-scenario! :job-prog-w-inv-paid-disp-resolved
  [{:keys [employers candidates arbiters
           scenario-name scenario-number]}]
  (log/debug (str/format "Generating Scenario #%s - %s" scenario-number scenario-name))
  (let [employer-address (rand-nth employers)
        arbiter-address (rand-nth arbiters)
        candidate-address (rand-nth candidates)]
    (go-try
     (let [job-scenario-options
           {:employer-address employer-address
            :arbiter-requests [arbiter-address]
            :arbiter-accepted arbiter-address
            :bounty? false}

           {:keys [job-address]} (<! (generate-job! job-scenario-options))

           work-scenario-options
           {:employer-address employer-address
            :candidate-address candidate-address
            :candidate-accepted? true
            :job-address job-address}

           {:keys [work-address]} (<! (generate-work-contract! work-scenario-options))]
       
       (let [feedback-hash (<!-<throw (ipfs/add-edn! {:feedback/text "Amazing job!"}))]
         (<!-<throw (work-contract/leave-feedback! work-address 4 feedback-hash {:from employer-address}))
         (<!-<throw (work-contract/proceed! work-address {:from employer-address}))
         (<! (generate-invoice! {:candidate-address candidate-address
                                 :employer-address employer-address
                                 :job-address job-address
                                 :work-address work-address
                                 :paid? true}))
         (<! (generate-dispute! {:employer-address employer-address
                                 :candidate-address candidate-address
                                 :arbiter-address arbiter-address
                                 :job-address job-address
                                 :work-address work-address
                                 :resolved? true})))))))
