(ns ethlance.server.contract
  "Includes functions for working with ethereum contracts in an asynchronous manner."
  (:require
   [clojure.core.async :as async :refer [go go-loop <! >! chan close!] :include-macros true]
   [district.server.smart-contracts :as contracts]))


(defn deploy!
  "Deploy the given `contract-key` with constructor `args` and
  additional `opts`. Returns a success channel and an error channel.

  # Notes:

  - This function wraps
  `district.server.smart-contracts/deploy-smart-contract!` for use
  with core.async.
  "
  [contract-key args opts]
  (let [success-channel (chan 1)
        error-channel (chan 1)]
    (go
      (-> (contracts/deploy-smart-contract! contract-key args opts)
          (.then
           (fn [result]
             (>! success-channel result)
             (close! error-channel)))
          (.catch
           (fn [error-object]
             (>! error-channel error-object)
             (close! success-channel)))))
    [success-channel error-channel]))


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
  [contract-address method-name & args]
  (let [success-channel (chan 1)
        error-channel (chan 1)]
    (go
      (let [result (apply contracts/contract-call contract-address method-name args)]
        ;; Some of the calls return the result directly instead of a
        ;; js/Promise object. These are correctly passed to the
        ;; success channel in those situations.
        (if (instance? js/Promise result)
          (-> result
              (.then
               (fn [result]
                 (>! success-channel result)
                 (close! error-channel)))
              (.catch
               (fn [error-object]
                 (>! error-channel error-object)
                 (close! success-channel))))
          ;; No promise, pass the result to the success channel.
          (do (>! success-channel result)
              (close! error-channel))))))
  [success-channel error-channel])
