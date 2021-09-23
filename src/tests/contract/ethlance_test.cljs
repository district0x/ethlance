(ns tests.contract.ethlance-test
  (:require [bignumber.core :as bn]
            [cljs-web3-next.eth :as web3-eth]
            [cljs.test :refer-macros [deftest is testing async]]
            [district.server.web3 :refer [web3]]
            [ethlance.server.smart-contracts :as sc]
            [ethlance.server.contract.ethlance :as ethlance]
            [ethlance.shared.contract-constants :as contract-constants]
            [ethlance.shared.smart-contracts-dev :as addresses]
            [district.server.smart-contracts :as smart-contracts]
            [cljs.core.async :refer [<! go]]
            [cljs.core.async.impl.channels :refer [ManyToManyChannel]]
            [district.web3-utils :as web3-utils]
            [district.shared.async-helpers :refer [<?]]
            [tests.helpers.contract-funding :refer [fund-in-eth fund-in-erc20
                                                    fund-in-erc721
                                                    fund-in-erc1155
                                                    create-initialized-job
                                                    eth->wei wei->eth]]))

(defn is-same-amount [amount-a amount-b & more]
  (let [description (first more)]
    (if (nil? description)
     (is (= (bn/number amount-a) (bn/number amount-b)))
     (is (= (bn/number amount-a) (bn/number amount-b)) description))))

(defn- positive-int-upto [n]
  (+ 1 (rand-int (- n 1))))

#_ (deftest ethlance-erc20-payment
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

#_ (deftest ethlance-eth-payment
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

#_ (deftest ethlance-erc1155-payment
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

#_ (deftest job-contract-methods
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
                 (is (nil? tx-receipt) "Expected tx to fail (return nil) because only invited arbiter can call setQuoteForArbitration"))
               (done))))))

(defn offer-from-job-data
  "Fetches the offered value for token-id (contract-constants/token-type)
   item from array (returned from create-initialized-job)"
  [job-data token]
  (first
    (filter #(= (get-in % [:token :tokenContract :tokenType])
                (contract-constants/token-type token) )
            (get-in job-data [:offered-values]))))

#_ (deftest invoice-flows
  (testing "invoice flow (create/pay/cancel)"
    (async done
           (go
             (let [[_owner employer worker] (<! (web3-eth/accounts @web3))
                   job-data (<! (create-initialized-job
                                  [(partial fund-in-eth 0.02)
                                   (partial fund-in-erc20 employer 2)
                                   (partial fund-in-erc721 employer)
                                   (partial fund-in-erc1155 employer 5)]))
                   funding-tx-receipt (:tx-receipt job-data)
                   job-address (:job job-data)
                   invoice-amount-eth 0.01
                   [invoice-amounts _extra] (fund-in-eth invoice-amount-eth [] {})
                   add-candidate-tx (<! (smart-contracts/contract-send [:job job-address] :add-candidate [worker "0x0"] {:from employer}))

                   ; Create invoices
                   tx-receipt (<? (smart-contracts/contract-send [:job job-address] :create-invoice [invoice-amounts "0x0"] {:from worker}))
                   invoice-event (<! (smart-contracts/contract-event-in-tx :ethlance :InvoiceCreated tx-receipt))

                   tx-receipt-2 (<! (smart-contracts/contract-send [:job job-address] :create-invoice [invoice-amounts "0x0"] {:from worker}))
                   invoice-event-2 (<! (smart-contracts/contract-event-in-tx :ethlance :InvoiceCreated tx-receipt-2))

                   invoice-1-id (int (:invoice-id invoice-event))
                   invoice-2-id (int (:invoice-id invoice-event-2))]

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
                   (is (= worker-erc20-balance-change erc-20-token-amount)))

                 ; Pay ERC721 (NFT) invoice
                 (let [nft-offer (offer-from-job-data job-data :erc721)
                       token-id (get-in nft-offer [:token :tokenId])
                       tx-receipt (<! (smart-contracts/contract-send [:job job-address] :create-invoice [[nft-offer] "0x0"] {:from worker}))
                       invoice-event (<! (smart-contracts/contract-event-in-tx :ethlance :InvoiceCreated tx-receipt))
                       invoice-id (int (:invoice-id invoice-event))
                       tx-pay-invoice (<! (smart-contracts/contract-send [:job job-address] :pay-invoice [invoice-id "0x0"] {:from employer}))
                       final-token-owner (<? (smart-contracts/contract-call :test-nft :owner-of [token-id]))]
                   (is (= final-token-owner worker) "In the end the NFT721 must end up at worker's account"))

                 ; Pay ERC1155 (MultiToken) invoice
                 (let [token-offer (offer-from-job-data job-data :erc1155)
                       token-id (get-in token-offer [:token :tokenId])
                       offered-token-amount (get-in token-offer [:value])
                       tx-receipt (<! (smart-contracts/contract-send [:job job-address] :create-invoice [[token-offer] "0x0"] {:from worker}))
                       invoice-event (<! (smart-contracts/contract-event-in-tx :ethlance :InvoiceCreated tx-receipt))
                       invoice-id (int (:invoice-id invoice-event))
                       tx-pay-invoice (<! (smart-contracts/contract-send [:job job-address] :pay-invoice [invoice-id "0x0"] {:from employer}))
                       final-worker-balance (<? (smart-contracts/contract-call :test-multi-token :balance-of [worker token-id]))]
                   (is (= (int final-worker-balance) offered-token-amount) "In the end the offered NFT1155 must end up at worker's account"))
               (done))))))

#_ (deftest adding-candidate
  (testing "Job#addCandidate"
    (async done
           (go
             (let [[_owner employer worker candidate-a candidate-b] (<! (web3-eth/accounts @web3))
                   job-data (<! (create-initialized-job [(partial fund-in-eth 0.01)]))
                   job-address (:job job-data)
                   empty-ipfs-data "0x0"]

               ; Basic scenario - add candidate to a created GIG job by employer
               (let [tx-receipt (<! (smart-contracts/contract-send [:job job-address] :add-candidate [candidate-a empty-ipfs-data] {:from employer}))
                     candidate-added-event (<! (smart-contracts/contract-event-in-tx :ethlance :CandidateAdded tx-receipt))]
                 (is (= (:candidate candidate-added-event) candidate-a)))

               ; Add duplicate candidate (should fail)
               (let [tx-receipt (<! (smart-contracts/contract-send [:job job-address] :add-candidate [candidate-a empty-ipfs-data] {:from employer}))]
                 (is (= tx-receipt nil)))

               ; Try adding candidate for other job type - bounty (should fail)
              (let [[_owner employer _worker candidate-a candidate-b] (<! (web3-eth/accounts @web3))
                    job-data (<! (create-initialized-job [(partial fund-in-eth 0.01)]
                                                         :job-type (contract-constants/job-type :bounty)))
                    job-address (:job job-data)
                    empty-ipfs-data "0x0"
                    tx-receipt (<! (smart-contracts/contract-send [:job job-address] :add-candidate [candidate-b empty-ipfs-data] {:from employer}))]
                (is (= nil tx-receipt) "Transaction fails (receipt nil) when adding candidate to non-GIG (e.g. BOUNTY) job"))

              ; Try adding candidate by user other than job creator (should fail)
              (let [tx-receipt (<! (smart-contracts/contract-send [:job job-address] :add-candidate [worker empty-ipfs-data] {:from worker}))]
                (is (= nil tx-receipt))))
             (done)))))

(deftest cancel-invoice-test
  (testing "Job#cancelInvoice"
    (async done
           (go
             (let [[_owner employer worker] (<! (web3-eth/accounts @web3))
                   job-data (<! (create-initialized-job [(partial fund-in-erc20 employer 2)]))
                   token-offer (offer-from-job-data job-data :erc20)
                   job-address (:job job-data)
                   empty-ipfs-data "0x0"
                   invoice-tx-receipt (<! (smart-contracts/contract-send [:job job-address] :create-invoice [[token-offer] "0x0"] {:from worker}))
                   invoice-event (<! (smart-contracts/contract-event-in-tx :ethlance :InvoiceCreated invoice-tx-receipt))
                   invoice-id (int (:invoice-id invoice-event))]

               ; Successful cancel
               (let [tx-cancel-invoice (<! (smart-contracts/contract-send [:job job-address] :cancel-invoice [invoice-id "0x0"] {:from worker}))
                     cancel-event (<! (smart-contracts/contract-event-in-tx :ethlance :InvoiceCancelled tx-cancel-invoice))]
                 (is (= (int (:invoice-id cancel-event)) invoice-id)))

               ; Cancel same invoice again (tx fails)
               (let [tx-cancel-invoice (<! (smart-contracts/contract-send [:job job-address] :cancel-invoice [invoice-id "0x0"] {:from worker}))]
                 (is (= nil tx-cancel-invoice)))

               ; Create new invoice, pay it & try to cancel (should fail)
               (let [invoice-tx-receipt (<! (smart-contracts/contract-send [:job job-address] :create-invoice [[token-offer] "0x0"] {:from worker}))
                     invoice-event (<! (smart-contracts/contract-event-in-tx :ethlance :InvoiceCreated invoice-tx-receipt))
                     invoice-id (int (:invoice-id invoice-event))

                     tx-pay-invoice (<! (smart-contracts/contract-send [:job job-address] :pay-invoice [invoice-id "0x0"] {:from employer}))
                     event-pay-invoice (<! (smart-contracts/contract-event-in-tx :ethlance :InvoicePaid tx-pay-invoice))

                     tx-cancel-invoice (<! (smart-contracts/contract-send [:job job-address] :cancel-invoice [invoice-id "0x0"] {:from worker}))
                     invoice-raw (<! (smart-contracts/contract-call [:job job-address] :get-invoice [invoice-id]))
                     invoice (js->clj invoice-raw) ; [[[[0 0x0000000000000000000000000000000000000000] 0] 0] 0x0000000000000000000000000000000000000000 0 true false]
                     invoice-paid? (get-in invoice [3])
                     invoice-cancelled? (get-in invoice [4])]
                 (is (= nil tx-cancel-invoice))
                 (is (= invoice-paid? true))
                 (is (= invoice-cancelled? false))))
             (done)))))

(deftest accepting-arbiter-quote
  (testing "Accept arbiter quote workflow"
    (async done
           ; Test steps
           ; 1. Create a job (create-initialized-job) with invited arbiter
           ; 2. Arbiter sets a quote (Job#setQuoteForArbitration)
           ; 3. Employer accepts quote (by sending Tx with the funds included)
           ; 4. Assert that arbiter now has the funds
           ; 5. Try accepting another arbiter (should fail)
           (go
             ; ERC20
             (let [[_owner employer _worker arbiter] (<! (web3-eth/accounts @web3))
                   ; 1. Create a job (create-initialized-job) with invited arbiter
                   arbiter-funds-before (<? (smart-contracts/contract-call :token :balance-of [arbiter]))
                   arbiter-quote-amount 2
                   job-data (<! (create-initialized-job [(partial fund-in-erc20 employer arbiter-quote-amount)]
                                                        :arbiters [arbiter]))
                   job-address (:job job-data)
                   token-offer [(offer-from-job-data job-data :erc20)]

                   ; 2. Arbiter sets a quote (Job#setQuoteForArbitration)
                   set-quote-tx (<! (smart-contracts/contract-send [:job job-address] :set-quote-for-arbitration [token-offer] {:from arbiter}))

                   ; 3. Employer accepts quote (by sending Tx with the funds included)
                   ;      - in case of tokens not supporting callbacks(ERC20), 2 tx are needed:
                   ;        1st approves the amount in token contract
                   ;        2nd calls Job#setQuoteForArbitration
                   approve-for-job-tx (<! (smart-contracts/contract-send :token :approve [job-address arbiter-quote-amount] {:from employer}))
                   accept-quote-tx (<! (smart-contracts/contract-send [:job job-address] :accept-quote-for-arbitration [arbiter token-offer] {:from employer}))

                   ; 4. Assert that arbiter now has the funds
                   arbiter-funds-after (<? (smart-contracts/contract-call :token :balance-of [arbiter]))]
               (is (= arbiter-quote-amount (- arbiter-funds-after arbiter-funds-before))))

             ; ERC721
             (let [[_owner employer _worker arbiter] (<! (web3-eth/accounts @web3))
                   ; 1. Create a job (create-initialized-job) with invited arbiter
                   arbiter-funds-before (<? (smart-contracts/contract-call :test-nft :balance-of [arbiter]))
                   job-data (<! (create-initialized-job [(partial fund-in-erc20 employer 1)] :arbiters [arbiter]))
                   token-offer [(offer-from-job-data job-data :erc20)]
                   job-address (:job job-data)
                   [[erc721-offer] _] (<! (fund-in-erc721 employer [] {} :approval false))
                   token-id (get-in  erc721-offer [:token :tokenId])

                   ; 2. Arbiter sets a quote (Job#setQuoteForArbitration)
                   set-quote-tx (<! (smart-contracts/contract-send [:job job-address] :set-quote-for-arbitration [token-offer] {:from arbiter}))

                   ; 3. Employer accepts quote (by sending Tx with the funds included)
                   ;    As ERC721 (and 1155) support callbacks, this can be done with a single tx
                   ;    (we prepare transaction with data, employer signs & sends it, ERC721 contract
                   ;    calls Job#onERC721Received, in which we'll make necessary state changes)
                   target-method (contract-constants/job-target-method :accept-quote-for-arbitration)
                   not-used-invoice-id 0 ; just a placeholder, not used for accepting quote call
                   call-data (web3-eth/encode-abi (smart-contracts/instance :job)
                                                       :example-function-signature-for-token-callback-data-encoding
                                                       [target-method arbiter, [erc721-offer], not-used-invoice-id])

                   send-tokens-tx (<! (smart-contracts/contract-send :test-nft
                                                                         :safe-transfer-from
                                                                         [employer arbiter token-id call-data]
                                                                         {:from employer}))
                   accept-quote-tx (<! (smart-contracts/contract-send [:job job-address] :accept-quote-for-arbitration [arbiter token-offer] {:from employer}))

                   ; 4. Assert that arbiter now owns the token
                   token-owner-after (<? (smart-contracts/contract-call :test-nft :owner-of [token-id]))]
               (is (= arbiter token-owner-after)))
               (done)))))
