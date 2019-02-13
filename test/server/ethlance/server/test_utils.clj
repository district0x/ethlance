(ns ethlance.server.test-utils
  "Macros for test-utils")


(defmacro deftest-smart-contract
  "deftest for smart-contracts.
  
  Note:
  
  - The testnet is reverted to an initial deployment before running
  the testcase."
  [name opts & body]
  (assert (symbol? name) "Name must be a symbol.")
  (assert (map? opts) "Options field should be a map, did you forget to prepend it?")
  `(clojure.test/deftest ~name
     (ethlance.server.test-utils/fixture-start ~opts)
     ~@body
     (ethlance.server.test-utils/fixture-stop)))


(defmacro deftest-smart-contract-go
  "deftest for smart-contracts.
  
  Note:
  
  - The testnet is reverted to an initial deployment before running
  the testcase."
  [name opts & body]
  (assert (symbol? name) "Name must be a symbol.")
  (assert (map? opts) "Options field should be a map, did you forget to prepend it?")
  `(clojure.test/deftest ~name
     (clojure.test/async
      done#
      (clojure.core.async/go
        (ethlance.server.test-utils/fixture-start ~opts)
        ~@body
        (ethlance.server.test-utils/fixture-stop)
        (done#)))))
