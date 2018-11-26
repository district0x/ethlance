(ns ethlance.server.model.arbiter-test
  "Unit tests for the arbiter model."
  (:require
   [clojure.test :refer [deftest is are testing]]
   [bignumber.core :as bn]
   [cuerdas.core :as str]
   [district.server.config]
   [district.server.db :as district.db]
   [taoensso.timbre :as log]
   [ethlance.server.db :as ethlance.db]
   [ethlance.server.model.user :as user]
   [ethlance.server.model.arbiter :as arbiter]
   [ethlance.server.test-utils.db :refer [deftest-database] :include-macros true]))


(deftest-database main-arbiter-model {})
