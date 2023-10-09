(ns ethlance.ui.util.urls
  (:require
    [ethlance.ui.config :as ui-config]))

(defn ipfs-hash->gateway-url
 [ipfs-hash]
 ;TODO: This URL should come from configuration
 (cond
   (= nil ipfs-hash)
   ipfs-hash

   (or
     (clojure.string/starts-with? ipfs-hash "http")
     (clojure.string/starts-with? ipfs-hash "/"))
   ipfs-hash

   :else
   (str (get-in (ui-config/get-config) [:ipfs :gateway]) "/" ipfs-hash)))
