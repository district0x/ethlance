(ns ethlance.server.test-runner
  (:require
   [cljs.nodejs :as nodejs]
   [doo.runner :refer-macros [doo-tests]]
   [ethlance.server.graphql.resolvers-test]
   ))

(nodejs/enable-util-print!)

(doo-tests 'ethlance.server.graphql.resolvers-test)
