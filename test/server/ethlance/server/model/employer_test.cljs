(ns ethlance.server.model.employer-test
  "Unit tests for the employer model."
  (:require
   [clojure.test :refer [deftest is are testing]]
   [bignumber.core :as bn]
   [cuerdas.core :as str]
   [district.server.config]
   [district.server.db :as district.db]
   [taoensso.timbre :as log]
   [ethlance.server.db :as ethlance.db]
   [ethlance.server.model.user :as user]
   [ethlance.server.model.employer :as employer]
   [ethlance.server.test-utils.db :refer [deftest-database] :include-macros true]
   [ethlance.shared.enum.bid-option :as enum.bid-option]
   [ethlance.shared.enum.boolean :as enum.boolean]
   [ethlance.shared.enum.contract-status :as enum.status]))


(deftest-database main-employer-model {}
  (user/register! {:user/id 1
                   :user/address "0x1"
                   :user/country-code "CA"
                   :user/email "john.doe@gmail.com"
                   :user/profile-image ""
                   :user/date-updated 0
                   :user/date-created 0})

  (is (not (employer/is-registered? 1)))

  (testing "Registering a employer"
    (employer/register! {:user/id 1
                         :employer/biography "A testy fellow"
                         :employer/date-registered 0
                         :employer/professional-title "Project Manager"})

    (is (employer/is-registered? 1)))

  (testing "Getting main data"
    (let [employer-data (employer/get-data 1)]
      (is (= (:employer/biography employer-data) "A testy fellow"))
      (is (= (:employer/professional-title employer-data) "Project Manager")))))
