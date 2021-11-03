(ns tests.contract.ethlance-test
  (:require [bignumber.core :as bn]
            [cljs-web3-next.eth :as web3-eth]
            [cljs.test :refer-macros [deftest is testing async]]
            [district.server.web3 :refer [web3]]
            [ethlance.server.contract.ethlance :as ethlance]
            [ethlance.shared.contract-constants :as contract-constants]
            [ethlance.shared.smart-contracts-dev :as addresses]
            [district.server.smart-contracts :as smart-contracts]
            [cljs.core.async :refer [<! go]]
            [district.shared.async-helpers :refer [<?]]))

(defn is-same-amount [amount-a amount-b & more]
  (let [description (first more)]
    (if (nil? description)
     (is (= (bn/number amount-a) (bn/number amount-b)))
     (is (= (bn/number amount-a) (bn/number amount-b)) description))))

(defn- positive-int-upto [n]
  (+ 1 (rand-int (- n 1))))

(deftest ethlance-erc20-payment
  (testing "Creating job paid with ERC20 token"
    (async done
           (go
             (let [ethlance-addr (smart-contracts/contract-address :ethlance)
                   [_owner employer _worker] (<! (web3-eth/accounts @web3))
                   initial-balance (<? (smart-contracts/contract-call :token :balance-of [employer]))
                   funding-amount 5  ; How much employer gets added to begin with
                   to-approve-amount (positive-int-upto 5) ; How many tokens employer approves to be used taken by Ethlance
                   job-type 1
                   arbiters []
                   ipfs-data "0x0"
                   _ (<? (smart-contracts/contract-send :token :mint [employer funding-amount]))
                   expected-employer-funds-before (bn/+ (bn/number initial-balance) funding-amount)
                   employer-token-balance-before (<? (smart-contracts/contract-call :token :balance-of [employer]))
                   job-proxy-address (get-in addresses/smart-contracts [:job :address])
                   test-token-address (smart-contracts/contract-address :token)
                   not-used-for-erc20 0
                   offered-token-type (contract-constants/token-type :erc20)
                   offered-value {:token
                                  {:tokenContract {:tokenType offered-token-type :tokenAddress test-token-address}
                                   :tokenId not-used-for-erc20} :value to-approve-amount}]
               (is-same-amount employer-token-balance-before expected-employer-funds-before)
               (<! (smart-contracts/contract-send :token :approve [ethlance-addr to-approve-amount] {:from employer}))
               (<! (ethlance/initialize job-proxy-address))

               (let [tx-receipt (<! (ethlance/create-job employer [offered-value] job-type arbiters ipfs-data))
                     create-job-event (<! (smart-contracts/contract-event-in-tx :ethlance :JobCreated tx-receipt))
                     created-job-address (:job create-job-event)
                     job-proxy-balance (<? (smart-contracts/contract-call :token :balance-of [created-job-address]))]
                 (is-same-amount job-proxy-balance to-approve-amount "tokens held in proxy must >= approved")
                 (done)))))))

(deftest ethlance-eth-payment
  (testing "Paying with ETH for a Ethlance job"
    (async done
           (go
             (let [[_owner employer _worker] (<! (web3-eth/accounts @web3))
                   payment-in-wei (str (* (positive-int-upto 5) 10000000000000000)) ; 0.01..0.05 ETH
                   job-type (contract-constants/job-type :gig)
                   arbiters []
                   ipfs-data "0x0"
                   job-proxy-address (get-in addresses/smart-contracts [:job :address])
                   not-used-for-erc20 0
                   offered-token-type (contract-constants/token-type :eth)
                   placeholder-address "0x1111111111111111111111111111111111111111"
                   offered-value {:token
                                  {:tokenContract {:tokenType offered-token-type :tokenAddress placeholder-address}
                                   :tokenId not-used-for-erc20} :value payment-in-wei}]
               (<! (ethlance/initialize job-proxy-address))

              (let [tx-receipt (<! (ethlance/create-job employer [offered-value] job-type arbiters ipfs-data {:value payment-in-wei}))
                     create-job-event (<! (smart-contracts/contract-event-in-tx :ethlance :JobCreated tx-receipt))
                     created-job-address (:job create-job-event)
                     job-proxy-eth-balance (<? (web3-eth/get-balance @web3 created-job-address))]
                 (is-same-amount job-proxy-eth-balance payment-in-wei "all offered ETH must end up in the created job proxy contract")
                 (done)))))))

(deftest ethlance-erc721-payment
  (testing "Paying in NFT (ERC721)"
    (async done
     (go
       (let [ethlance-addr (smart-contracts/contract-address :ethlance)
            [_owner employer _worker] (<! (web3-eth/accounts @web3))
             receipt (<! (smart-contracts/contract-send :test-nft :award-item [employer])) ; Give him 1st token
             token-id (. (get-in receipt [:events :Transfer :return-values]) -tokenId)
             job-type 1
             arbiters []
             ipfs-data "0x0"
             job-proxy-address (get-in addresses/smart-contracts [:job :address])
             _ (<! (ethlance/initialize job-proxy-address))
             test-token-address (smart-contracts/contract-address :test-nft)
             offered-token-type (contract-constants/token-type :erc721)
             offered-value {:token
                            {:tokenContract {:tokenType offered-token-type :tokenAddress test-token-address}
                             :tokenId token-id} :value 1}
             operation-type (contract-constants/operation-type :one-step-job-creation)
             ; TODO: Write clojure.spec for the call-data structure
             call-data (web3-eth/encode-abi (smart-contracts/instance :ethlance)
                                                 :transfer-callback-delegate
                                                 [operation-type employer [offered-value] job-type arbiters ipfs-data])
             transfer-receipt (<! (smart-contracts/contract-send :test-nft :safe-transfer-from [employer ethlance-addr token-id call-data] {:from employer}))
             job-created-event (<! (smart-contracts/contract-event-in-tx :ethlance :JobCreated transfer-receipt))
             created-job (:job job-created-event)
             token-owner (<! (smart-contracts/contract-call :test-nft :owner-of [token-id]))]

         (is (= created-job token-owner))
         (done))))))

(deftest ethlance-erc1155-payment
  (testing "Paying in multi-token (ERC1155)"
    (async done
     (go
       (let [ethlance-addr (smart-contracts/contract-address :ethlance)
            [_owner employer _worker] (<! (web3-eth/accounts @web3))
             receipt (<? (smart-contracts/contract-send :test-multi-token :award-item [employer 7])) ; Give him 1st token
             token-id (. (get-in receipt [:events :Transfer-single :return-values]) -id)
             job-type 1
             arbiters []
             ipfs-data "0x0"
             job-proxy-address (get-in addresses/smart-contracts [:job :address])
             _ (<! (ethlance/initialize job-proxy-address))
             test-token-address (smart-contracts/contract-address :test-multi-token)
             offered-token-type (contract-constants/token-type :erc1155)
             operation-type (contract-constants/operation-type :one-step-job-creation)
             sent-amount 3
             offered-value {:token
                            {:tokenContract {:tokenType offered-token-type :tokenAddress test-token-address}
                             :tokenId token-id}
                            :value sent-amount}
             call-data (web3-eth/encode-abi (smart-contracts/instance :ethlance)
                                                 :transfer-callback-delegate
                                                 [operation-type employer [offered-value] job-type arbiters ipfs-data])
             transfer-receipt (<! (smart-contracts/contract-send :test-multi-token :safe-transfer-from [employer ethlance-addr token-id sent-amount call-data] {:from employer}))
             job-created-event (<! (smart-contracts/contract-event-in-tx :ethlance :JobCreated transfer-receipt))
             created-job (:job job-created-event)
             job-token-balance (<! (smart-contracts/contract-call :test-multi-token :balance-of [created-job token-id]))]

         (is (= (int job-token-balance) sent-amount))
         (done)))))

  (testing "Batch payment in multi-token (ERC1155)"
    (async done
     (go
       (let [ethlance-addr (smart-contracts/contract-address :ethlance)
            [_owner employer _worker] (<! (web3-eth/accounts @web3))
             token-1-receipt (<? (smart-contracts/contract-send :test-multi-token :award-item [employer 7]))
             token-2-receipt (<? (smart-contracts/contract-send :test-multi-token :award-item [employer 5]))
             token-ids (map (fn [receipt] (. (get-in receipt [:events :Transfer-single :return-values]) -id)) [token-1-receipt token-2-receipt])
             job-type 1
             arbiters []
             ipfs-data "0x0"
             job-proxy-address (get-in addresses/smart-contracts [:job :address])
             _ (<! (ethlance/initialize job-proxy-address))
             test-token-address (smart-contracts/contract-address :test-multi-token)
             offered-token-type (contract-constants/token-type :erc1155)
             sent-amount 3
             operation-type (contract-constants/operation-type :one-step-job-creation)
             offered-values (map (fn [id] {:token
                                    {:tokenContract {:tokenType offered-token-type :tokenAddress test-token-address}
                                     :tokenId id} :value sent-amount}) token-ids)
             call-data (web3-eth/encode-abi (smart-contracts/instance :ethlance)
                                                 :transfer-callback-delegate
                                                 [operation-type employer offered-values job-type arbiters ipfs-data])
             transfer-receipt (<! (smart-contracts/contract-send :test-multi-token :safe-batch-transfer-from
                                                                 [employer ethlance-addr token-ids [sent-amount sent-amount] call-data] {:from employer}))
             job-created-event (<! (smart-contracts/contract-event-in-tx :ethlance :JobCreated transfer-receipt))
             created-job (:job job-created-event)
             job-token-1-balance (<! (smart-contracts/contract-call :test-multi-token :balance-of [created-job (first token-ids)]))
             job-token-2-balance (<! (smart-contracts/contract-call :test-multi-token :balance-of [created-job (second token-ids)]))]

         (is (= (int job-token-1-balance) sent-amount))
         (is (= (int job-token-2-balance) sent-amount))
         (done))))))
