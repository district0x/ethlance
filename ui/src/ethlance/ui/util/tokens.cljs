(ns ethlance.ui.util.tokens
  (:require
    [re-frame.core :as re]
    [cljs-web3-next.eth :as w3-eth]))

(defn address->token-info-url [address]
  (str "https://ethplorer.io/address/" address))
