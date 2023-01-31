(ns tests.helpers.contract-funding
  (:require [bignumber.core :as bn]
            [cljs-web3-next.eth :as web3-eth]
            [district.server.web3 :refer [web3]]
            [ethlance.server.contract.ethlance :as ethlance]
            [ethlance.shared.contract-constants :as contract-constants]
            [ethlance.shared.smart-contracts-dev :as addresses]
            [district.server.smart-contracts :as smart-contracts]
            [cljs.core.async :refer [<! go]]
            [cljs.core.async.impl.channels :refer [ManyToManyChannel]]
            [district.shared.async-helpers :refer [<?]]))

(defn eth->wei [eth-amount]
  (let [wei-in-eth (bn/number 10e17)]
    (bn/number (* wei-in-eth (bn/number eth-amount)))))

(defn wei->eth [wei-amount]
  (let [wei-in-eth (bn/number 10e17)]
    (/ (bn/number wei-amount) wei-in-eth)))

(defn diff-percent [first second]
  (/ (Math/abs (- second first)) second))

(defn approx=
  "Returns true if second differs from first less than diff-percent (0..1)"
  [percent first second]
  (< (diff-percent first second) percent))

(defn fund-in-eth
  "Produces data 2 structures that can be used as input for `ethlance/create-job`

   TIP: use with currying provide the eth-amount, so it can be used with 2 args in the
   create-initialized-job reduce function"
  ([eth-amount] (fund-in-eth eth-amount [] {}))
  ([eth-amount offered-values create-job-opts]
   (let [amount-in-wei (str (eth->wei eth-amount)) ; FIXME: why web3-utils/eth->wei returns nil
         offered-token-type (contract-constants/token-type :eth)
         placeholder-address "0x1111111111111111111111111111111111111111"
         not-used-for-erc20 0
         eth-offered-value {:token
                            {:tokenContract {:tokenType offered-token-type
                                             :tokenAddress placeholder-address}
                             :tokenId not-used-for-erc20} :value amount-in-wei}
         additional-opts {:value amount-in-wei}]
     [(conj offered-values eth-offered-value) (merge create-job-opts additional-opts)])))

(defn fund-in-erc20
  "Mints ERC20 TestToken for recipient and approves them for Ethlance (or address at :approve-for).
   Returns 2 data structures to be used for ethlance/create-job"
  ([recipient funding-amount]
   (fund-in-erc20 recipient funding-amount [] {}))

  ([recipient funding-amount offered-values create-job-opts & {approve-for :approve-for :or {approve-for :ethlance}}]
   (go
     (let [ethlance-addr (smart-contracts/contract-address :ethlance)
           approve-addr (if (= :ethlance approve-for) ethlance-addr approve-for)
           _ (<? (smart-contracts/contract-send :token :mint [recipient funding-amount]))
           test-token-address (smart-contracts/contract-address :token)
           not-used-for-erc20 0
           offered-token-type (contract-constants/token-type :erc20)
           erc-20-value {:token
                         {:tokenContract {:tokenType offered-token-type :tokenAddress test-token-address}
                          :tokenId not-used-for-erc20} :value funding-amount}]
       (smart-contracts/contract-send :token :approve [approve-addr funding-amount] {:from recipient})
       [(conj offered-values erc-20-value) create-job-opts]))))

(defn fund-in-erc721
  [recipient offered-values create-job-opts & {approval :approval :or {approval true}}]
  (go
    (let [ethlance-addr (smart-contracts/contract-address :ethlance)
          receipt (<? (smart-contracts/contract-send :test-nft :award-item [recipient]))
          token-id (. (get-in receipt [:events :Transfer :return-values]) -tokenId)
          test-token-address (smart-contracts/contract-address :test-nft)
          offered-token-type (contract-constants/token-type :erc721)
          token-offer {:token
                       {:tokenContract {:tokenType offered-token-type :tokenAddress test-token-address}
                        :tokenId token-id} :value 1}]
      (if approval (<? (smart-contracts/contract-send :test-nft :approve [ethlance-addr token-id] {:from recipient})) nil)
      [(conj offered-values token-offer) create-job-opts])))

(defn fund-in-erc1155
  ([recipient amount] (fund-in-erc1155 recipient amount [] {} :approval true))
  ([recipient amount offered-values create-job-opts & {approval :approval :or {approval true}}]
   (go
     (let [ethlance-addr (smart-contracts/contract-address :ethlance)
           receipt (<? (smart-contracts/contract-send :test-multi-token :award-item [recipient amount]))
           token-id (. (get-in receipt [:events :Transfer-single :return-values]) -id)
           token-amount (. (get-in receipt [:events :Transfer-single :return-values]) -value)
           test-token-address (smart-contracts/contract-address :test-multi-token)
           offered-token-type (contract-constants/token-type :erc1155)
           token-offer {:token
                        {:tokenContract {:tokenType offered-token-type :tokenAddress test-token-address}
                         :tokenId token-id} :value (int token-amount)}]
       (if approval
         (<? (smart-contracts/contract-send :test-multi-token :set-approval-for-all [ethlance-addr true] {:from recipient}))
         nil)
       [(conj offered-values token-offer) create-job-opts]))))

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

(defn create-initialized-job
  "Creates new Job contract and initializes Ethlance contract with it.
   Also adds initial-balance-in-wei (currently 0.05 ETH) to the balance.

   NB! Using fund-in-eth will take ETH from `employer` account (web3 2nd account)
       The rest of the token funding functions(fund-in-erc20 fund-in-erc721 fund-in-erc1155)
       accept `recipient` param"
  ([] (create-initialized-job []))

  ([funding-functions & {job-type :job-type
                         arbiters :arbiters
                         :or {arbiters []
                              job-type (contract-constants/job-type :gig)}}]
   (go
      (let [[_owner employer worker] (<! (web3-eth/accounts @web3))
            ipfs-data "0x0"
            job-impl-address (get-in addresses/smart-contracts [:job :address])
            real-vals (<! (reduce collect-from-funding-funcs [[] {}] funding-functions))
            [offered-values additional-opts] real-vals
            _ (<! (ethlance/initialize job-impl-address))
            tx-receipt (<! (ethlance/create-job employer
                                                  offered-values
                                                  job-type
                                                  arbiters
                                                  ipfs-data
                                                  additional-opts))
            create-job-event (<! (smart-contracts/contract-event-in-tx :ethlance :JobCreated tx-receipt))
            created-job-address (:job create-job-event)]
        (if (= nil create-job-event) (throw (str "Job creation failed for values" offered-values)) nil)
        {:ethlance (smart-contracts/contract-address :ethlance)
         :job created-job-address
         :employer employer
         :worker worker
         :offered-values offered-values
         :tx-receipt tx-receipt}))))
