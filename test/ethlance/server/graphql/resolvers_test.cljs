(ns ethlance.server.graphql.resolvers-test
  (:require

   [cljs.core.async :refer [go <!]]
   [cljs.test :refer-macros [deftest is testing async use-fixtures]]
   [taoensso.timbre :as log]

   [mount.core :as mount]

   [district.server.logging]
   [ethlance.server.graphql.server]
   [district.server.db]
   [ethlance.server.db]

   [ethlance.server.graphql.generator :as generator]

   ))

;; TODO: generate-data fixture

(use-fixtures :once
  {:before (fn []

             (log/debug "Running before fixture")

             (-> (mount/with-args {
                                   :db {:opts {:memory true}}
                                   :ethlance/db {:resync? false}
                                   :graphql {:port 4000}
                                   :logging {:level :info
                                             :console? true}

                                   })
                 (mount/only [#'district.server.logging/logging
                              #'district.server.db/db
                              #'ethlance.server.db/ethlance-db
                              #'ethlance.server.graphql.server/graphql
                              ])
                 (mount/start)
                 (as-> $ (log/warn "Started" $)))

             (generator/generate-dev-data)

             )
   :after  (fn [] (log/debug "Running after fixture"))})

;; TODO : print user table
(deftest test1




  (is true))
