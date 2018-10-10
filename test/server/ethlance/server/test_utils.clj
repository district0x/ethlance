(ns ethlance.server.test-utils)


(defmacro deftest-smart-contract
  [name & body]
  `(clojure.test/deftest ~name
     (ethlance.server.test-utils/fixture-start {})
     ~@body
     (ethlance.server.test-utils/fixture-stop)))


#_(defmacro defvalue [name value]
    `(def ~name ~value))
