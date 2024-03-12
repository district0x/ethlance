(ns tests.repl
  (:require
    [tests.setup]))


(defn -main
  []
  (println ">>> Running tests.repl/-main")
  (tests.setup/setup-test-env)
  (js/setInterval #(println "Exiting after waiting") 100000000))
