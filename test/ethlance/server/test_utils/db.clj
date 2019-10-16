(ns ethlance.server.test-utils.db)


(defmacro deftest-database
  "deftest for database testcases.

  Note:

  - cannot be used in tandem with smart contracts, since they both
  maintain a seperate mount cycle. Please look at other developments."
  [name opts & body]
  (assert (symbol? name) "Name must be a symbol.")
  (assert (map? opts) "Options field should be a map, did you forget to prepend it?")
  
  ;; Use try-finally (?)
  `(clojure.test/deftest ~name
     (try
       (ethlance.server.test-utils.db/fixture-start ~opts)
       ~@body
       (finally
         (ethlance.server.test-utils.db/fixture-stop)))))
