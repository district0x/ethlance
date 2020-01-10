(ns ethlance.server.graphql.resolvers-test
  (:require
   [cljs.core.async :refer [go <!]]
   [cljs.nodejs :as nodejs]
   [cljs.test :refer-macros [deftest is testing async use-fixtures]]
   [district.server.db :as db]
   [district.server.logging]
   [district.shared.async-helpers :as async-helpers :refer [promise->]]
   [ethlance.server.db]
   [ethlance.server.graphql.generator :as generator]
   [ethlance.server.graphql.server]
   [ethlance.server.graphql.utils :refer [run-query]]
   [mount.core :as mount]
   [taoensso.timbre :as log]
   ))

;; TODO: run-query

(async-helpers/extend-promises-as-channels!)

(use-fixtures :once
  {:before (fn []
             (log/debug "Running before fixture")
             (-> (mount/with-args {
                                   :db {:opts {:memory true}}
                                   :ethlance/db {:resync? false}
                                   :graphql {:port 4000}
                                   :logging {:level :debug
                                             :console? true}})
                 (mount/only [#'district.server.logging/logging
                              #'district.server.db/db
                              #'ethlance.server.db/ethlance-db
                              #'ethlance.server.graphql.server/graphql
                              ])
                 (mount/start)
                 (as-> $ (log/warn "Started" $))))
   :after  (fn [] (log/debug "Running after fixture"))})

(deftest test1
  (async done
         (go
           (let [_ (<! (generator/generate-users ["EMPLOYER" "CANDIDATE" "ARBITER"]))
                 response (<! (run-query {:url "http://localhost:4000/graphql"
                                          :query [:user {:user/address "EMPLOYER"}
                                                  [:user/address]]}))]

             (log/debug "User" {:db (db/get {:select [:*]
                                             :from [:User]
                                             :where [:= "EMPLOYER" :User.user/address]})
                                :response response})

             (is true)

             (done)))))
