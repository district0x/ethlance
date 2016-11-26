(ns ethlance.utils
  (:require [bidi.bidi :as bidi]
            [clojure.string :as string]
            [ethlance.routes :refer [routes]]))

(defn path-for [& args]
  (str "#" (apply bidi/path-for routes args)))

(defn location-hash []
  (let [hash (-> js/document
               .-location
               .-hash
               (string/split #"\?")
               first
               (string/replace "#" ""))]
    (if (empty? hash) "/" hash)))

(defn nsname [x]
  (when x
    (str (when-let [n (namespace x)] (str n "")) (name x))))

(defn match-current-location []
  (bidi/match-route routes (location-hash)))

(defn truncate
  "Truncate a string with suffix (ellipsis by default) if it is
   longer than specified length."
  ([string length]
   (truncate string length "..."))
  ([string length suffix]
   (let [string-len (count string)
         suffix-len (count suffix)]
     (if (<= string-len length)
       string
       (str (subs string 0 (- length suffix-len)) suffix)))))