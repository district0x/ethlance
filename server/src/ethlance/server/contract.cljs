(ns ethlance.server.contract
  "Includes functions for working with ethereum contracts in an asynchronous manner."
  (:require [clojure.core.async
             :as
             async
             :refer
             [>! chan close! go put!]
             :include-macros
             true]
            [district.server.smart-contracts :as contracts]))

(defn call
  "Call the given `contract-address` with the kebab-case formatted
  `method-name` and with the given `args`. Returns a success channel
  and an error channel.

  # Notes:

  - This function wraps
  `district.server.smart-contracts/contract-call` for use with
  core.async.

  - Correctly passes the result to the success channel in situations
  where a promise is not generated.
  "
  [& {:keys [contract-key method-name contract-arguments contract-options]
      :or {contract-arguments []
           contract-options {}}}]
  (let [success-channel (chan 1)
        error-channel (chan 1)]
    (go
      (let [result (contracts/contract-call contract-key method-name contract-arguments contract-options)]
        ;; Some of the calls return the result directly instead of a
        ;; js/Promise object. These are correctly passed to the
        ;; success channel in those situations.
        (if (instance? js/Promise result)
          (-> result
              (.then
               ;; Success
               (fn [result]
                 (put! success-channel result)
                 (close! error-channel))
               ;; Failure
               (fn [error-object]
                 (put! error-channel error-object)
                 (close! success-channel))))
          ;; No promise, pass the result to the success channel.
          (do (>! success-channel result)
              (close! error-channel)))))
    [success-channel error-channel]))
