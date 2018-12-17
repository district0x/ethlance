(ns ethlance.server.generator
  "For development purposes, includes functions to generate employers,
  candidates, and arbiters, and simulate use-cases between the three
  parties in different states of jobs containing work contracts."
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
   [ethlance.server.deployer :refer [deployer]]
   [ethlance.server.generator.choice-collections :as choice-collections]
   [ethlance.server.generator.scenario :as scenario]))


(declare start stop)
(defstate ^{:on-reload :noop} generator
  :start (start)
  :stop (stop))


(defn testnet-max-accounts [] 10) ;; TODO: check config value


(defn get-registered-user-ipfs
  "Gets the EthlanceUser metahash data for the testnet account at `account-index`."
  [account-index]
   
  (let [eth-account (nth (web3-eth/accounts @web3) account-index)
        metahash-ipfs (user/with-ethlance-user (user-factory/user-by-address eth-account)
                        (user/metahash-ipfs))]
    (ipfs/get-edn metahash-ipfs)))


(defn generate-registered-users!
  "Generate registered users along with registering for candidate,
  employer, and arbiter.
  
  Note:

  - function is asynchronous, and returns a channel with the
    configuration values it has finished processing.
  "
  [{:keys [num-employers num-candidates num-arbiters]
    :or {num-employers 3 num-candidates 4 num-arbiters 3}}]
  (let [total-accounts (+ num-employers num-candidates num-arbiters)
        max-accounts (testnet-max-accounts)
        *user-emails (atom choice-collections/user-emails)
        *user-names (atom choice-collections/user-names)
        finished-chan (chan 1)]
    (assert (<= total-accounts max-accounts)
            (str "The number of total registrations exceeds the max number of testnet accounts: %d > %d"
                 total-accounts max-accounts))

    (go

      ;; Registering User Accounts
      (doseq [index (range total-accounts)]
        (let [eth-account (nth (web3-eth/accounts @web3) index)
              email (random/pluck! *user-emails)
              [full-name user-name] (random/pluck! *user-names)
              languages (vector (rand-nth choice-collections/languages))
              
              ipfs-data {:user/email email
                         :user/full-name full-name
                         :user/user-name user-name
                         :user/country-code "US"
                         :user/languages languages}
              metahash-ipfs (<!-<throw (ipfs/add-edn! ipfs-data))]
          (log/debug (str/format "Registering User #%s" (inc index)))
          (user-factory/register-user! {:metahash-ipfs metahash-ipfs} {:from eth-account})))

      ;; Registering Employers
      (doseq [employer-index (range num-employers)]
        (let [eth-account (nth (web3-eth/accounts @web3) employer-index)
              biography "I employ people" ;;TODO: generate random bios
              professional-title "Project Manager & Tech Lead" ;;TODO: generate title
              user-ipfs-data (<!-<throw (get-registered-user-ipfs employer-index))
              _ (log/debug (pr-str "User IPFS" user-ipfs-data))
              employer-ipfs-data {:employer/biography biography
                                  :employer/professional-title professional-title}
              _ (log/debug (pr-str "Joined IPFS" (merge user-ipfs-data employer-ipfs-data)))
              new-metahash (<!-<throw (ipfs/add-edn! (merge user-ipfs-data employer-ipfs-data)))]
          (log/debug (str/format "Registering User #%s as Employer..." (inc employer-index)))
          (user/with-ethlance-user (user-factory/user-by-address eth-account)
            (user/register-employer! {:from eth-account})
            (user/update-metahash! new-metahash {:from eth-account}))))

      ;; Registering Candidates
      (doseq [candidate-index (range num-employers (+ num-employers num-candidates))]
        (let [eth-account (nth (web3-eth/accounts @web3) candidate-index)
              biography "I like to work" ;;TODO: generate random bios
              professional-title "Software Consultant"
              categories (vector (rand-nth choice-collections/categories))
              skills (vector (rand-nth choice-collections/skills))
              user-ipfs-data (<!-<throw (get-registered-user-ipfs candidate-index))
              candidate-ipfs-data {:candidate/biography biography
                                   :candidate/professional-title professional-title
                                   :candidate/categories categories
                                   :candidate/skills skills}
              new-metahash (<!-<throw (ipfs/add-edn! (merge user-ipfs-data candidate-ipfs-data)))]
          (log/debug (str/format "Registering User #%s as Candidate..." (inc candidate-index)))
          (user/with-ethlance-user (user-factory/user-by-address eth-account)
            (user/register-candidate!
             {:hourly-rate 10 :currency-type ::enum.currency/eth} ;;TODO: randomize
             {:from eth-account})
            (user/update-metahash! new-metahash {:from eth-account}))))

      (doseq [arbiter-index (range (+ num-employers num-candidates) total-accounts)]
        (let [eth-account (nth (web3-eth/accounts @web3) arbiter-index)
              biography "I am fair."
              user-ipfs-data (<!-<throw (get-registered-user-ipfs arbiter-index))
              arbiter-ipfs-data {:arbiter/biography biography}
              new-metahash (<!-<throw (ipfs/add-edn! (merge user-ipfs-data arbiter-ipfs-data)))]
          (log/debug (str/format "Registering User #%s as Arbiter..." (inc arbiter-index)))
          (user/with-ethlance-user (user-factory/user-by-address eth-account)
            (user/register-arbiter!
             {:payment-value 5
              :currency-type ::enum.currency/eth
              :payment-type ::enum.payment/percentage}
             {:from eth-account})
            (user/update-metahash! new-metahash {:from eth-account}))))

      (let [accounts (web3-eth/accounts @web3)
            employers (take num-employers accounts)
            candidates (->> accounts (drop num-employers) (take num-candidates))
            arbiters (->> accounts (drop (+ num-employers num-candidates)) (take num-arbiters))]
        (>! finished-chan {:employers employers
                           :candidates candidates
                           :arbiters arbiters})))
    finished-chan))


;; TODO: pull in additional information from district.server.config
(defn generate! []
  (go
    (log/info "Started Generating Users and Scenarios...")
    (let [user-listing (<! (generate-registered-users! {}))]
      (<! (scenario/generate-scenarios! user-listing))
      (log/info "Finished Generating Users and Scenarios!"))))


(defn start
  [& config]
  ;; Wait on our deployer before we generate users
  @deployer
  
  (generate!))


(defn stop
  [])


