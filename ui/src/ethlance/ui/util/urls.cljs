(ns ethlance.ui.util.urls)

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
   (str "http://ipfs.localhost:8080/ipfs/" ipfs-hash)))
