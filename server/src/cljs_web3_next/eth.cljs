(ns cljs-web3-next.eth
  (:refer-clojure :exclude [filter])
  (:require [cljs-web3-next.helpers :as web3-helpers]
            [taoensso.timbre :as log]
            [oops.core :refer [ocall ocall+ oget oget+ oset! oapply oapply+]]))

(defn is-listening? [provider & [callback]]
  (oapply+ provider "eth.net.isListening" (remove nil? [callback])))

(defn get-node-info [provider & [callback]]
  (oapply+ provider "eth.getNodeInfo" (remove nil? [callback])))

(defn connected? [provider & [callback]]
  (is-listening? provider (fn [err listening?]
                            (println ">>> cljs-web3-next.eth/connected? OUTER " err listening?)
                            (if (nil? err)
                              (callback nil listening?)
                              (get-node-info provider (fn [err info]
                                                        (println ">>> cljs-web3-next.eth/connected? INNER " err info)
                                                        (if (nil? err)
                                                          (callback nil true)
                                                          (callback err nil))))))))

(defn contract-at [provider abi address]
  (new (aget provider "eth" "Contract") abi address))

(defn get-transaction-receipt [provider tx-hash & [callback]]
  (oapply+ provider "eth.getTransactionReceipt" (remove nil? [tx-hash callback])))

(defn accounts [provider & [callback]]
  (oapply+ provider "eth.getAccounts" (remove nil? [callback])))

;; recheck conflicts with updated fn
;; (defn get-balance [provider address]
;;   (ocall provider "eth" "getBalance" address))

(defn get-chain-id [provider & [callback]]
  (oapply+ provider "eth.getChainId" (remove nil? [callback])))

(defn get-block-number [provider & [callback]]
  (oapply+ provider "eth.getBlockNumber" (remove nil? [callback])))

(defn get-block [provider block-hash-or-number return-transactions? & [callback]]
  (oapply+ provider "eth.getBlock" (remove nil? [block-hash-or-number return-transactions? callback])))

;; not working
(defn encode-abi [contract-instance method args]
  (js-invoke (oapply+ (oget contract-instance "methods") (web3-helpers/camel-case (name method)) (clj->js args)) "encodeABI"))

(defn contract-call
  ([contract-instance method args opts]
   (contract-call contract-instance method args opts (fn [err res])))
  ([contract-instance method args opts callback]
   (let [web3-contract-method (oapply+ (oget contract-instance "methods")
                                       (if (string? method)
                                         method
                                         (web3-helpers/camel-case (name method)))
                                       (clj->js args))]
     (ocall web3-contract-method "call" (clj->js opts) callback))))

(defn contract-send [contract-instance method args opts & [callback]]
  (let [callback (or callback (fn [_err _res]))]
    (ocall (oapply+ (oget contract-instance "methods") (web3-helpers/camel-case (name method)) (clj->js args)) "send" (clj->js opts) callback)))

(defn subscribe-events [contract-instance event opts & [callback]]
  (log/info ">>> cljs-web3-next.eth/subscribe-events" {:contract-instance contract-instance :event event :opts opts})
  (oapply+ (oget contract-instance "events")
           (web3-helpers/camel-case (name event)) ; https://web3js.readthedocs.io/en/v1.7.1/web3-eth-contract.html#contract-events
           [(web3-helpers/cljkk->js opts) callback]))

(defn subscribe-logs [provider opts & [callback]]
  (js-invoke (aget provider "eth") "subscribe" "logs" (web3-helpers/cljkk->js opts) callback))

(defn subscribe-blocks [provider & [callback]]
  (js-invoke (aget provider "eth") "subscribe" "newBlockHeaders" callback))

(defn decode-log [provider abi data topics]
  (ocall+ provider "eth.abi.decodeLog" (clj->js abi) data (clj->js topics)))

(defn unsubscribe [subscription & [callback]]
  (ocall subscription "unsubscribe" callback))

(defn clear-subscriptions [provider]
  (ocall provider "eth" "clearSubscriptions"))

(defn get-past-events [contract-instance event opts & [callback]]
  (ocall contract-instance "getPastEvents" (web3-helpers/camel-case (name event)) (web3-helpers/cljkk->js opts) callback))

(defn get-past-logs [provider opts & [callback]]
  (js-invoke (oget provider "eth") "getPastLogs" (web3-helpers/cljkk->js opts) callback))

(defn on [event-emitter event callback]
  (ocall event-emitter "on" (name event) callback))

(defn eth
  "Gets eth object from web3-instance.

  Parameter:
  web3 - web3 instance"
  [provider]
  (oget provider "eth"))

(defn iban
  "Gets iban object from web3-instance.

  Parameter:
  web3 - web3 instance"
  [provider]
  (oget provider "Iban"))


;; legacy


(defn default-account
  "Gets the default address that is used for the following methods (optionally
  you can overwrite it by specifying the :from key in their options map):

  - `send-transaction!`
  - `call!`

  Parameters:
  web3 - web3 instance

  Returns the default address HEX string.

  Example:
  user> `(default-account web3-instance)`
  \"0x85d85715218895ae964a750d9a92f13a8951de3d\""
  [provider]
  (oget provider "eth" "defaultAccount"))

(defn set-default-account!
  "Sets the default address that is used for the following methods (optionally
  you can overwrite it by specifying the :from key in their options map):

  - `send-transaction!`
  - `call!`

  Parameters:
  web3    - web3 instance
  hex-str - Any 20 bytes address you own, or where you have the private key for


  Returns a 20 bytes HEX string representing the currently set address.

  Example:
  user> (set-default-account! web3-instance
                              \"0x85d85715218895ae964a750d9a92f13a8951de3d\")
  \"0x85d85715218895ae964a750d9a92f13a8951de3d\""
  [provider hex-str]
  (oset! provider "eth" "defaultAccount" hex-str))

(defn default-block
  "This default block is used for the following methods (optionally you can
  override it by passing the default-block parameter):

  - `get-balance`
  - `get-code`
  - `get-transactionCount`
  - `get-storageAt`
  - `call`
  - `contract-call`
  - `estimate-gas`

  Parameters:
  web3 - web3 instance

  Returns one of:
  - a block number
  - \"earliest\", the genisis block
  - \"latest\", the latest block (current head of the blockchain)
  - \"pending\", the currently mined block (including pending transactions)

  Example:
  user> `(default-block web3-instance)`
  \"latest\""
  [provider]
  (oget provider "eth" "defaultBlock"))


(defn set-default-block!
  "Sets default block that is used for the following methods (optionally you can
  override it by passing the default-block parameter):

  - `get-balance`
  - `get-code`
  - `get-transactionCount`
  - `get-storageAt`
  - `call`
  - `contract-call`
  - `estimate-gas`

  Parameters:
  web3  - web3 instance
  block - one of:
            - a block number
            - \"earliest\", the genisis block
            - \"latest\", the latest block (current head of the blockchain)
            - \"pending\", the currently mined block (including pending
              transactions)

  Example:
  user> `(set-default-block! web3-instance \"earliest\")`
  \"earliest\""
  [provider block]
  (oset! provider "eth" "defaultBlock" block))

;;DEPRECATED partially
(defn syncing
  "This property is read only and returns the either a sync object, when the
  node is syncing or false.

  Parameters:
  web3        - web3 instance
  callback-fn - callback with two parameters, error and result

  Returns a sync object as follows, when the node is currently syncing or false:
  - startingBlock: The block number where the sync started.
  - currentBlock:  The block number where at which block the node currently
                   synced to already.
  - highestBlock:  The estimated block number to sync to.

  Example:
  user> `(syncing web3-instance (fn [err res] (when-not err (println res))))`
  nil
  user> `false`"
  [provider]
  (ocall provider "eth" "isSyncing"))

(def syncing? syncing)

(defn coinbase
  "This property is read only and returns the coinbase address where the mining
  rewards go to.

  Parameters:
  web3 - web3 instance

  Returns a string representing the coinbase address of the client.

  Example:
  user> `(coinbase web3-instance)`
  \"0x85d85715218895ae964a750d9a92f13a8951de3d\""
  [provider]
  (ocall provider "eth" "getCoinbase"))

(defn mining?
  "This property is read only and says whether the node is mining or not.

  Parameters:
  web3 - web3 instance

  Returns a boolean: true if the client is mining, otherwise false.

  Example:
  `(mining? web3-instance (fn [err res] (when-not err (println res))))`
  nil
  user> `false`"
  [provider]
  (ocall provider "eth" "isMining"))


(defn hashrate
  "This property is read only and returns the number of hashes per second that
  the node is mining with.

  Parameters:
  web3 - web3 instance

  Returns a number representing the hashes per second.

  user> `(hashrate web3-instance (fn [err res] (when-not err (println res))))`
  nil
  user> 0
  "
  [provider]
  (ocall provider "eth" "getHashrate"))


(defn gas-price
  "This property is read only and returns the current gas price. The gas price
  is determined by the x latest blocks median gas price.

  Parameters:
  web3        - web3 instance
  callback-fn - callback with two parameters, error and result

  Returns a BigNumber instance of the current gas price in wei.

  Example:
  user> `(gas-price web3-instance (fn [err res] (when-not err (println res))))`
  nil
  user> #object[e 90000000000]"
  [provider]
  (ocall provider "eth" "getGasPrice"))

(defn block-number
  "This property is read only and returns the current block number.

  Parameters:
  web3        - web3 instance
  callback-fn - callback with two parameters, error and result

  Returns the number of the most recent block.

  Example:
  `(block-number web3-instance
                 (fn [err res] (when-not err (println res))))`
  nil
  user> `1783426`"
  [provider]
  (get-block-number provider))

(defn get-balance
  "Get the balance of an address at a given block.

  Parameters:
  web3          - web3 instance
  address       - The address to get the balance of.
  default-block - If you pass this parameter it will not use the default block
                  set with set-default-block.
  callback-fn   - callback with two parameters, error and result

  Returns a String of the current balance for the given address in
  wei.

  Example:
  user> `(get-balance web3-instance
                      \"0x85d85715218895ae964a750d9a92f13a8951de3d\"
                      \"latest\"
                      (fn [err res] (when-not err (println res))))`
  nil
  user> \"1729597111000000000\""
  [provider & [address default-block :as args]]
  (oapply+ provider "eth" "getBalance" args))

;; recheck currying here
(defn stop-watching!
  "Stops and uninstalls the filter.

  Arguments:
  filter - the filter to stop"
  [filter & args]
  (unsubscribe filter (first args)))


;; DEPRECATED
;; Use (encode-abi ) instead
(defn contract-get-data
  "Gets binary data of a contract method call.

  Use the kebab-cases version of the original method.
  E.g., function fooBar() can be addressed with :foo-bar.

  Parameters:
  contract-instance - an instance of the contract (obtained via `contract` or
                      `contract-at`)
  method            - the kebab-cased version of the method
  args              - arguments to the method

  Example:
  user> `(web3-eth/contract-call ContractInstance :multiply 5)`
  25"
  [contract-instance method & args]
  (encode-abi contract-instance method args))


;; DEPRECATED
;; these functions existed in 0.* but
;; were interfaced to empty implementations/broken/soft-deprecated
(defn get-compile
  "Gets compile object from web3-instance.

  Parameter:
  web3 - web3 instance"
  [web3]
  nil)

(defn namereg
  "Returns GlobalRegistrar object.

  See https://github.com/ethereum/web3.js/blob/master/example/namereg.html
  for an example in JavaScript."
  [web3]
  nil)

(defn get-compilers
  "Compiling features being deprecated https://github.com/ethereum/EIPs/issues/209"
  [web3 & args]
  nil)


(defn compile-solidity
  "Compiling features being deprecated https://github.com/ethereum/EIPs/issues/209"
  [web3 & [source-string :as args]]
  nil)


(defn compile-lll
  "Compiling features being deprecated https://github.com/ethereum/EIPs/issues/209"
  [web3 & [source-string :as args]]
  nil)


(defn compile-serpent
  "Compiling features being deprecated https://github.com/ethereum/EIPs/issues/209"
  [web3 & [source-string :as args]]
  nil)

(defn register
  "(Not Implemented yet) Registers the given address to be included in
  `accounts`. This allows non-private-key owned accounts to be associated
  as an owned account (e.g., contract wallets).

  Parameters:
  web3        - web3 instance
  address     - string representing the address
  callback-fn - callback with two parameters, error and result."
  [web3 address]
  nil)


(defn unregister
  "(Not Implemented yet) Unregisters a given address.

  Parameters:
  web3        - web3 instance
  address     - string representing the address
  callback-fn - callback with two parameters, error and result."
  [web3 address]
  nil)

(defn get-storage-at
  "Get the storage at a specific position of an address.

  Parameters:
  web3          - web3 instance
  address       - The address to get the storage from.
  position      - The index position of the storage.
  default-block - If you pass this parameter it will not use the default block
                  set with web3.eth.defaultBlock.
  callback-fn   - callback with two parameters, error and result

  Returns the value in storage at the given position.

  Example:
  user> `(get-storage-at web3-instance
                         \"0x85d85715218895ae964a750d9a92f13a8951de3d\"
                         0
                         \"latest\"
                         (fn [err res] (when-not err (println res))))`
  nil
  user> \"0x0000000000000000000000000000000000000000000000000000000000000000\" "
  [web3 & [address position default-block :as args]]
  (oapply+ (oget web3 "eth") "getStorageAt" args))

(defn get-code
  "Get the code at a specific address.

  Parameters:
  web3          - web3 instance
  address       - The address to get the code from.
  default-block - If you pass this parameter it will not use the default block set
                  with `get-default-block!`.
  callback-fn   - callback with two parameters, error and result

  Returns the data at given address HEX string.

  Example:
  user> (get-code web3-instance
                  \"0x85d85715218895ae964a750d9a92f13a8951de3d
                  0
                  \"latest\"
                  (fn [err res] (when-not err (println res))))
  nil
  user> `0x`
  "
  [web3 & [address default-block :as args]]
  (oapply+ (eth web3) "getCode" args))

(defn get-block-transaction-count
  "Returns the number of transaction in a given block.

  Parameters
  web3                 - web3 instance
  block-hash-or-number - The block number or hash. Or the string \"earliest\",
                         \"latest\" or \"pending\" as in the default block
                         parameter.
  callback-fn          - callback with two parameters, error and result

  Example:
  user> `(get-block-transaction-count
           web3-instance
           0
           (fn [err res] (when-not err (println res))))`
  nil
  user> 0"
  [web3 & [block-hash-or-number :as args]]
  (oapply+ (eth web3) "getBlockTransactionCount" args))

(defn get-uncle
  "Returns a blocks uncle by a given uncle index position.
  Parameters

  Parameters:
  web3                        - web3 instance
  block-hash-or-number        - The block number or hash. Or the string
                                \"earliest\", \"latest\" or \"pending\" as in
                                the default block parameter
  uncle-number                - The index position of the uncle
  return-transaction-objects? - If true, the returned block will contain all
                                transactions as objects, if false it will only
                                contains the transaction hashes
  default-block               - If you pass this parameter it will not use the
                                default block set with (set-default-block)
  callback-fn                 - callback with two parameters, error and result

  Returns the returned uncle. For a return value see `(get-block)`.

  Note: An uncle doesn't contain individual transactions."
  [web3 & [block-hash-or-number uncle-number return-transaction-objects? :as args]]
  (oapply+ (eth web3) "getUncle" args))

(defn get-transaction
 "Returns a transaction matching the given transaction hash.

  Parameters:
  web3             - web3 instance
  transaction-hash - The transaction hash.
  callback-fn      - callback with two parameters, error and result

  Returns a transaction object its hash transaction-hash:

  - hash: String, 32 Bytes - hash of the transaction.
  - nonce: Number - the number of transactions made by the sender prior to this
    one.
  - block-hash: String, 32 Bytes - hash of the block where this transaction was
                                   in. null when its pending.
  - block-number: Number - block number where this transaction was in. null when
                           its pending.
  - transaction-index: Number - integer of the transactions index position in the
                                block. null when its pending.
  - from: String, 20 Bytes - address of the sender.
  - to: String, 20 Bytes - address of the receiver. null when its a contract
                           creation transaction.
  - value: BigNumber - value transferred in Wei.
  - gas-price: BigNumber - gas price provided by the sender in Wei.
  - gas: Number - gas provided by the sender.
  - input: String - the data sent along with the transaction.

  Example:
  user> `(get-transaction
           web3-instance
           \"0x...\"
           (fn [err res] (when-not err (println res))))`
  nil
  user> {:r 0x...
         :v 0x2a
         :hash 0xf...
         :transaction-index 3 ...
         (...)
         :to 0x...}"
  [web3 & [transaction-hash :as args]]
  (oapply+ (eth web3) "getTransaction" args))

(defn get-transaction-from-block
  "Returns a transaction based on a block hash or number and the transactions
  index position.

  Parameters:
  web3                 - web3 instance
  block-hash-or-number - A block number or hash. Or the string \"earliest\",
                         \"latest\" or \"pending\" as in the default block
                         parameter.
  index                - The transactions index position.
  callback-fn          - callback with two parameters, error and result
  Number               - The transactions index position.

  Returns a transaction object, see `(get-transaction)`

  Example:
  user> `(get-transaction-from-block
           web3-instance
           1799402
           0
           (fn [err res] (when-not err (println res))))`
  nil
  user> {:r 0x...
         :v 0x2a
         :hash 0xf...
         :transaction-index 0 ...
         (...)
         :to 0x...}"
  [web3 & [block-hash-or-number index :as args]]
  (oapply+ (eth web3) "getTransactionFromBlock" args))

(defn get-transaction-count
  "Get the numbers of transactions sent from this address.

  Parameters:
  web3          - web3 instance
  address       - The address to get the numbers of transactions from.
  default-block - If you pass this parameter it will not use the default block
                  set with set-default-block.
  callback-fn   - callback with two parameters, error and result

  Returns the number of transactions sent from the given address.

  Example:
  user> `(get-transaction-count web3-instance \"0x8\"
           (fn [err res] (when-not err (println res))))`
  nil
  user> 16"
  [web3 & [address default-block :as args]]
  (oapply+ (eth web3) "getTransactionCount" args))

(defn contract
  "Important - callback has been deprecated
  Creates an *abstract* contract object for a solidity contract, which can be used to
  initiate contracts on an address.

  Parameters:
  web3          - web3 instance
  abi           - ABI array with descriptions of functions and events of
                  the contract

  Returns a contract object."
  [web3 & [abi :as args]]
  (new (aget web3 "eth" "Contract") abi))

(defn contract-new
  "Deploy a contract asynchronous from a Solidity file.

  Parameters:
  web3             - web3 instance
  abi              - ABI array with descriptions of functions and events of
                     the contract
  options          - map that contains
    - :data the BIN of the contract
    - :arguments list of arguments for the contract constructor
  tx-opts - map that contains
    - :gas - max gas to use
    - :from account to use
  callback-fn      - callback with two parameters, error and contract.
                     Use (aget instance \"options\" \"address\") to
                     obtain the address.

  Example:
  `(contract-new web3-instance
                 abi
                 {:data bin
                  :arguments [1 2 3]}
                 {:from \"0x..\"
                  :gas  4000000}
                 (fn [err contract]
                   (if-not err
                    (let [address (aget contract \"options\" \"address\")]
                      (do-something-with-contract contract)
                      (do-something-with-address address))
                    (println \"error deploying contract\" err))))`
   nil"
  [web3 abi options tx-opts & [callback]]
  (let [noop-callback (fn [_err _res])
        callback (or callback noop-callback)]
    (try
      (let [contract-obj (oapply+ (contract web3 abi) "deploy" [(clj->js options)])
            transaction (ocall contract-obj "send" (clj->js tx-opts))]
        (.then transaction
          (fn [res] (callback nil res))
          (fn [err] (callback err nil))))
      (catch :default err (callback err nil)))))

(defn send-transaction!
  "Sends a transaction to the network.

  Parameters:
  web3               - web3 instance
  transaction-object - The transaction object to send:

    :from: String - The address for the sending account. Uses the
                    `default-account` property, if not specified.

    :to: String   - (optional) The destination address of the message, left
                               undefined for a contract-creation
                               transaction.

    :value        - (optional) The value transferred for the transaction in
                               Wei, also the endowment if it's a
                               contract-creation transaction.

    :gas:         - (optional, default: To-Be-Determined) The amount of gas
                    to use for the transaction (unused gas is refunded).
    :gas-price:   - (optional, default: To-Be-Determined) The price of gas
                    for this transaction in wei, defaults to the mean network
                    gas price.
    :data:        - (optional) Either a byte string containing the associated
                    data of the message, or in the case of a contract-creation
                    transaction, the initialisation code.
    :nonce:       - (optional) Integer of a nonce. This allows to overwrite your
                               own pending transactions that use the same nonce.
  callback-fn   - callback with two parameters, error and result, where result
                  is the transaction hash

  Returns the 32 Bytes transaction hash as HEX string.

  If the transaction was a contract creation use `(get-transaction-receipt)` to
  get the contract address, after the transaction was mined.

  Example:
  user> (send-transaction! web3-instance {:to \"0x..\"}
          (fn [err res] (when-not err (println res))))
  nil
  user> 0x..."
  [web3 transaction-object & [callback]]
  (oapply+ (eth web3) "sendTransaction" (remove nil? [(clj->js transaction-object) callback])))


(defn send-raw-transaction!
  "Sends an already signed transaction. For example can be signed using:
  https://github.com/SilentCicero/ethereumjs-accounts

  Parameters:
  web3                    - web3 instance
  signed-transaction-data - Signed transaction data in HEX format

  callback-fn             - callback with two parameters, error and result

  Returns the 32 Bytes transaction hash as HEX string.

  If the transaction was a contract creation use `(get-transaction-receipt)`
  to get the contract address, after the transaction was mined.

  See https://github.com/ethereum/wiki/wiki/JavaScript-API#example-46 for a
  JavaScript example."
  [web3 & [signed-transaction-data :as args]]
  (oapply+ (eth web3) "sendSignedTransaction" args))

(def send-signed-transaction send-raw-transaction!)

(defn send-iban-transaction!
  "Sends IBAN transaction from user account to destination IBAN address.

  note: IBAN protocol seems to be soft-deprecated

  Parameters:
  web3          - web3 instance
  from          - address from which we want to send transaction
  iban-address  - IBAN address to which we want to send transaction
  value         - value that we want to send in IBAN transaction
  callback-fn   - callback with two parameters, error and result

  Note: uses smart contract to transfer money to IBAN account.

  Example:
  user> `(send-iban-transaction! '0xx'
                                 'NL88YADYA02'
                                  0x100
                                  (fn [err res] (prn res)))`"
  [web3 & [from iban-address value cb :as args]]
  (oapply+ (eth web3) "sendTransaction" [from (ocall (iban web3) "toAddress" iban-address) value cb]))


(defn sign
  "Signs data from a specific account. This account needs to be unlocked.

  Parameters:
  web3          - web3 instance
  address       - The address to sign with
  data-to-sign  - Data to sign
  callback-fn   - callback with two parameters, error and result

  Returns the signed data.

  After the hex prefix, characters correspond to ECDSA values like this:

  r = signature[0:64]
  s = signature[64:128]
  v = signature[128:130]

  Note that if you are using ecrecover, v will be either \"00\" or \"01\". As a
  result, in order to use this value, you will have to parse it to an integer
  and then add 27. This will result in either a 27 or a 28.

  Example:
  user> `(sign web3-instance
               \"0x135a7de83802408321b74c322f8558db1679ac20\"
               \"0x9dd2c369a187b4e6b9c402f030e50743e619301ea62aa4c0737d4ef7e10a3d49\"
               (fn [err res] (when-not err (println res))))`

  user> 0x3..."
  [web3 & [address data-to-sign :as args]]
  (oapply (eth web3) "sign" args))


(defn sign-transaction
  "Sign a transaction. Method is not documented in the web3.js docs. Not sure if it is safe.

  Parameters:
  web3           - web3 instance
  sign-tx-params - Parameters of transaction
                   See `(send-transaction!)`
  private-key    - Private key to sign the transaction with
  callback-fn    - callback with two parameters, error and result

  Returns signed transaction data."
  [web3 & [sign-tx-params private-key signed-tx :as args]]
  (oapply (eth web3) "signTransaction" args))


(defn call!
  "Executes a message call transaction, which is directly executed in the VM of
  the node, but never mined into the blockchain.

  Parameters:
  web3          - web3 instance
  call-object   - A transaction object see web3.eth.sendTransaction, with the
                  difference that for calls the from property is optional as
                  well.
  default-block - If you pass this parameter it will not use the default block
                  set with set-default-block.
  callback-fn   - callback with two parameters, error and result

  Returns the returned data of the call as string, e.g. a codes functions return
  value.

  Example:
  user> `(call! web3-instance {:to   \"0x\"
                               :data \"0x\"}
                (fn [err res] (when-not err (println res))))`
  nil
  user> 0x"
  [web3 & [call-object default-block :as args]]
  (oapply (eth web3) "call" args))


(defn estimate-gas

  "Executes a message call or transaction, which is directly executed in the VM
  of the node, but never mined into the blockchain and returns the amount of the
  gas used.

  Parameters:
  web3          - web3 instance
  call-object   - See `(send-transaction!)`, except that all properties are
                  optional.
  callback-fn   - callback with two parameters, error and result

  Returns the used gas for the simulated call/transaction.

  Example:
  user> `(estimate-gas web3-instance
           {:to   \"0x135a7de83802408321b74c322f8558db1679ac20\",
            :data \"0x135a7de83802408321b74c322f8558db1679ac20\"}
           (fn [err res] (when-not err (println res))))`
  nil
  user> 22361"
  [web3 & [call-object :as args]]

  (oapply (eth web3) "estimateGas" args))


(defn filter
  "Parameters:

  Important: filter functionality is deprecated

  web3          - web3 instance
  block-or-transaction  - The string \"latest\" or \"pending\" to watch
                          for changes in the latest block or pending
                          transactions respectively. Or a filter options
                          object as follows:

    from-block: Number|String - The number of the earliest block (latest may be
                                given to mean the most recent and pending
                                currently mining, block). By default
                               latest.
    to-block: Number|String   - The number of the latest block (latest may be
                                given to mean the most recent and pending
                                currently mining, block). By default latest.

    address: String           - An address or a list of addresses to only get
                                logs from particular account(s).

    :topics: Array of Strings - An array of values which must each appear in the
                                log entries. The order is important, if you want
                                to leave topics out use null, e.g.
                                `[null, '0x00...']`. You can also pass another array
                                for each topic with options for that topic e.g.
                                `[null, ['option1', 'option2']]`

  Watch callback return value

    String - When using the \"latest\" parameter, it returns the block hash of
             the last incoming block.

    String - When using the \"pending\" parameter, it returns a transaction hash
             of the most recent pending transaction.
    Object - When using manual filter options, it returns a log object as follows:

        logIndex: Number - integer of the log index position in the block. null
                           when its pending log.
        transactionIndex: Number - integer of the transactions index position log
                                   was created from. null when its pending log.
        transactionHash: String, 32 Bytes - hash of the transactions this log was
                                            created from. null when its pending log.
        blockHash: String, 32 Bytes - hash of the block where this log was in. null
                                      when its pending. null when its pending log.
        blockNumber: Number - the block number where this log was in. null when its
                              pending. null when its pending log.
        address: String, 32 Bytes - address from which this log originated.
        data: String - contains one or more 32 Bytes non-indexed arguments of the log.

        topics: Array of Strings - Array of 0 to 4 32 Bytes DATA of indexed log
                                   arguments. (In solidity: The first topic is the hash
                                   of the signature of the event, except if you declared the
                                   event with the anonymous specifier.)

  Note for event filter return values see Contract Events at
  https://github.com/ethereum/wiki/wiki/JavaScript-API#contract-events"
  [web3 & args]
  (let [simple-from? (string? (first args))]
    (if simple-from?
      (get-past-logs web3 {:from-block (first args) :topics [nil]} (second args))
      (get-past-logs web3 (first args) (second args)))))

(defn extend [web3 & args] (oapply (eth web3) "extend" args))
