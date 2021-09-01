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
            [cljs.core.async.impl.channels :refer [ManyToManyChannel]]
            [district.web3-utils :as web3-utils]
            [district.shared.async-helpers :refer [<?]]))

(defn is-same-amount [amount-a amount-b & more]
  (let [description (first more)]
    (if (nil? description)
     (is (= (bn/number amount-a) (bn/number amount-b)))
     (is (= (bn/number amount-a) (bn/number amount-b)) description))))

(defn- positive-int-upto [n]
  (+ 1 (rand-int (- n 1))))

(defn eth->wei [eth-amount]
  (let [wei-in-eth (bn/number 10e17)]
    (bn/number (* wei-in-eth (bn/number eth-amount)))))

(defn wei->eth [wei-amount]
  (let [wei-in-eth (bn/number 10e17)]
    (/ (bn/number wei-amount) wei-in-eth)))

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
             receipt (<? (smart-contracts/contract-send :test-nft :award-item [employer])) ; Give him 1st token
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
             call-data (web3-eth/encode-abi (smart-contracts/instance :ethlance)
                                                 :create-job
                                                 [employer [offered-value] job-type arbiters ipfs-data])
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
             sent-amount 3
             offered-value {:token
                            {:tokenContract {:tokenType offered-token-type :tokenAddress test-token-address}
                             :tokenId token-id}
                            :value sent-amount}
             call-data (web3-eth/encode-abi (smart-contracts/instance :ethlance)
                                                 :create-job
                                                 [employer [offered-value] job-type arbiters ipfs-data])
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
             offered-values (map (fn [id] {:token
                                    {:tokenContract {:tokenType offered-token-type :tokenAddress test-token-address}
                                     :tokenId id} :value sent-amount}) token-ids)
             call-data (web3-eth/encode-abi (smart-contracts/instance :ethlance)
                                                 :create-job
                                                 [employer offered-values job-type arbiters ipfs-data])
             transfer-receipt (<! (smart-contracts/contract-send :test-multi-token :safe-batch-transfer-from
                                                                 [employer ethlance-addr token-ids [sent-amount sent-amount] call-data] {:from employer}))
             job-created-event (<! (smart-contracts/contract-event-in-tx :ethlance :JobCreated transfer-receipt))
             created-job (:job job-created-event)
             job-token-1-balance (<! (smart-contracts/contract-call :test-multi-token :balance-of [created-job (first token-ids)]))
             job-token-2-balance (<! (smart-contracts/contract-call :test-multi-token :balance-of [created-job (second token-ids)]))]

         (is (= (int job-token-1-balance) sent-amount))
         (is (= (int job-token-2-balance) sent-amount))
         (done))))))

(defn- fund-in-eth
  "Produces data 2 structures that can be used as input for `ethlance/create-job`

   TIP: use with currying provide the eth-amount, so it can be used with 2 args in the
   create-initialized-job reduce function"
  [eth-amount offered-values create-job-opts]

  (let [
        amount-in-wei (str (eth->wei eth-amount)) ; FIXME: why web3-utils/eth->wei returns nil
        offered-token-type (contract-constants/token-type :eth)
        placeholder-address "0x1111111111111111111111111111111111111111"
        not-used-for-erc20 0
        eth-offered-value {:token
                           {:tokenContract {:tokenType offered-token-type
                                            :tokenAddress placeholder-address}
                            :tokenId not-used-for-erc20} :value amount-in-wei}
        additional-opts {:value amount-in-wei}]
    [(conj offered-values eth-offered-value) (merge create-job-opts additional-opts)]))

(defn- fund-in-erc20
  "Mints ERC20 TestToken for recipient and approves them for Ethlance.
   Returns 2 data structures to be used for ethlance/create-job"
  [recipient funding-amount offered-values create-job-opts]
  (go
    (let [ethlance-addr (smart-contracts/contract-address :ethlance)
          [_owner employer _worker] (<! (web3-eth/accounts @web3))
          job-type 1
          arbiters []
          ipfs-data "0x0"
          _ (<? (smart-contracts/contract-send :token :mint [recipient funding-amount]))
          test-token-address (smart-contracts/contract-address :token)
          not-used-for-erc20 0
          offered-token-type (contract-constants/token-type :erc20)
          erc-20-value {:token
                        {:tokenContract {:tokenType offered-token-type :tokenAddress test-token-address}
                         :tokenId not-used-for-erc20} :value funding-amount}]
      (<! (smart-contracts/contract-send :token :approve [ethlance-addr funding-amount] {:from recipient}))
      [(conj offered-values erc-20-value) create-job-opts])))


(defn collect-from-funding-funcs
  "Takes the previous results, which are produced by calling other funding functions and
   appends passes them to the next funding function.
   To support the funding functions to be async (e.g. result of a go block when having to send
   messages to the blockchain network), there is the check (instance? ManyToManyChannel) to unify the result usage"
  [previous-results funding-func]
  (go
    (let [[offered additional] (if (instance? ManyToManyChannel previous-results)
                                 (<? previous-results) previous-results)
          funding-result (funding-func offered additional)
          [new-offered new-additional] (if (instance? ManyToManyChannel funding-result)
                                         (<? funding-result) funding-result)]
      [new-offered new-additional])))

(defn- create-initialized-job
  "Creates new Job contract and initializes Ethlance contract with it.
   Also adds initial-balance-in-wei (currently 0.05 ETH) to the balance."
  ([] (create-initialized-job []))

  ([funding-functions & {arbiters :arbiters :or {arbiters []}}]
   (go
      (let [[_owner employer worker] (<! (web3-eth/accounts @web3))
            job-type (contract-constants/job-type :gig)
            ipfs-data "0x0"
            job-impl-address (get-in addresses/smart-contracts [:job :address])
            real-vals (reduce collect-from-funding-funcs [[] {}] funding-functions)
            [offered-values additional-opts] (<! real-vals)]
        (<! (ethlance/initialize job-impl-address))
        (let [tx-receipt (<! (ethlance/create-job employer
                                                  offered-values
                                                  job-type
                                                  arbiters
                                                  ipfs-data
                                                  additional-opts))
              create-job-event (<! (smart-contracts/contract-event-in-tx :ethlance :JobCreated tx-receipt))
              created-job-address (:job create-job-event)]
          {:ethlance (smart-contracts/contract-address :ethlance)
           :job created-job-address
           :employer employer
           :worker worker
           :offered-values offered-values
           :tx-receipt tx-receipt})))))

(deftest job-contract-methods
  (testing "setQuoteForArbitration"
    (async done
           (go
             (let [[_owner employer _worker arbiter] (<! (web3-eth/accounts @web3))
                   job-data (<! (create-initialized-job [(partial fund-in-eth 0.01)] :arbiters [arbiter]))
                   job-address (:job job-data)
                   quoted-token-address "0x1111111111111111111111111111111111111111" ; placeholder for ETH
                   payment-in-wei (str 10000000000000000) ; 0.01 ETH
                   token-type (contract-constants/token-type :eth)
                   [arbitration-quote _extra] (fund-in-eth 0.01 [] {})
                   tx-receipt (<! (smart-contracts/contract-send [:job job-address] :set-quote-for-arbitration [arbitration-quote] {:from arbiter}))
                   quote-set-event (<! (smart-contracts/contract-event-in-tx :ethlance :QuoteForArbitrationSet tx-receipt))
                   quote-from-event (js->clj (:quote quote-set-event)) ; Structure: [[[[0 0x1111111111111111111111111111111111111111] 0] 10000000000000000]]
                   value-held-by-job-contract (<? (web3-eth/get-balance @web3 job-address))
                   received-amount (get-in quote-from-event [0 1])
                   received-token-address (get-in quote-from-event [0 0 0 1])
                   received-token-type (get-in quote-from-event [0 0 0 0])]
               ; Job related assertions (for debugging & sanity checking)
               (is (not (nil? (:job job-data))))
               (is (= payment-in-wei value-held-by-job-contract))
               (is (= job-address (:job quote-set-event)))

               ; setQuoteForArbitration related assertions
               (is (= arbiter (:arbiter quote-set-event)))
               (is (= payment-in-wei received-amount))
               (is (= quoted-token-address received-token-address))
               (is (= (str token-type) received-token-type))

               (let [[arbitration-quote _extra] (fund-in-eth 10 [] {})
                     tx-receipt (<! (smart-contracts/contract-send [:job job-address] :set-quote-for-arbitration [arbitration-quote] {:from employer}))]
                 (is (nil? tx-receipt) "Expected to fail because only invited arbiter can call setQuoteForArbitration"))

               (done))))))

(deftest invoice-flows
  (testing "invoice flow (create/pay/cancel)"
    (async done
           (go
             (let [[_owner employer worker] (<! (web3-eth/accounts @web3))
                   job-data (<! (create-initialized-job [(partial fund-in-eth 0.02) (partial fund-in-erc20 employer 2)]))
                   job-address (:job job-data)
                   invoice-amount-eth 0.01
                   [invoice-amounts _extra] (fund-in-eth invoice-amount-eth [] {})
                   add-candidate-tx (<! (smart-contracts/contract-send [:job job-address] :add-candidate [worker "0x0"] {:from employer}))

                   ; Create invoices
                   tx-receipt (<! (smart-contracts/contract-send [:job job-address] :create-invoice [invoice-amounts "0x0"] {:from worker}))
                   invoice-event (<! (smart-contracts/contract-event-in-tx :ethlance :InvoiceCreated tx-receipt))

                   tx-receipt-2 (<! (smart-contracts/contract-send [:job job-address] :create-invoice [invoice-amounts "0x0"] {:from worker}))
                   invoice-event-2 (<! (smart-contracts/contract-event-in-tx :ethlance :InvoiceCreated tx-receipt-2))

                   invoice-1-id (int (:invoice-id invoice-event))
                   invoice-2-id (int (:invoice-id invoice-event-2))
                   ]

                 (is (not (nil? tx-receipt)))
                 (is (< invoice-1-id invoice-2-id) "Different invoices must have different ids (and they're auto-increment)")

                 ; Try to pay as other than Job creator
                 (is (= nil (<! (smart-contracts/contract-send [:job job-address] :pay-invoice [invoice-1-id "0x0"] {:from worker}))), "Only job creator(employer) can pay invoices")

                 ; Pay ETH invoice
                 (let [worker-eth-balance-before (<? (web3-eth/get-balance @web3 worker))
                       tx-pay-invoice (<! (smart-contracts/contract-send [:job job-address] :pay-invoice [invoice-1-id "0x0"] {:from employer}))
                       event-pay-invoice (<! (smart-contracts/contract-event-in-tx :ethlance :InvoicePaid tx-pay-invoice))
                       worker-eth-balance-after (<? (web3-eth/get-balance @web3 worker))
                       worker-eth-change (wei->eth (bn/- (bn/number worker-eth-balance-after) (bn/number worker-eth-balance-before)))
                       ]
                   (is (= (int (:invoice-id event-pay-invoice)) invoice-1-id))
                   (is (= worker-eth-change invoice-amount-eth)))

                 ; Pay ERC20 invoice
                 (let [erc-20-token-amount 1
                       worker-initial-erc20-balance (<? (smart-contracts/contract-call :token :balance-of [worker]))
                       [erc-20-invoice-amounts _extra] (<? (fund-in-erc20 employer erc-20-token-amount [] {}))
                       ; Create invoices
                       tx-receipt (<! (smart-contracts/contract-send [:job job-address] :create-invoice [erc-20-invoice-amounts "0x0"] {:from worker}))
                       invoice-event (<! (smart-contracts/contract-event-in-tx :ethlance :InvoiceCreated tx-receipt))
                       erc-20-invoice-id (int (:invoice-id invoice-event))
                       tx-pay-invoice (<! (smart-contracts/contract-send [:job job-address] :pay-invoice [erc-20-invoice-id "0x0"] {:from employer}))
                       worker-final-erc20-balance (<? (smart-contracts/contract-call :token :balance-of [worker]))
                       worker-erc20-balance-change (- (int worker-final-erc20-balance) (int worker-initial-erc20-balance))
                       ]
                   (is (= worker-erc20-balance-change erc-20-token-amount))
                   )
               (done))))))
