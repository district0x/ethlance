(ns ethlance.server.new-syncer
  (:require
    [taoensso.timbre :as log]
    ; [ethlance.server.config :as server-config]
    ; [district.server.config :as district-config]
    [ethlance.server.new-syncer.core :as new-syncer.core]

    [ethlance.server.db]
    [ethlance.server.ipfs]

    [clojure.core.async :as async :refer [<! >! go] :include-macros true]
    [district.server.config :as server.config]
    [ethlance.server.new-syncer.web3-subscribe :as new-syncer.web3-subscribe]
    [district.server.async-db :as db :include-macros true]
    [mount.core :as mount :refer [defstate]]))

(declare start stop)

(defstate new-syncer :start (start @server.config/config) :stop (stop))
(defonce subscription-started? (atom false))

(defn db-context-provider [handler]
  (async/go
    (let [db-conn (async/<! (db/get-connection))]
     (try
       (handler db-conn)
       (catch js/Error error
         (log/error "Something failed processing" error))
       (finally
         (db/release-connection db-conn))))))

(defn start-syncer
  [config]
  (let [out-chan (async/chan)
        ethereum-node (get-in config [:web3 :url])
        ethlance-abi (new-syncer.core/parse-contract-abi (str (get-in config [:smart-contracts :contracts-build-path]) "/Ethlance.json"))
        contract (get-in config [:smart-contracts :contracts-var])
        ethlance-address (get-in @contract [:ethlance :address])
        handlers (get-in config [:new-syncer :handlers])
        save-checkpoint (get-in config [:new-syncer :save-checkpoint])
        listen-to-new-events? (get-in config [:new-syncer :auto-start-listening-new-events?])
        start-subscription (fn []
                             (reset! subscription-started? true)
                             (new-syncer.web3-subscribe/subscribe!
                               {:ethereum-node ethereum-node
                                :ethlance-abi ethlance-abi
                                :ethlance-address ethlance-address}
                               out-chan))
        started-at (new js/Date)
        syncer (new-syncer.core/init-syncer out-chan handlers db-context-provider)]
    (if listen-to-new-events?
      (start-subscription)
      (log/info "Not subscribing to new events due to :auto-start-listening-new-events? false"))

    (async/go-loop []
      (let [result (<! syncer)
            block-number (get-in result [:event :block-number])
            transaction-index (get-in result [:event :transaction-index])
            log-index (get-in result [:event :log-index])
            checkpoint {:last-processed-block block-number
                        :processed-log-indexes [transaction-index log-index]
                        :started-at started-at}]
        (println ">>> SYNCER processed: " result)
        (println ">>> SYNCER checkpoint: " checkpoint)
        (save-checkpoint checkpoint)
        (recur)))

    {:start-subscription start-subscription
     :started-at started-at
     :out-chan out-chan
     :syncer syncer}))

(defn start
  [config]
  ;; Start the subscription
  (log/info "Starting new-syncer. auto-start?" (get-in config [:new-syncer :auto-start-listening-new-events?]))
  ; (if (get-in config [:new-syncer :auto-start-listening-new-events?])
  ;   (start-syncer config)
  ;   (log/info "Postponing listening new events due to :auto-start-listening-new-events? false"))
  (start-syncer config)
  )

(defn stop
  []
  ;; Stop listening to the events
  )

(defn start-listening-new-events
  []
  (log/info "start-listening-new-events called")
  ((:start-subscription @new-syncer)))

(defn -main
  [& _]
  (log/info "Initializing new-syncer")
  ; (->
  ;   (mount/only #{#'ethlance.server.db/ethlance-db
  ;                 #'ethlance.server.ipfs/ipfs})
  ;   (mount/with-args
  ;       {:config
  ;        {:env-name "SERVER_CONFIG_PATH"
  ;         :default (server-config/env-config server-config/environment)}})
  ;     (mount/start))
  )
