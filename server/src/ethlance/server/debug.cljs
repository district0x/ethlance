(ns ethlance.server.debug
  (:require
    [cljs-web3-next.eth :as web3-eth]
    [district.server.web3 :refer [web3]]))

(defn connected? []
  (new js/Promise
       (fn [resolve _reject]
         (web3-eth/connected?
           @web3
           (fn [err connected?]
             (resolve {:error err :connected connected?}))))))

(defonce connection-history (atom []))

(defn add-data
  []
  (web3-eth/connected?
    @web3
    (fn [err connected?]
      (when (not= (:connected (last @connection-history)) connected?)
        (swap! connection-history
               conj
               {:time (.toUTCString (new js/Date))
                :connected connected?
                :error (when (not (nil? err)) (.-message err))})))))

(defn start-collecting
  [seconds]
  (js/setInterval add-data (* 1000 seconds)))

(defn collect
  "Returns a JS Promise with the debug data"
  []
  (new js/Promise
    (fn [resolve _reject]
      ; Let each promise return a pair: key under which they go and value
      ; Then use Promise.all to resolve them and create a map with the values
      (.then
        (connected?)
        (fn [values]
          (resolve (merge
                     {:connection-history @connection-history}
                     (js->clj values))))))))
