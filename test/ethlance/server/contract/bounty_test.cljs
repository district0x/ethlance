(ns ethlance.server.contract.bounty-test
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
   [ethlance.server.contract.token :as token]
   [ethlance.server.test-utils :refer [test-config] :refer-macros [deftest-smart-contract-go]]
   [clojure.string :as str]))

(def ethlance-standard-bounty-fulfillment-meta-1 "QmVDdxfDKyhqDk6uuiaL7LzG2RihPFo6FegQ1gLLugD34a") ;; ethlance-standard-bounty-fulfillment-meta-1.json
(def ethlance-standard-bounty-meta-1 "QmRGpWTDJ6CfpJwnsL8wP939G9bKz7tGr6U2gvchvFeGx8") ;; ethlance-standard-bounty-meta-1.json
(def gitcoin-standard-bounty-meta-1 "QmcjTwJeEpP4wFYRiZcv5w5vU5csafuJenp6aMggzHhQ29") ;; gitcoin-standard-bounty-meta-1.json

(defn gas-price
  [provider]
  (js-invoke (aget provider "eth") "getGasPrice"))

(deftest-smart-contract-go issue-bounty-test {}
  (let [hex  (partial web3-utils/to-hex @web3)
        [deployer] (<! (web3-eth/accounts @web3))
        ethlance-issuer-address (ethlance-issuer/test-ethlance-issuer-address)
        deadline 123123
        deposit 2e18]
    (testing "Issue with ETH"
      (let [token-version (ethlance-issuer/token-version :eth)
            token-address "0x0000000000000000000000000000000000000000"
            tx-receipt (<? (ethlance-issuer/issue-bounty ethlance-issuer-address
                                                         [ethlance-standard-bounty-meta-1
                                                          deadline
                                                          token-address
                                                          token-version
                                                          (hex deposit)]
                                                         {:from deployer
                                                          :value deposit}))
            ev (<! (ethlance-issuer/standard-bounties-event-in-tx :BountyIssued tx-receipt))]
        (is (:_bounty-id ev) "It should have a bounty id")
        (is (= (str/lower-case ethlance-issuer-address) (str/lower-case (:_creator ev))) "EthalnceBountyIssuer should be the creator")
        (is (= [(str/lower-case ethlance-issuer-address)]
               (mapv str/lower-case (:_issuers ev))) "EthalnceBountyIssuer should be the only issuer")
        (is (empty? (:_approvers ev)) "It should not have approvers")
        (is (= ethlance-standard-bounty-meta-1 (:_data ev)) "It should have the correct bounty data hash")))

    ;; TODO fix this, reverts with token
    ;; For some reason the token contracts shows ballance and allowance but when looking
    ;; at the contract with the debugger it looks like issue-and-contribute is creating a new contract
    ;; and it is empty, no idea what can be causing that
    ;; (testing "Issue with ERC20 token"
    ;;     (let [token-version (ethlance-issuer/token-version :erc20)
    ;;           token-address (token/test-token-address)
    ;;           _ (<? (token/mint! token-address deployer (hex deposit) {:from deployer}))
    ;;           _ (<? (token/approve! token-address ethlance-issuer-address (hex deposit) {:from deployer}))
    ;;           _ (prn "Balance of " deployer " is " (<? (token/balance-of token-address deployer)))
    ;;           _ (prn "Balance of " ethlance-issuer-address " is " (<? (token/balance-of token-address ethlance-issuer-address)))
    ;;           _ (prn "Allowance deployer -> ethlance-issuer-address" (<? (token/allowance token-address deployer ethlance-issuer-address)))
    ;;           tx-receipt (<? (ethlance-issuer/issue-and-contribute ethlance-issuer-address
    ;;                                                                [ethlance-standard-bounty-meta-1
    ;;                                                                 deadline
    ;;                                                                 token-address
    ;;                                                                 token-version
    ;;                                                                 (hex deposit)]
    ;;                                                                {:from deployer}))
    ;;           ev (<! (ethlance-issuer/standard-bounties-event-in-tx :BountyIssued tx-receipt))]

    ;;       (is (bn/number (:_bounty-id ev)) "It should have a bounty id")
    ;;       (is (= ethlance-issuer-address (:_creator ev)) "EthalnceBountyIssuer should be the creator")
    ;;       (is (= [ethlance-issuer-address] (:_issuers ev)) "EthalnceBountyIssuer should be the only issuer")
    ;;       (is (= [] (:_approvers ev)) "It should not have approvers")
    ;;       (is (= ethlance-standard-bounty-meta-1 (:_data ev)) "It should have the correct bounty data hash")
    ;;       (is (= token-address (:_token ev)))
    ;;       (is (= token-version (:_token-version ev)))))

    ))


(deftest-smart-contract-go arbiters-test {}
  (let [bn #(js/BigNumber. %)
        gp (<? (gas-price @web3))
        hex  (partial web3-utils/to-hex @web3)
        [deployer arbiter1 arbiter2 arbiter3] (<! (web3-eth/accounts @web3))
        arbiter1-prev-balance (bn (<? (web3-eth/get-balance @web3 arbiter1)))
        arbiter2-prev-balance (bn (<? (web3-eth/get-balance @web3 arbiter2)))
        ethlance-issuer-address (ethlance-issuer/test-ethlance-issuer-address)
        token-address "0x0000000000000000000000000000000000000000"
        token-version (ethlance-issuer/token-version :eth)
        deadline 123123
        arbiter-fee 1e18
        currency-id 1
        deposit 2e18
        tx-receipt (<? (ethlance-issuer/issue-bounty ethlance-issuer-address
                                                     [ethlance-standard-bounty-meta-1
                                                      deadline
                                                      token-address
                                                      token-version
                                                      (hex deposit)]
                                                     {:from deployer
                                                      :value deposit}))
        issued-bounty-id (-> (<! (ethlance-issuer/standard-bounties-event-in-tx :BountyIssued tx-receipt))
                             :_bounty-id bn/number)]

    (testing "Should be able to invite arbiters"
      (let [invite-rec (<? (ethlance-issuer/invite-arbiters ethlance-issuer-address
                                                            [[arbiter1 arbiter2]
                                                             (hex arbiter-fee)
                                                             currency-id
                                                             issued-bounty-id
                                                             (ethlance-issuer/job-type :standard-bounty)]
                                                            {:from deployer
                                                             :value (bn/* arbiter-fee 2)}))
            accept1-rec (<? (ethlance-issuer/accept-arbiter-invitation ethlance-issuer-address
                                                                       [(ethlance-issuer/job-type :standard-bounty)
                                                                        issued-bounty-id]
                                                                       {:from arbiter1}))
            accept2-rec (<? (ethlance-issuer/accept-arbiter-invitation ethlance-issuer-address
                                                                       [(ethlance-issuer/job-type :standard-bounty)
                                                                        issued-bounty-id]
                                                                       {:from arbiter2}))]
        (is (= [arbiter1] (->> (<? (ethlance-issuer/standard-bounties-event-in-tx :BountyApproversUpdated accept1-rec))
                               :_approvers
                               (into [])))
            "Should have arbiter1 as the only arbiter")
        (is (= [arbiter1 arbiter2] (->> (<? (ethlance-issuer/standard-bounties-event-in-tx :BountyApproversUpdated accept2-rec))
                                        :_approvers
                                        (into [])))
            "Should have arbiter1 and arbiter2 as arbiters")

        (is (bn/= (bn (<? (web3-eth/get-balance @web3 arbiter1)))
                  (bn/+ (bn/- arbiter1-prev-balance (bn/* (bn gp) (bn (:gas-used accept1-rec)))) (bn arbiter-fee)))
            "Arbiter 1 should have arbiter fee after accepting")
        (is (bn/= (bn (<? (web3-eth/get-balance @web3 arbiter2)))
                  (bn/+ (bn/- arbiter2-prev-balance (bn/* (bn gp) (bn (:gas-used accept2-rec)))) (bn arbiter-fee)))
            "Arbiter 2 should have arbiter fee after accepting")))))
