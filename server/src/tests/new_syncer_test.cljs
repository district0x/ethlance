(ns tests.new-syncer-test
  (:require
    [cljs.test :refer-macros [deftest is async testing]]
    [clojure.core.async :as async :refer [<! >! go]:include-macros true]
    [ethlance.server.new-syncer.core :as new-syncer.core]
    ))

(deftest test-init-syncer-single
  (testing "processing one by one"
    (async done
      (go
        (let [source (async/chan)
              context (fn [handler] (handler "I am DB"))
              events-processed (atom [])
              add-to-processed (fn [_conn _err event] (swap! events-processed conj event))
              async-handler (fn [_conn _err event] (go (add-to-processed _conn _err event)))
              handlers {:JobCreated add-to-processed
                        :FundsAdded async-handler}
              syncer (new-syncer.core/init-syncer source handlers context)
              ev-job-created {:event :JobCreated}
              ev-funds-added {:event :FundsAdded}]
          (>! source [nil ev-job-created])
          (<! syncer)
          (is (= (last @events-processed) ev-job-created))

          (>! source [nil ev-funds-added])
          (<! syncer)
          (is (= (last @events-processed) ev-funds-added))
          (done))))))

#_ (deftest test-init-syncer-multi
  (testing "processing one by one"
    (async done
      (go
        (let [source (async/chan)
              context {:db "I am DB connection"}
              events-processed (atom [])
              add-to-processed (fn [_conn _err event] (swap! events-processed conj event))
              handlers {:JobCreated add-to-processed
                        :FundsAdded add-to-processed}
              syncer (new-syncer.core/init-syncer source handlers context)
              ev-job-created {:event :JobCreated}
              ev-funds-added {:event :FundsAdded}]

          (>! source [nil ev-job-created])
          (>! source [nil ev-funds-added])
          (loop []
            (when (<! syncer)
              (println "Looping")
              (recur)))

          (is (= @events-processed [ev-job-created ev-funds-added]))
          (println ">>> 2nd test @events-processed" @events-processed)

          (done))))))

; (deftest test-process-past-events)

; (deftest test-process-new-events
;   (async done
;          (let [])))
