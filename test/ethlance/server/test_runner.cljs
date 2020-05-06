(ns ethlance.server.test-runner
  (:require [cljs.nodejs :as nodejs]
            [doo.runner :refer-macros [doo-tests]]
            [ethlance.server.graphql.resolvers-test]
            [ethlance.server.contract.bounty-test]
            [ethlance.server.contract.ethlance-jobs-test]))

(nodejs/enable-util-print!)

(doo-tests 'ethlance.server.graphql.resolvers-test
           'ethlance.server.contract.bounty-test
           'ethlance.server.contract.ethlance-jobs-test
           )
