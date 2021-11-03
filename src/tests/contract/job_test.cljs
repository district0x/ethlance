(ns tests.contract.job-test
  (:require [bignumber.core :as bn]
            [cljs-web3-next.eth :as web3-eth]
            [cljs.test :refer-macros [deftest is testing async]]
            [district.server.web3 :refer [web3]]
            [ethlance.server.contract.job :as job-contract]
            [ethlance.shared.contract-constants :as contract-constants]
            [district.server.smart-contracts :as smart-contracts]
            [cljs.core.async :refer [<! go]]
            [district.shared.async-helpers :refer [<?]]
            [tests.helpers.contract :as cont :refer [tx-reverted-with]]
            [tests.helpers.contract-funding :refer [fund-in-eth fund-in-erc20
                                                    fund-in-erc721
                                                    fund-in-erc1155
                                                    create-initialized-job
                                                    eth->wei wei->eth
                                                    approx=] :as cofu]))

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

(deftest invoice-flows
  (testing "invoice flow (create/pay/cancel)"
    (async done
           (go
             (let [[_owner employer worker] (<! (web3-eth/accounts @web3))
                   job-data (<! (create-initialized-job
                                  [(partial fund-in-eth 0.02)
                                   (partial fund-in-erc20 employer 2)
                                   (partial fund-in-erc721 employer)
                                   (partial fund-in-erc1155 employer 5)]))
                   job-address (:job job-data)
                   invoice-amount-eth 0.01
                   [invoice-amounts _extra] (fund-in-eth invoice-amount-eth [] {})
                   _ (<! (smart-contracts/contract-send [:job job-address] :add-candidate [worker "0x0"] {:from employer}))

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
                       worker-eth-change (wei->eth (bn/- (bn/number worker-eth-balance-after) (bn/number worker-eth-balance-before)))]
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
                       _ (<! (smart-contracts/contract-send [:job job-address] :pay-invoice [erc-20-invoice-id "0x0"] {:from employer}))
                       worker-final-erc20-balance (<? (smart-contracts/contract-call :token :balance-of [worker]))
                       worker-erc20-balance-change (- (int worker-final-erc20-balance) (int worker-initial-erc20-balance))]
                   (is (= worker-erc20-balance-change erc-20-token-amount)))

                 ; Pay ERC721 (NFT) invoice
                 (let [nft-offer (offer-from-job-data job-data :erc721)
                       token-id (get-in nft-offer [:token :tokenId])
                       tx-receipt (<! (smart-contracts/contract-send [:job job-address] :create-invoice [[nft-offer] "0x0"] {:from worker}))
                       invoice-event (<! (smart-contracts/contract-event-in-tx :ethlance :InvoiceCreated tx-receipt))
                       invoice-id (int (:invoice-id invoice-event))
                       _ (<! (smart-contracts/contract-send [:job job-address] :pay-invoice [invoice-id "0x0"] {:from employer}))
                       final-token-owner (<? (smart-contracts/contract-call :test-nft :owner-of [token-id]))]
                   (is (= final-token-owner worker) "In the end the NFT721 must end up at worker's account"))

                 ; Pay ERC1155 (MultiToken) invoice
                 (let [token-offer (offer-from-job-data job-data :erc1155)
                       token-id (get-in token-offer [:token :tokenId])
                       offered-token-amount (get-in token-offer [:value])
                       tx-receipt (<! (smart-contracts/contract-send [:job job-address] :create-invoice [[token-offer] "0x0"] {:from worker}))
                       invoice-event (<! (smart-contracts/contract-event-in-tx :ethlance :InvoiceCreated tx-receipt))
                       invoice-id (int (:invoice-id invoice-event))
                       _ (<! (smart-contracts/contract-send [:job job-address] :pay-invoice [invoice-id "0x0"] {:from employer}))
                       final-worker-balance (<? (smart-contracts/contract-call :test-multi-token :balance-of [worker token-id]))]
                   (is (= (int final-worker-balance) offered-token-amount) "In the end the offered NFT1155 must end up at worker's account"))
               (done))))))

(deftest adding-candidate
  (testing "Job#addCandidate"
    (async done
           (go
             (let [[_owner employer worker candidate-a candidate-b] (<! (web3-eth/accounts @web3))
                   job-data (<! (create-initialized-job [(partial fund-in-eth 0.01)]))
                   job-address (:job job-data)
                   empty-ipfs-data "0x0"]

               ; Basic scenario - add candidate to a created GIG job by employer
               (let [tx-receipt (<! (smart-contracts/contract-send [:job job-address] :add-candidate [candidate-a empty-ipfs-data] {:from employer :output :receipt-or-error}))
                     ; candidate-added-event (<! (smart-contracts/contract-event-in-tx :ethlance :CandidateAdded tx-receipt))
                     ; FIXME:
                     ; Explanation of this also in Ethlance.sol Basically (web3-helpers/event-interface contract-instance event)
                     ;  doesn't find the event signature hash and thus can't filter the event. Though the event is in the logs
                     ;  (checked it via truffle console).
                     candidate-added-event (<! (smart-contracts/contract-event-in-tx :ethlance :CandidatoAgregado tx-receipt))
                     ]
                 (is (= (:candidate candidate-added-event) candidate-a)))

               ; Add duplicate candidate (should fail)
               (let [tx-receipt (<! (smart-contracts/contract-send [:job job-address] :add-candidate [candidate-a empty-ipfs-data] {:from employer}))]
                 (is (= tx-receipt nil)))

               ; Try adding candidate for other job type - bounty (should fail)
              (let [job-data (<! (create-initialized-job [(partial fund-in-eth 0.01)]
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
                   _ (<! (smart-contracts/contract-send [:job job-address] :add-candidate [worker empty-ipfs-data] {:from employer}))
                   invoice-tx-receipt (<! (smart-contracts/contract-send [:job job-address] :create-invoice [[token-offer] empty-ipfs-data] {:from worker}))
                   invoice-event (<! (smart-contracts/contract-event-in-tx :ethlance :InvoiceCreated invoice-tx-receipt))
                   invoice-id (int (:invoice-id invoice-event))]

               ; Successful cancel
               (let [tx-cancel-invoice (<! (smart-contracts/contract-send [:job job-address] :cancel-invoice [invoice-id "0x0"] {:from worker}))
                     cancel-event (<! (smart-contracts/contract-event-in-tx :ethlance :InvoiceCancelled tx-cancel-invoice))]
                 (is (= (int (:invoice-id cancel-event)) invoice-id)))

               ; Cancel same invoice again (tx fails)
               (let [tx-cancel-invoice (<! (smart-contracts/contract-send
                                             [:job job-address] :cancel-invoice [invoice-id "0x0"] {:from worker :output :receipt-or-error}))]
                 (is (tx-reverted-with tx-cancel-invoice #"was already cancelled")))

               ; Create new invoice, pay it & try to cancel (should fail)
               (let [invoice-tx-receipt (<! (smart-contracts/contract-send [:job job-address] :create-invoice [[token-offer] "0x0"] {:from worker}))
                     invoice-event (<! (smart-contracts/contract-event-in-tx :ethlance :InvoiceCreated invoice-tx-receipt))
                     invoice-id (int (:invoice-id invoice-event))

                     tx-pay-invoice (<! (smart-contracts/contract-send [:job job-address] :pay-invoice [invoice-id "0x0"] {:from employer}))
                     event-pay-invoice (<! (smart-contracts/contract-event-in-tx :ethlance :InvoicePaid tx-pay-invoice))

                     tx-cancel-invoice (<! (smart-contracts/contract-send
                                             [:job job-address] :cancel-invoice [invoice-id "0x0"] {:from worker :output :receipt-or-error}))
                     invoice-raw (<! (smart-contracts/contract-call [:job job-address] :get-invoice [invoice-id]))
                     invoice (js->clj invoice-raw) ; [[[[0 0x0000000000000000000000000000000000000000] 0] 0] 0x0000000000000000000000000000000000000000 0 true false]
                     invoice-paid? (get-in invoice [3])
                     invoice-cancelled? (get-in invoice [4])]
                 (is (= (int (:invoice-id event-pay-invoice)) invoice-id))
                 (is (tx-reverted-with tx-cancel-invoice #"was already paid"))
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
                   _ (<! (smart-contracts/contract-send [:job job-address] :set-quote-for-arbitration [token-offer] {:from arbiter}))

                   ; 3. Employer accepts quote (by sending Tx with the funds included)
                   ;      - in case of tokens not supporting callbacks(ERC20), 2 tx are needed:
                   ;        1st approves the amount in token contract
                   ;        2nd calls Job#setQuoteForArbitration
                   _ (<! (smart-contracts/contract-send :token :approve [job-address arbiter-quote-amount] {:from employer}))
                   _ (<! (smart-contracts/contract-send [:job job-address] :accept-quote-for-arbitration [arbiter token-offer] {:from employer}))

                   ; 4. Assert that arbiter now has the funds
                   arbiter-funds-after (<? (smart-contracts/contract-call :token :balance-of [arbiter]))]
               (is (= arbiter-quote-amount (- arbiter-funds-after arbiter-funds-before))))

             ; ERC721
             (let [[_owner employer _worker arbiter] (<! (web3-eth/accounts @web3))
                   ; 1. Create a job (create-initialized-job) with invited arbiter
                   job-data (<! (create-initialized-job [(partial fund-in-erc20 employer 1)] :arbiters [arbiter]))
                   token-offer [(offer-from-job-data job-data :erc20)]
                   job-address (:job job-data)
                   [[erc721-offer] _] (<! (fund-in-erc721 employer [] {} :approval false))
                   token-id (get-in  erc721-offer [:token :tokenId])

                   ; 2. Arbiter sets a quote (Job#setQuoteForArbitration)
                   _ (<! (smart-contracts/contract-send [:job job-address] :set-quote-for-arbitration [token-offer] {:from arbiter}))

                   ; 3. Employer accepts quote (by sending Tx with the funds included)
                   ;    As ERC721 (and 1155) support callbacks, this can be done with a single tx
                   ;    (we prepare transaction with data, employer signs & sends it, ERC721 contract
                   ;    calls Job#onERC721Received, in which we'll make necessary state changes)
                   target-method (contract-constants/job-callback-target-method :accept-quote-for-arbitration)
                   not-used-invoice-id 0 ; just a placeholder, not used for accepting quote call
                   call-data (web3-eth/encode-abi (smart-contracts/instance :job)
                                                       :example-function-signature-for-token-callback-data-encoding
                                                       [target-method arbiter [erc721-offer] not-used-invoice-id])

                   _ (<! (smart-contracts/contract-send :test-nft :safe-transfer-from [employer arbiter token-id call-data] {:from employer}))
                   _ (<! (smart-contracts/contract-send [:job job-address] :accept-quote-for-arbitration [arbiter token-offer] {:from employer}))

                   ; 4. Assert that arbiter now owns the token
                   token-owner-after (<? (smart-contracts/contract-call :test-nft :owner-of [token-id]))]
               (is (= arbiter token-owner-after)))
               (done)))))

(deftest add-withdraw-funds
  (testing "Add & withdraw funds workflows"
    (async done
           (go
             ; ERC20
             (let [[_owner contributor-a _worker contributor-b contributor-c] (<! (web3-eth/accounts @web3))
                   ; 1. Create a job (create-initialized-job) with ETH (employer account A)
                   contributor-a-amount 0.05
                   contributor-b-amount 0.07
                   contributor-c-amount 3
                   job-data (<! (create-initialized-job [(partial fund-in-eth contributor-a-amount)]))
                   job-address (:job job-data)
                   contributor-a-amount-eth (offer-from-job-data job-data :eth)
                   target-method-add-funds (contract-constants/job-callback-target-method :add-funds)

                   ; 2. Add ETH from account B after job creation
                   [contributor-b-offer _extra] (fund-in-eth contributor-b-amount)
                   _ (<! (smart-contracts/contract-send [:job job-address]
                                                        :add-funds
                                                        [contributor-b-offer]
                                                        {:from contributor-b :value (eth->wei contributor-b-amount)}))

                   ; 3. Add ERC1155 from account C
                   [[contributor-c-offer-multi-token] _] (<! (fund-in-erc1155 contributor-c contributor-c-amount))
                   token-id (get-in contributor-c-offer-multi-token [:token :tokenId])
                   not-used-invoice-id 0 ; just a placeholder, not used for add-funds call
                   call-data (web3-eth/encode-abi (smart-contracts/instance :job)
                                                       :example-function-signature-for-token-callback-data-encoding
                                                       [target-method-add-funds contributor-c [contributor-c-offer-multi-token] not-used-invoice-id])
                   _ (<! (smart-contracts/contract-send :test-multi-token
                                                        :safe-transfer-from
                                                        [contributor-c job-address token-id contributor-c-amount call-data]
                                                        {:from contributor-c}))
                   funds-in-job-by-contributor-c (map contract-constants/token-value-vec->map
                                                      (js->clj (<! (smart-contracts/contract-call [:job job-address] :get-deposits [contributor-c]))))]
               (is (= (:value (first funds-in-job-by-contributor-c)) contributor-c-amount) "contributor-c ERC1155 tokens have to end up in Job before continuing")

               ; 4. A withdraws his ETH
               (let [balance-before (js/parseInt (<! (web3-eth/get-balance @web3 contributor-a)))
                     _ (<! (smart-contracts/contract-send [:job job-address] :withdraw-funds [[contributor-a-amount-eth]] {:from contributor-a}))
                     balance-after (js/parseInt (<! (web3-eth/get-balance @web3 contributor-a)))
                     balance-change (- balance-after balance-before)
                     allowed-error-pct 0.03]; Allow 3% difference due to gas fees
                 (is (> balance-after balance-before) "Withdrawing should have increased A's balance")
                 (is (approx= allowed-error-pct (eth->wei contributor-a-amount) balance-change) "After withdrawal the ETH must end up at A's account"))

               ; 5. C withdraws his ERC1155
               (let [_ (<! (smart-contracts/contract-send [:job job-address] :withdraw-funds [[contributor-c-offer-multi-token]] {:from contributor-c}))
                     contributor-owned-tokens (js/parseInt (<! (smart-contracts/contract-call :test-multi-token :balance-of [contributor-c token-id])))]
                 (is (= contributor-c-amount contributor-owned-tokens) "After withdrawal the ERC1155 must end up at C's account"))

               ; 5.a C can't withdraw ETH (as he didn't contribute any)
               (let [balance-before (js/parseInt (<! (web3-eth/get-balance @web3 contributor-c)))
                     withdraw-tx (<! (smart-contracts/contract-send [:job job-address] :withdraw-funds [[contributor-a-amount-eth]] {:from contributor-c}))
                     balance-after (js/parseInt (<! (web3-eth/get-balance @web3 contributor-c)))
                     allowed-error-pct 0.0001]; Allow 0.01% difference due to gas fees
                 (is (= nil withdraw-tx) "Tx fails (nil receipt) because C didn't contribute ETH")
                 (is (approx= allowed-error-pct balance-before balance-after) "The Tx fails but some gas gets spent still"))

               ; 6. B can withdraw their contributed ETH
               (let [balance-before (js/parseInt (<! (web3-eth/get-balance @web3 contributor-b)))
                     _ (<! (smart-contracts/contract-send [:job job-address] :withdraw-funds [contributor-b-offer] {:from contributor-b}))
                     balance-after (js/parseInt (<! (web3-eth/get-balance @web3 contributor-b)))
                     balance-change (- balance-after balance-before)
                     allowed-error-pct 0.03]; Allow 3% difference due to gas fees
                 (is (> balance-after balance-before) "Withdrawing should have increased B's balance")
                 (is (approx= allowed-error-pct (eth->wei contributor-b-amount) balance-change) "After withdrawal the ETH must end up at B's account"))
               (done))))))

(deftest raise-resolve-disputes
  (testing "Raising & resolving disputes"
    (async done
           (go
             ; 1. Job gets set up & funded by A with 2 ERC20
             ; 2. A adds 4 ERC20
             ; 3. B adds 3 ERC20
             ; 4. Worker raises invoice for 9 ERC20
             ; 5. Worker raises dispute
             ; 6. Arbiter resolves dispute with TokenValue[] value = 6 (2/3)
             ; 7. Payouts (proportional):
             ;   - Worker (2/3)*9       = *6* (2/3 of the invoice according to arbiter)
             ;   - A gets (1/3)*(2/3)*9 = *2* (1/3 remaining, his contribution was 2/3)
             ;   - B gets (1/3)*(1/3)*9 = *1*
             (let [[_owner employer worker sponsor arbiter] (<! (web3-eth/accounts @web3))
                   contribution-a-amount 2
                   contribution-b-amount 4
                   contribution-c-amount 3

                   ; 1. Job gets set up & funded by A with 2 ERC20
                   job-data (<! (create-initialized-job [(partial fund-in-erc20 employer contribution-a-amount)] :arbiters [arbiter]))
                   job-address (:job job-data)
                   _ (<! (smart-contracts/contract-send [:job job-address] :add-candidate [worker "0x0"] {:from employer}))
                   employer-contribution-a (offer-from-job-data job-data :erc20)

                   ; 2. A adds 4 ERC20
                   [extra-funding-from-employer _extra] (<! (fund-in-erc20 employer contribution-b-amount [] {} :approve-for job-address))
                   _ (<! (smart-contracts/contract-send [:job job-address] :add-funds [extra-funding-from-employer] {:from employer}))

                   ; 3. B adds 3 ERC20
                   [extra-funding-from-sponsor _extra] (<! (fund-in-erc20 sponsor contribution-c-amount [] {} :approve-for job-address))
                   _ (<! (smart-contracts/contract-send [:job job-address] :add-funds [extra-funding-from-sponsor] {:from sponsor}))
                   ; 4. Worker raises invoice for 9 ERC20
                   invoice-amounts-all [(merge employer-contribution-a {:value 9})]
                   invoice-tx (<? (smart-contracts/contract-send [:job job-address] :create-invoice [invoice-amounts-all "0x0"] {:from worker}))
                   invoice-event (<! (smart-contracts/contract-event-in-tx :ethlance :InvoiceCreated invoice-tx))
                   invoice-id (int (:invoice-id invoice-event))]

               ; 5. Worker raises dispute
               (let [_ (<! (smart-contracts/contract-send [:job job-address] :raise-dispute [invoice-id "0x0"] {:from worker}))
                     withdraw-tx (<! (smart-contracts/contract-send [:job job-address] :withdraw-funds [[employer-contribution-a]] {:from employer}))]
                 (is (nil? withdraw-tx) "Withdraw not allowed with unresolved dispute"))

               (let [worker-balance-before (js/parseInt (<? (smart-contracts/contract-call :token :balance-of [worker])))
                     employer-balance-before (js/parseInt (<? (smart-contracts/contract-call :token :balance-of [employer])))
                     sponsor-balance-before (js/parseInt (<? (smart-contracts/contract-call :token :balance-of [sponsor])))

                     worker-withdraw-amounts [(merge employer-contribution-a {:value 6})]
                     employer-withdraw-amounts [(merge employer-contribution-a {:value 2})]
                     sponsor-withdraw-amounts [(merge employer-contribution-a {:value 1})]
                     dispute-resolve-by-rando-tx (<! (smart-contracts/contract-send [:job job-address] :resolve-dispute [invoice-id worker-withdraw-amounts "0x0"] {:from worker}))
                     _ (<! (smart-contracts/contract-send [:job job-address] :resolve-dispute [invoice-id worker-withdraw-amounts "0x0"] {:from arbiter}))
                     withdraw-employer-tx (<! (smart-contracts/contract-send [:job job-address] :withdraw-funds [employer-withdraw-amounts] {:from employer}))
                     withdraw-sponsor-tx (<! (smart-contracts/contract-send [:job job-address] :withdraw-funds [sponsor-withdraw-amounts] {:from sponsor}))

                     withdraw-employer-event (<! (smart-contracts/contract-event-in-tx :ethlance :FundsWithdrawn withdraw-employer-tx))
                     withdraw-sponsor-event (<! (smart-contracts/contract-event-in-tx :ethlance :FundsWithdrawn withdraw-sponsor-tx))

                     worker-balance (- (js/parseInt (<? (smart-contracts/contract-call :token :balance-of [worker]))) worker-balance-before)
                     employer-balance (- (js/parseInt (<? (smart-contracts/contract-call :token :balance-of [employer]))) employer-balance-before)
                     sponsor-balance (- (js/parseInt (<? (smart-contracts/contract-call :token :balance-of [sponsor]))) sponsor-balance-before)]
                 (is (= (:withdrawer withdraw-employer-event) employer))
                 (is (= (:withdrawer withdraw-sponsor-event) sponsor))
                 (is (nil? dispute-resolve-by-rando-tx) "Only invited arbiters can resolve disputes")
                 (is (= worker-balance 6))
                 (is (= employer-balance 2))
                 (is (= sponsor-balance 1)))
               (done))))))

(deftest withdraw-overdraw-checks
  (testing "Checks for not withdrawing too much"
    (async done
           (go
             (let [[_owner employer worker sponsor arbiter] (<! (web3-eth/accounts @web3))
                   contribution-a-amount 10 ; employer initial contribution
                   contribution-b-amount 6

                   ; 1. Prepare a job (10 ERC from employer, 6 from sponsor)
                   job-data (<! (create-initialized-job [(partial fund-in-erc20 employer contribution-a-amount)] :arbiters [arbiter]))
                   job-address (:job job-data)
                   _ (<! (smart-contracts/contract-send [:job job-address] :add-candidate [worker "0x0"] {:from employer}))
                   employer-contribution-a (offer-from-job-data job-data :erc20)
                   [extra-funding-from-sponsor _extra] (<! (fund-in-erc20 sponsor contribution-b-amount [] {} :approve-for job-address))
                   _ (<! (smart-contracts/contract-send [:job job-address] :add-funds [extra-funding-from-sponsor] {:from sponsor}))

                   ; 2. Invoice for 7 out of the 16 and pay it out
                   invoice-amounts [(merge employer-contribution-a {:value 7})]
                   invoice-tx (<? (smart-contracts/contract-send [:job job-address] :create-invoice [invoice-amounts "0x0"] {:from worker}))
                   invoice-event (<! (smart-contracts/contract-event-in-tx :ethlance :InvoiceCreated invoice-tx))
                   invoice-id (int (:invoice-id invoice-event))
                   tx-pay-invoice (<! (smart-contracts/contract-send [:job job-address] :pay-invoice [invoice-id "0x0"] {:from employer}))
                   event-pay-invoice (<! (smart-contracts/contract-event-in-tx :ethlance :InvoicePaid tx-pay-invoice))]
               (is (= (int (:invoice-id event-pay-invoice)) invoice-id))

               ; 3. Employer intents to withdraw original sum of 10 (fails, as 7 of 16 are already paid out with only 9 left)
               (let [withdraw-tx (<! (smart-contracts/contract-send
                                       [:job job-address] :withdraw-funds [[employer-contribution-a]] {:from employer :output :receipt-or-error}))]
                 (is (tx-reverted-with withdraw-tx #"Insufficient ERC20 tokens")))

               ; 4. Employer & sponsor withdraw according to `Job#maxWithdrawableAmounts` (succeeds)
               ; 5. Job contract will be left empty
               (let [employer-balance-before (int (<? (smart-contracts/contract-call :token :balance-of [employer])))
                     sponsor-balance-before (js/parseInt (<? (smart-contracts/contract-call :token :balance-of [sponsor])))

                     sponsor-withdraw-amounts (<! (job-contract/max-withdrawable-amounts job-address sponsor))
                     _ (<! (smart-contracts/contract-send [:job job-address] :withdraw-funds [sponsor-withdraw-amounts] {:from sponsor}))
                     employer-withdraw-amounts (<! (job-contract/max-withdrawable-amounts job-address employer))
                     _ (<! (smart-contracts/contract-send [:job job-address] :withdraw-funds [employer-withdraw-amounts] {:from employer}))

                     employer-balance-after (int (<? (smart-contracts/contract-call :token :balance-of [employer])))
                     employer-balance-change (- employer-balance-after employer-balance-before)
                     sponsor-balance (- (int (<? (smart-contracts/contract-call :token :balance-of [sponsor]))) sponsor-balance-before)]
                 (is (= sponsor-balance 6))
                 (is (= employer-balance-change 3))) ; sponsor withdrew first, got 6
               (done))))))

(deftest add-funds-and-pay-invoice
  ; Scenario:
  ;   1. Job gets created & funded with 2 ERC20
  ;   2. Worker raises an invoice for larger amount 5 ERC20
  ;   3. Employer tries to withdraw and fails because there is an unpaid invoice
  ;   4. Employer adds 8 more ERC20 tokens (now 10 total) & pays invoice in one step (expect success)
  ;   5. Employer withdraws remaining ERC20 tokens (based on Job#maxWithdrawableAmounts)
  ;   6. Result: job is left with 0 ERC20, employer gets 3 ERC20
  (testing "when invoice amount is > funds, employer can add funds & pay invoice in 1 step"
    (async done
           (go
             (let [[_owner employer worker] (<! (web3-eth/accounts @web3))
                   employer-starting-balance (int (<! (smart-contracts/contract-call :token :balance-of [employer])))
                   erc-20-amount 2
                   additional-amount 3
                   ;   1. Job gets created & funded with 2 ERC20
                   job-data (<! (create-initialized-job [(partial fund-in-erc20 employer erc-20-amount)]))
                   job-address (:job job-data)
                   _ (<! (smart-contracts/contract-send [:job job-address] :add-candidate [worker "0x0"] {:from employer}))

                   ;   2. Worker raises an invoice for larger amount 5 ERC20 (3 extra)
                   invoice-amounts [(merge (offer-from-job-data job-data :erc20) {:value (+ erc-20-amount additional-amount)})]
                   tx-receipt (<? (smart-contracts/contract-send [:job job-address] :create-invoice [invoice-amounts "0x0"] {:from worker}))
                   invoice-event (<! (smart-contracts/contract-event-in-tx :ethlance :InvoiceCreated tx-receipt))
                   invoice-id (int (:invoice-id invoice-event))]

               ;   3. Employer tries to withdraw and fails because there is an unpaid invoice
               (let [employer-withdraw-amounts [(offer-from-job-data job-data :erc20)]
                     withdraw-tx (<! (smart-contracts/contract-send
                                       [:job job-address] :withdraw-funds [employer-withdraw-amounts] {:from employer :output :receipt-or-error}))]
                 (is (tx-reverted-with withdraw-tx #"Can't withdraw whilst there are unpaid invoices")))

               ; Try to pay (will fail because there are insufficient ERC20 funds)
               (let [pay-invoice-tx (<! (smart-contracts/contract-send
                                          [:job job-address] :pay-invoice [invoice-id "0x0"] {:from employer :output :receipt-or-error}))]
                 (is (tx-reverted-with pay-invoice-tx #"Insufficient ERC20 tokens")))

               ;   4. Employer adds 3+5=8 more ERC20 tokens (now 10 total) & pays invoice in one step (expect success)
               (let [leftover-tokens 5 ; These will stay in ERC20 contract as Job only takes ownershipe of minimum amount needed
                     to-approve (+ additional-amount leftover-tokens)
                     _ (<! (smart-contracts/contract-send :token :mint [employer to-approve]))
                     _ (<! (smart-contracts/contract-send :token :approve [job-address to-approve] {:from employer}))

                     worker-token-balance-before (<? (smart-contracts/contract-call :token :balance-of [worker]))
                     extra-funds [(merge (first invoice-amounts) {:value to-approve})]
                     tx-pay-invoice (<! (smart-contracts/contract-send [:job job-address] :add-funds-and-pay-invoice [extra-funds invoice-id "0x0"] {:from employer}))
                     event-pay-invoice (<! (smart-contracts/contract-event-in-tx :ethlance :InvoicePaid tx-pay-invoice))
                     worker-token-balance-after (<? (smart-contracts/contract-call :token :balance-of [worker]))
                     worker-token-change (bn/- (bn/number worker-token-balance-after) (bn/number worker-token-balance-before))
                     max-withdrawable-amounts (map contract-constants/token-value-vec->map
                                                        (js->clj (<! (smart-contracts/contract-call [:job job-address] :max-withdrawable-amounts [employer] {:from employer}))))

                     ;   5. Employer withdraws remaining ERC20 tokens (based on Job#maxWithdrawableAmounts)
                     _ (<! (smart-contracts/contract-send [:job job-address] :withdraw-funds [max-withdrawable-amounts] {:from employer}))
                     job-final-balance (<! (smart-contracts/contract-call :token :balance-of [job-address]))
                     employer-erc20-balance-diff (- (<! (smart-contracts/contract-call :token :balance-of [employer])) employer-starting-balance)]
                 ;   6. Result: job is left with 0 ERC20, employer gets 3 ERC20
                 (is (= (int (:invoice-id event-pay-invoice)) invoice-id))
                 (is (= (int employer-erc20-balance-diff) 5))
                 (is (= (int job-final-balance) 0) "After withdrawing ")
                 (is (= worker-token-change 5)))
               (done))))))
