(ns ethlance.server.test-utils)


(defmacro deftest-smart-contract
  "deftest for smart-contracts.
  
  Note:
  
  - The testnet is reverted to an initial deployment before running
  the testcase."
  [name opts & body]
  `(clojure.test/deftest ~name
     (ethlance.server.test-utils/fixture-start ~opts)
     ~@body
     (ethlance.server.test-utils/fixture-stop)))
