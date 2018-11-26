(ns ethlance.server.model.user-test
  "Unit tests for the user model."
  (:require
   [clojure.test :refer [deftest is are testing]]
   [bignumber.core :as bn]
   [cuerdas.core :as str]
   [district.server.config]
   [district.server.db :as district.db]
   [taoensso.timbre :as log]
   [ethlance.server.db :as ethlance.db]
   [ethlance.server.model.user :as user]
   [ethlance.server.test-utils.db :refer [deftest-database] :include-macros true]))


(deftest-database main-user-model {}
  (testing "Registering a user"
    (user/register! {:user/id 1
                     :user/address "0x1"
                     :user/country-code "CA"
                     :user/email "john.doe@gmail.com"
                     :user/profile-image ""
                     :user/date-last-active 0
                     :user/date-joined 0}))

  (testing "Checking existence."
    (is (user/exists? 1))
    (is (not (user/exists? 2))))

  (testing "Getting the registered user."
    (let [user (user/get-data 1)]
      (is (= (:user/country-code user) "CA")))))
