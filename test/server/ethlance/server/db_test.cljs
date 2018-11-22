(ns ethlance.server.db-test
  (:require
   [clojure.test :refer [deftest is are testing async]]

   [taoensso.timbre :as log]
   [cuerdas.core :as str]
   [mount.core :as mount]

   [district.server.config :refer [config]]
   [district.server.db]

   [ethlance.server.db :as db]
   [ethlance.server.core]))


(def test-config
  "Test configuration for the database."
  (-> ethlance.server.core/main-config
      (merge {:logging {:level "debug" :console? true}})
      (update :db merge {:opts {:memory true}})))


(defn fixture-start
  "Test Fixture Setup."
  []
  (-> (mount/with-args test-config)
      (mount/only
       [#'district.server.db/db
        #'ethlance.server.db/ethlance-db])
      mount/start))


(defn fixture-stop
  "Test Fixture Teardown."
  []
  (mount/stop))


(deftest test-database
  (fixture-start)
  ;;
  
  (testing "Inserting a row.."
    (db/insert-row! :User {:user/address "0x1"
                           :user/country-code "CA"
                           :user/email "john.doe@gmail.com"
                           :user/profile-image ""
                           :user/date-last-active 0
                           :user/date-joined 0}))

  ;;
  (fixture-stop))
