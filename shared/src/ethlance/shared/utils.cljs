(ns ethlance.shared.utils
  (:require-macros [ethlance.shared.utils])
  (:require [alphabase.base58 :as base58]
            [alphabase.hex :as hex]
            [goog.date.relative :as gdate]
            [goog.object]
            ["web3" :as w3]))

(defn now []
  (.getTime (js/Date.)))

(defn base58->hex
  "Useful for converting IPFS hash to a format suitable for storing in Solidity
  bytes memory _ipfsData

  Example:
    (base58->hex \"QmSj298W5U7cn7ync6kLxZgTdmSC1j9cMxeVAc8d6bt2ej\")"
  [base58-str]
  (->> base58-str
       base58/decode
       hex/encode
       (str "0x" ,,,)))

(defn hex->base58
  "Useful for converting Solidity bytes memory _ipfsData back to IPFS hash

  Example:
    (base58->hex \"0x12204129c213954a4864af722e5160c92b158f1215c13416a1165a6ee7142371b368\")"
  [hex-str]
  (-> hex-str
      (clojure.string/replace ,,, #"^0x" "")
      hex/decode
      base58/encode))

(defn eth->wei [eth-amount] (.toWei (.-utils w3) (str eth-amount)))
(defn wei->eth [wei-amount] (.fromWei (.-utils w3) (str wei-amount)))

(defn millis->relative-time [millis]
  (gdate/format (new js/Date (js/parseInt millis))))

(defn ilike=
  "Makes case insensitive comparison of string representation of all arguments
  Note!
    1 = \"1\"
    nil = nil
    a = A
  "
  [& args]
  (apply = (map #(clojure.string/lower-case (str %)) args)))

(def ilike!= (comp not ilike=))

(defn js-obj->clj-map
  [obj]
  (-> (fn [result key]
        (let [v (goog.object/get obj key)]
          (if (= "function" (goog/typeOf v))
            result
            (assoc result key v))))
      (reduce {} (.getKeys goog/object obj))))

(defn deep-merge
  "Merges nested maps, left to right (overwriting existing values)

   Example:
    (deep-merge {:first {:a 1 :b 2}}
                {:first {:b 3 :c 4} :second {:d 4}})

    => {:first {:a 1, :b 3, :c 4}, :second {:d 4}}
  "
  [& maps]
  (apply merge-with (fn [& args]
                      (if (every? map? args)
                        (apply deep-merge args)
                        (last args)))
         maps))
