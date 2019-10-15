(ns ethlance.server.db-test
  (:require
   [clojure.test :refer [deftest is are testing async]]

   [taoensso.timbre :as log]
   [cuerdas.core :as str]
   [mount.core :as mount]

   [district.server.config :refer [config]]
   [district.server.db]

   [ethlance.server.test-utils.db :refer [deftest-database] :include-macros true]
   [ethlance.server.db :as db]
   [ethlance.server.core]))


(deftest-database test-user {})
