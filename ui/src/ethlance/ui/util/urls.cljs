(ns ethlance.ui.util.urls
  (:require
    [ethlance.ui.config :as ui-config]
    [re-frame.core :as re]))


(defn ipfs-hash->gateway-url
  [ipfs-hash]
  ;; TODO: This URL should come from configuration
  (let [config @(re/subscribe [:ethlance.ui.subscriptions/config])
        ipfs-gateway (get-in config [:ipfs :gateway])]
    (cond
      (nil? ipfs-hash)
      ipfs-hash

      (or
        (clojure.string/starts-with? ipfs-hash "http")
        (clojure.string/starts-with? ipfs-hash "/"))
      ipfs-hash

      :else
      (str ipfs-gateway "/" ipfs-hash))))
