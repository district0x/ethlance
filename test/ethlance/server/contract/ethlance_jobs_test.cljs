(ns ethlance.server.contract.ethlance-jobs-test
  (:require
   [bignumber.core :as bn]
   cljsjs.bignumber
   [clojure.test :refer [deftest is are testing use-fixtures]]
   [cljs-web3-next.eth :as web3-eth]
   [cljs-web3-next.utils :as web3-utils]
   [taoensso.timbre :as log]
   [clojure.core.async :as async :refer [go go-loop <! >! chan] :include-macros true]
   [district.shared.async-helpers :refer [<?]]
   [district.server.web3 :refer [web3]]
   [district.server.smart-contracts :as contracts]
   [ethlance.server.contract.ethlance-issuer :as ethlance-issuer]
   [ethlance.server.contract.ethlance-jobs :as ethlance-jobs]
   [ethlance.server.contract.token :as token]
   [ethlance.server.test-utils :refer [test-config] :refer-macros [deftest-smart-contract-go]]
   [clojure.string :as str]))

(def jobs-data-hash "hash")

(defn gas-price
  [provider]
  (js-invoke (aget provider "eth") "getGasPrice"))

;; What to test for jobs
;; - We can issue a Job
;; - Someone can apply as candidate
;; - Issuer can accept the candidate
;; - Only accepted candidates can invoice

(deftest-smart-contract-go issue-jobs-test {}
  (let [hex  (partial web3-utils/to-hex @web3)
        [issuer] (<! (web3-eth/accounts @web3))
        ethlance-issuer-address (ethlance-issuer/test-ethlance-issuer-address)
        deposit 2e18]
    (testing "Issue with ETH"
      (let [token-version (ethlance-issuer/token-version :eth)
            token-address "0x0000000000000000000000000000000000000000"
            tx-receipt (<? (ethlance-issuer/issue-job ethlance-issuer-address
                                                      [jobs-data-hash
                                                       token-address
                                                       token-version
                                                       (hex deposit)]
                                                      {:from issuer
                                                       :value deposit}))
            ev (<! (ethlance-issuer/ethlance-jobs-event-in-tx :JobIssued tx-receipt))]
        (is (:_job-id ev) "It should have a job id")
        (is (= (str/lower-case ethlance-issuer-address) (str/lower-case (:_creator ev))) "EthalnceIssuer should be the creator")
        (is (= [(str/lower-case ethlance-issuer-address)]
               (mapv str/lower-case (:_issuers ev))) "EthalnceIssuer should be the only issuer")
        (is (empty? (:_approvers ev)) "It should not have approvers")
        (is (= jobs-data-hash (:_ipfs-hash ev)) "It should have the correct bounty data hash")))))


(deftest-smart-contract-go candidates-test {}
  (let [hex  (partial web3-utils/to-hex @web3)
        [issuer candidate1 candidate2] (<! (web3-eth/accounts @web3))
        ethlance-issuer-address (ethlance-issuer/test-ethlance-issuer-address)
        ethlance-jobs-address (ethlance-issuer/test-ethlance-jobs-address)
        deposit 2e18
        token-version (ethlance-issuer/token-version :eth)
        token-address "0x0000000000000000000000000000000000000000"
        tx-receipt (<? (ethlance-issuer/issue-job ethlance-issuer-address
                                                  [jobs-data-hash
                                                   token-address
                                                   token-version
                                                   (hex deposit)]
                                                  {:from issuer
                                                   :value deposit}))
        ev (<! (ethlance-issuer/ethlance-jobs-event-in-tx :JobIssued tx-receipt))
        job-id (:_job-id ev)

        ;; Both apply as candidate
        aac1-tx-receipt (<? (ethlance-jobs/apply-as-candidate ethlance-jobs-address
                                                              [job-id
                                                               candidate1]
                                                              {:from candidate1}))
        aac2-tx-receipt (<? (ethlance-jobs/apply-as-candidate ethlance-jobs-address
                                                              [job-id
                                                               candidate2]
                                                              {:from candidate1}))
        ;; but only candidate1 gets accepted
        accept-tx-receipt (<? (ethlance-jobs/accept-candidate ethlance-jobs-address
                                                              [job-id
                                                               candidate1]
                                                              {:from issuer}))

        ;; since candidate1 was accepted should be able to invoice the job
        in1-tx-receipt (<? (ethlance-jobs/invoice-job ethlance-jobs-address
                                                      [candidate1
                                                       job-id
                                                       candidate1
                                                       jobs-data-hash
                                                       (hex 10)]
                                                      {:from candidate1}))


        ;; since candidate2 was NOT accepted it should NOT be able to invoice the job
        in2-tx-receipt (<? (ethlance-jobs/invoice-job ethlance-jobs-address
                                                      [candidate2
                                                       job-id
                                                       [candidate2]
                                                       jobs-data-hash
                                                       (hex 10)]
                                                      {:from candidate2}))
        ]
    (testing "Candidates can apply to the job"
      (let [ev (<! (ethlance-issuer/ethlance-jobs-event-in-tx :CandidateApplied aac1-tx-receipt))]
        (is (= (:job-id ev) job-id))
        (is (= (:candidate ev) candidate1))))

    (testing "Accept candidate for the job"
      (let [ev (<! (ethlance-issuer/ethlance-jobs-event-in-tx :CandidateAccepted accept-tx-receipt))]
        (is (= (:job-id ev) job-id))
        (is (= (:candidate ev) candidate1))))

    (testing "Only accepted candidates can invoice a job"
      (let [ev (<! (ethlance-issuer/ethlance-jobs-event-in-tx :JobInvoice in1-tx-receipt))]
        (is (= (:_job-id ev) job-id))
        (is (= (js->clj (:_invoice-issuer ev)) candidate1))
        (is (= (:_ipfs-hash ev) jobs-data-hash))
        (is (= (:_submitter ev) candidate1))
        (is (= (:_amount ev) "10")))

      (is (nil? in2-tx-receipt)))))
