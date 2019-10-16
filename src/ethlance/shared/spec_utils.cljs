(ns ethlance.shared.spec-utils
  "Includes functions for using clojure.spec to conform data at
  runtime."
  (:require
   [clojure.spec.alpha :as s]))


(defn strict-conform
  "Checks the given `value` against the given `spec` for
  conformity. Will throw a js/Error containing additional information
  on spec conformity.

  Example:

  ```clojure

  (try 
   (strict-conform str? 2)
   (catch js/Error ex
    (let [msg (-> (ex-data ex) :message)]
      (println \"Failed, Reason: \" msg))))

  ```"
  [spec value]
  (if (s/valid? spec value)
    value
    (throw (ex-info
            (str "Failed Strict Spec Conform: " (s/explain-str spec value))
            {:type ::spec-strict-conform
             :message (s/explain-str spec value)}))))
