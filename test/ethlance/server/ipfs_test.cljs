(ns ethlance.server.ipfs-test
  "Tests for IPFS use cases."
  (:require
   [clojure.core.async :as async :refer [go go-loop <! >! chan close!] :include-macros true]
   [clojure.test :refer [deftest is are testing async]]

   [taoensso.timbre :as log]
   [cuerdas.core :as str]
   [mount.core :as mount]

   [district.server.config :refer [config]]

   [ethlance.server.test-utils.ipfs :refer [deftest-ipfs] :include-macros true]
   [ethlance.server.ipfs :as ipfs]
   [ethlance.server.core]
   [ethlance.shared.async-utils :refer [<!-<log <!-<throw] :include-macros true]))


(deftest-ipfs main-ipfs {}
  (async
   done
   (let [test-data "Here is a piece of test data"
         [result-chan err-chan] (ipfs/add! (ipfs/to-buffer test-data))]
     (go
       (when-let [err (<! err-chan)]
         (log/error (str "IPFS Test: Failed to add IPFS data " err)))
       
       (let [result (<! result-chan)]
         (is (not (nil? result)))

         (let [[result-chan err-chan] (ipfs/get result)]
           (when-let [err (<! err-chan)]
             (log/error (str "IPFS Test: Failed to get IPFS data " err)))
           
           (let [result (<! result-chan)]
             (is (not (nil? result)))
             (log/debug (str "Get Result: " result)))))
       (done)))))


(deftest test-cors-edn-parser
  (let [unparsed-1 "QmdMiD5N5ZaFbHKDDLUp4hb3nQyKmmzqLaAV7ZGKBHyTjR0000644000000000000000000000002113401770704016770 0ustar0000000000000000{:a \"123\", :b 12}"
        unparsed-2 "QmVnTHmLZbY1Ztf6UDrkLjZewwKaQVU1JWJZirfMCVfgCY0000644000000000000000000000003413401770704017264 0ustar0000000000000000{:a [1 2 3], :b #{:c :b :a}}"
        unparsed-3 "QmQpFuw3a4SJr7FG92dSo67XKRoM3GATWqH6iABps6axbf0000644000000000000000000000007613401770704016642 0ustar0000000000000000{:list-element-1 [1 2 3], :list-element-2 [:a {:b {:c true}}]}"
        unparsed-4 "QmamZXehmRf7CjPJZnCJNfW5feJp9yJozt4TaCJCQoBNuL0000644000000000000000000000002313401770704017217 0ustar0000000000000000{:a {:b {:c true}}}"
        unparsed-5 "QmamZXehmRf7CjPJZnCJNfW5feJp9yJozt4TaCJCQoBNuL0000644000000000000000000000002313401770704017217 0ustar0000000000000000{}"]
    (is (= (ipfs/parse-edn-result unparsed-1) {:a "123" :b 12}))
    (is (= (ipfs/parse-edn-result unparsed-2) {:a [1 2 3] :b #{:c :b :a}}))
    (is (= (ipfs/parse-edn-result unparsed-3) {:list-element-1 [1 2 3] :list-element-2 [:a {:b {:c true}}]}))
    (is (= (ipfs/parse-edn-result unparsed-4) {:a {:b {:c true}}}))
    (is (= (ipfs/parse-edn-result unparsed-5) {}))))


(deftest test-cors-edn-parser-async
  (async
   done
   (let [unparsed "QmdMiD5N5ZaFbHKDDLUp4hb3nQyKmmzqLaAV7ZGKBHyTjR0000644000000000000000000000002113401770704016770 0ustar0000000000000000{:a \"123\", :b 12}"]
     (go
       (let [result (ipfs/parse-edn-result unparsed)]
         (is (= result {:a "123" :b 12}))
         (done))))))


(deftest-ipfs edn-tests {}
  (async
   done
   (let [edn-value-1 {:a "123" :b 12}
         *hash-1 (atom nil)
         edn-value-2 {:a [1 2 3] :b #{:a :b :c}}
         *hash-2 (atom nil)
         edn-value-3 {:list-element-1 [1 2 3] :list-element-2 [:a {:b {:c true}}]}
         *hash-3 (atom nil)
         edn-value-4 {:a {:b {:c true}}}
         *hash-4 (atom nil)]
     (go
       (let [result-1 (<!-<log (ipfs/add-edn! edn-value-1))
             result-2 (<!-<log (ipfs/add-edn! edn-value-2))
             result-3 (<!-<log (ipfs/add-edn! edn-value-3))
             result-4 (<!-<log (ipfs/add-edn! edn-value-4))]

         (reset! *hash-1 result-1)
         (reset! *hash-2 result-2)
         (reset! *hash-3 result-3)
         (reset! *hash-4 result-4))

       (let [value-1 (<!-<log (ipfs/get-edn @*hash-1))
             value-2 (<!-<log (ipfs/get-edn @*hash-2))
             value-3 (<!-<log (ipfs/get-edn @*hash-3))
             value-4 (<!-<log (ipfs/get-edn @*hash-4))]
         (is (= edn-value-1 value-1))
         (is (= edn-value-2 value-2))
         (is (= edn-value-3 value-3))
         (is (= edn-value-4 value-4)))

       (done)))))
       
