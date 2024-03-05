(ns tests.runner
  (:require
    [cljs-promises.async]
    [cljs.nodejs :as nodejs]
    [district.shared.async-helpers :as async-helpers]
    [tests.setup]))


(nodejs/enable-util-print!)

(async-helpers/extend-promises-as-channels!)


;; Tests get run automatically by shadow.test.node/main which runs tests using cljs.test
;; To run specific namespace tests, add --tests=<namespaces-separated-by-comma>

(println "tests.runner Running tests")
(tests.setup/setup-test-env)
